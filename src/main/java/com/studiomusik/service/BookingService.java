package com.studiomusik.service;

import com.studiomusik.dao.BookingDAO;
import com.studiomusik.dao.BookingInstrumentDAO;
import com.studiomusik.dao.CustomerDAO;
import com.studiomusik.dao.InstrumentDAO;
import com.studiomusik.dao.RoomDAO;
import com.studiomusik.model.Booking;
import com.studiomusik.model.BookingInstrument;
import com.studiomusik.model.Customer;
import com.studiomusik.model.Instrument;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.BookingStatus;
import com.studiomusik.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * Aturan bisnis Booking (API_CONTRACTS §4.5; BR-01…BR-22).
 *
 * <h2>Transaksi dan penguncian</h2>
 * Setiap operasi tulis berjalan dalam satu transaksi ({@link #inTransaction}) pada satu
 * {@link Connection} yang dibuka lewat {@link DatabaseConnection}; DAO hanya dipanggil dengan
 * koneksi itu. Pada {@link #createBooking} baris {@code room} dikunci dengan
 * {@code RoomDAO.findByIdForUpdate} <b>sebelum</b> pengecekan overlap (D-019), sehingga dua pemesan
 * pada room yang sama diserialkan. Perubahan status dan CRUD detail alat mengunci baris room dari
 * booking terkait dengan cara yang sama sebelum membaca ulang booking (urutan: kunci dulu, baru baca,
 * agar pembacaan tidak memakai snapshot lama). Gagal di langkah mana pun &rarr; rollback penuh
 * (BR-15): tidak ada booking yatim tanpa detail yang seharusnya ada.
 *
 * <h2>Catatan perilaku</h2>
 * <ul>
 *   <li>Waktu: {@code end > start} (BR-03), kedua waktu tepat pada jam (BR-04, A-05), hari yang sama
 *       (BR-02; otomatis karena {@code LocalTime}). {@code 24:00} tidak dapat dinyatakan sehingga
 *       sesi terakhir yang mungkin berakhir pukul 23:00.</li>
 *   <li>Tanggal lampau ditolak (BR-21); hari ini diterima (jam yang sudah lewat hari ini tidak diperiksa).</li>
 *   <li>Total memakai {@link Booking#calculateTotalCost()} (HALF_UP, skala 2). Harga alat di-snapshot
 *       pada {@code booking_instrument.rental_price}; harga room diambil dari room saat perhitungan
 *       (room tidak punya snapshot di skema).</li>
 *   <li>Booking tidak pernah dihapus (A-08); pembatalan lewat status {@code CANCELLED}.</li>
 *   <li>Id yang tidak ditemukan &rarr; {@link ValidationException}; booking bukan milik Customer &rarr;
 *       {@link BusinessRuleException} dengan pesan yang sama seperti "tidak ditemukan" (BR-16).</li>
 * </ul>
 */
public class BookingService {

    private final BookingDAO bookingDAO;
    private final BookingInstrumentDAO bookingInstrumentDAO;
    private final RoomDAO roomDAO;
    private final InstrumentDAO instrumentDAO;
    private final CustomerDAO customerDAO;

    public BookingService() {
        this(new BookingDAO(), new BookingInstrumentDAO(), new RoomDAO(), new InstrumentDAO(), new CustomerDAO());
    }

    /** Konstruktor injeksi DAO (dipakai tes, mis. untuk menyimulasikan kegagalan insert detail). */
    public BookingService(BookingDAO bookingDAO, BookingInstrumentDAO bookingInstrumentDAO,
                          RoomDAO roomDAO, InstrumentDAO instrumentDAO, CustomerDAO customerDAO) {
        this.bookingDAO = bookingDAO;
        this.bookingInstrumentDAO = bookingInstrumentDAO;
        this.roomDAO = roomDAO;
        this.instrumentDAO = instrumentDAO;
        this.customerDAO = customerDAO;
    }

    // =====================================================================
    // Pengecekan & estimasi (read-only)
    // =====================================================================

    /**
     * Memvalidasi jadwal (BR-02/03/04/21), memastikan room ada dan tidak MAINTENANCE (BR-05), lalu
     * memeriksa overlap dengan booking PENDING/CONFIRMED (BR-07/08).
     *
     * @return {@code true} bila jadwal bebas; {@code false} bila bertumpuk
     * @throws ValidationException   input jadwal tidak valid atau room tidak ditemukan
     * @throws BusinessRuleException room MAINTENANCE
     */
    public boolean isRoomAvailable(int roomId, LocalDate date, LocalTime start, LocalTime end) {
        validateSchedule(date, start, end, true);
        try {
            Room room = roomDAO.findById(roomId).orElseThrow(BookingService::roomNotFound);
            requireRoomBookable(room);
            return !bookingDAO.existsOverlap(roomId, date, start, end);
        } catch (SQLException e) {
            throw ServiceSupport.dbError("memeriksa ketersediaan ruangan", e);
        }
    }

    /**
     * Estimasi total = harga room &times; durasi + &Sigma; (quantity &times; harga alat saat ini).
     * Tidak menyimpan apa pun. Tanggal tidak dibutuhkan untuk biaya.
     *
     * @param instrumentQuantities key = instrumentId, value = quantity (&ge; 1); null/kosong = tanpa alat
     * @throws ValidationException   waktu/quantity tidak valid, room atau alat tidak ditemukan
     * @throws BusinessRuleException room atau alat MAINTENANCE
     */
    public BigDecimal estimateTotalCost(int roomId, LocalTime start, LocalTime end,
                                        Map<Integer, Integer> instrumentQuantities) {
        validateSchedule(null, start, end, false);
        Map<Integer, Integer> quantities = normalizeQuantities(instrumentQuantities);
        try {
            Room room = roomDAO.findById(roomId).orElseThrow(BookingService::roomNotFound);
            requireRoomBookable(room);
            Booking priced = buildPricedBooking(null, room, null, start, end, quantities,
                    instrumentDAO::findById);
            return priced.getTotalCost();
        } catch (SQLException e) {
            throw ServiceSupport.dbError("menghitung estimasi biaya", e);
        }
    }

    // =====================================================================
    // Pembuatan
    // =====================================================================

    /**
     * Membuat booking berstatus {@code PENDING} (BR-13) beserta detail alatnya dalam <b>satu
     * transaksi</b> (BR-15): kunci room &rarr; cek overlap &rarr; hitung total &rarr; insert booking
     * &rarr; insert detail (snapshot harga) &rarr; commit. Kegagalan di langkah mana pun = rollback.
     *
     * @param instrumentQuantities key = instrumentId, value = quantity (&ge; 1); null/kosong = tanpa alat
     * @return booking tersimpan beserta detail alat
     * @throws ValidationException   input tidak valid (waktu, tanggal lampau, quantity, id tidak ada)
     * @throws BusinessRuleException room/alat MAINTENANCE atau jadwal bertumpuk
     * @throws ServiceException      kegagalan database (pesan umum)
     */
    public Booking createBooking(int customerId, int roomId, LocalDate date, LocalTime start, LocalTime end,
                                 Map<Integer, Integer> instrumentQuantities) {
        validateSchedule(date, start, end, true);
        Map<Integer, Integer> quantities = normalizeQuantities(instrumentQuantities);

        return inTransaction("menyimpan booking", conn -> {
            // Customer dibaca lewat koneksi sendiri (bukan koneksi transaksi) agar pernyataan pertama
            // pada koneksi transaksi adalah penguncian room.
            Customer customer = customerDAO.findById(customerId)
                    .orElseThrow(() -> new ValidationException("Customer tidak ditemukan."));

            Room room = roomDAO.findByIdForUpdate(conn, roomId).orElseThrow(BookingService::roomNotFound);
            requireRoomBookable(room);
            if (bookingDAO.existsOverlap(conn, roomId, date, start, end)) {
                throw new BusinessRuleException("Jadwal bertabrakan dengan booking lain pada ruangan dan "
                        + "tanggal yang sama. Pilih jam lain.");
            }

            Booking booking = buildPricedBooking(customer, room, date, start, end, quantities,
                    id -> instrumentDAO.findById(conn, id));

            int bookingId = bookingDAO.insert(conn, booking);
            booking.setBookingId(bookingId);
            if (!booking.getInstruments().isEmpty()) {
                bookingInstrumentDAO.insertAll(conn, bookingId, booking.getInstruments());
            }

            Booking saved = bookingDAO.findById(conn, bookingId)
                    .orElseThrow(() -> new ServiceException("Booking gagal dibaca kembali setelah disimpan."));
            saved.setInstruments(bookingInstrumentDAO.findByBookingId(conn, bookingId));
            return saved;
        });
    }

    // =====================================================================
    // Pembacaan
    // =====================================================================

    /** Semua booking (Admin), terbaru lebih dulu; tanpa detail alat. */
    public List<Booking> getAllBookings() {
        try {
            return bookingDAO.findAll();
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca daftar booking", e);
        }
    }

    /** Riwayat booking milik satu Customer, terbaru lebih dulu; tanpa detail alat. */
    public List<Booking> getBookingsByCustomer(int customerId) {
        try {
            return bookingDAO.findByCustomerId(customerId);
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca riwayat booking", e);
        }
    }

    /**
     * Detail booking (Admin) beserta daftar {@link BookingInstrument} (JOIN).
     *
     * @throws ValidationException bila booking tidak ditemukan
     */
    public Booking getBookingDetail(int bookingId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Booking booking = bookingDAO.findById(conn, bookingId).orElseThrow(BookingService::bookingNotFound);
            booking.setInstruments(bookingInstrumentDAO.findByBookingId(conn, bookingId));
            return booking;
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca detail booking", e);
        }
    }

    /**
     * Detail booking untuk Customer; hanya pemilik yang boleh (BR-16, ditegakkan di sini).
     *
     * @throws BusinessRuleException bila booking tidak ada atau bukan milik {@code customerId}
     *                               (pesan sama agar keberadaan booking orang lain tidak terungkap)
     */
    public Booking getBookingForCustomer(int bookingId, int customerId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Optional<Booking> found = bookingDAO.findById(conn, bookingId);
            if (found.isEmpty() || found.get().getCustomer().getId() != customerId) {
                throw new BusinessRuleException("Booking tidak ditemukan atau bukan milik Anda.");
            }
            Booking booking = found.get();
            booking.setInstruments(bookingInstrumentDAO.findByBookingId(conn, bookingId));
            return booking;
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca detail booking", e);
        }
    }

    /** Jumlah booking per status untuk dashboard; keempat status selalu ada (0 bila kosong). */
    public Map<BookingStatus, Long> getBookingStatusSummary() {
        try {
            Map<BookingStatus, Long> summary = new EnumMap<>(BookingStatus.class);
            for (BookingStatus status : BookingStatus.values()) {
                summary.put(status, bookingDAO.countByStatus(status));
            }
            return summary;
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca ringkasan status booking", e);
        }
    }

    // =====================================================================
    // Status (Admin)
    // =====================================================================

    /**
     * Mengubah status sesuai BR-14: PENDING&rarr;CONFIRMED/CANCELLED, CONFIRMED&rarr;COMPLETED/CANCELLED;
     * COMPLETED dan CANCELLED final.
     *
     * @throws ValidationException   status null atau booking tidak ditemukan
     * @throws BusinessRuleException transisi tidak diizinkan
     */
    public void updateBookingStatus(int bookingId, BookingStatus newStatus) {
        if (newStatus == null) {
            throw new ValidationException("Status baru wajib diisi.");
        }
        int roomId = roomIdOfBooking(bookingId);
        inTransaction("mengubah status booking", conn -> {
            roomDAO.findByIdForUpdate(conn, roomId); // kunci dulu, baru baca ulang status terkini
            Booking booking = bookingDAO.findById(conn, bookingId).orElseThrow(BookingService::bookingNotFound);
            BookingStatus current = booking.getStatus();
            if (!current.canTransitionTo(newStatus)) {
                throw new BusinessRuleException("Status booking tidak dapat diubah dari " + current
                        + " ke " + newStatus + ".");
            }
            if (!bookingDAO.updateStatus(conn, bookingId, newStatus)) {
                throw bookingNotFound();
            }
            return null;
        });
    }

    // =====================================================================
    // CRUD BookingInstrument (Admin; BR-19; total dihitung ulang dalam transaksi)
    // =====================================================================

    /**
     * CREATE: menambah alat ke booking dengan snapshot harga saat ini (BR-12).
     *
     * @throws ValidationException   quantity &lt; 1 (BR-18), booking atau alat tidak ditemukan
     * @throws BusinessRuleException booking final (BR-19), alat MAINTENANCE (BR-06), alat sudah ada (BR-17)
     */
    public BookingInstrument addInstrumentToBooking(int bookingId, int instrumentId, int quantity) {
        requireQuantity(quantity);
        int roomId = roomIdOfBooking(bookingId);
        return inTransaction("menambah alat ke booking", conn -> {
            Booking booking = lockAndLoadEditable(conn, roomId, bookingId);

            Instrument instrument = instrumentDAO.findById(conn, instrumentId)
                    .orElseThrow(() -> new ValidationException("Alat tidak ditemukan."));
            requireInstrumentSelectable(instrument);
            if (bookingInstrumentDAO.findByBookingIdAndInstrumentId(conn, bookingId, instrumentId).isPresent()) {
                throw new BusinessRuleException("Alat '" + instrument.getName() + "' sudah ada pada booking ini. "
                        + "Ubah jumlahnya, bukan menambahkan lagi.");
            }

            int detailId = bookingInstrumentDAO.insert(conn, new BookingInstrument(
                    0, bookingId, instrument, quantity, instrument.getRentalPrice())); // snapshot
            recalculateTotal(conn, booking);
            return bookingInstrumentDAO.findById(conn, detailId)
                    .orElseThrow(() -> new ServiceException("Detail alat gagal dibaca kembali setelah disimpan."));
        });
    }

    /**
     * READ: daftar detail alat satu booking (JOIN booking_instrument &harr; instrument).
     *
     * @throws ValidationException bila booking tidak ditemukan
     */
    public List<BookingInstrument> getBookingInstruments(int bookingId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            bookingDAO.findById(conn, bookingId).orElseThrow(BookingService::bookingNotFound);
            return bookingInstrumentDAO.findByBookingId(conn, bookingId);
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca detail alat booking", e);
        }
    }

    /**
     * UPDATE: mengubah quantity saja; snapshot harga tidak berubah (BR-12). Total dihitung ulang.
     *
     * @throws ValidationException   newQuantity &lt; 1 (BR-18) atau detail tidak ditemukan
     * @throws BusinessRuleException booking final (BR-19)
     */
    public void updateBookingInstrumentQuantity(int bookingInstrumentId, int newQuantity) {
        requireQuantity(newQuantity);
        int bookingId = bookingIdOfDetail(bookingInstrumentId);
        int roomId = roomIdOfBooking(bookingId);
        inTransaction("mengubah jumlah alat booking", conn -> {
            Booking booking = lockAndLoadEditable(conn, roomId, bookingId);
            requireDetailBelongsTo(conn, bookingInstrumentId, bookingId);
            if (!bookingInstrumentDAO.updateQuantity(conn, bookingInstrumentId, newQuantity)) {
                throw detailNotFound();
            }
            recalculateTotal(conn, booking);
            return null;
        });
    }

    /**
     * DELETE: menghapus satu detail alat dari booking; total dihitung ulang (booking tetap ada).
     *
     * @throws ValidationException   detail tidak ditemukan
     * @throws BusinessRuleException booking final (BR-19)
     */
    public void removeInstrumentFromBooking(int bookingInstrumentId) {
        int bookingId = bookingIdOfDetail(bookingInstrumentId);
        int roomId = roomIdOfBooking(bookingId);
        inTransaction("menghapus alat dari booking", conn -> {
            Booking booking = lockAndLoadEditable(conn, roomId, bookingId);
            requireDetailBelongsTo(conn, bookingInstrumentId, bookingId);
            if (!bookingInstrumentDAO.delete(conn, bookingInstrumentId)) {
                throw detailNotFound();
            }
            recalculateTotal(conn, booking);
            return null;
        });
    }

    // =====================================================================
    // Transaksi
    // =====================================================================

    @FunctionalInterface
    private interface TxWork<T> {
        T run(Connection conn) throws SQLException;
    }

    @FunctionalInterface
    private interface InstrumentLookup {
        Optional<Instrument> find(int instrumentId) throws SQLException;
    }

    /**
     * Menjalankan {@code work} dalam satu transaksi: commit bila sukses, rollback penuh bila ada
     * exception apa pun. {@link ServiceException} diteruskan apa adanya; {@link SQLException} dibungkus
     * menjadi {@link ServiceException} berpesan umum; exception tak terduga dibungkus dan dicatat.
     */
    private <T> T inTransaction(String operation, TxWork<T> work) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (Exception e) { // rethrow presisi: hanya SQLException/RuntimeException
                rollbackQuietly(conn);
                throw e;
            }
        } catch (ServiceException e) {
            throw e;
        } catch (SQLException e) {
            throw ServiceSupport.dbError(operation, e);
        } catch (RuntimeException e) {
            throw ServiceSupport.unexpected(operation, e);
        }
    }

    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException rollbackFailure) {
            ServiceSupport.LOG.log(Level.SEVERE, "Rollback transaksi gagal", rollbackFailure);
        }
    }

    // =====================================================================
    // Validasi & helper
    // =====================================================================

    /**
     * BR-02/03/04 (+ BR-21 bila {@code checkDate}). Urutan: kelengkapan, {@code end > start},
     * jam bulat, tanggal tidak lampau. Dijalankan sebelum akses database apa pun.
     */
    private static void validateSchedule(LocalDate date, LocalTime start, LocalTime end, boolean checkDate) {
        if (checkDate && date == null) {
            throw new ValidationException("Tanggal booking wajib diisi.");
        }
        if (start == null || end == null) {
            throw new ValidationException("Waktu mulai dan waktu selesai wajib diisi.");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("Waktu selesai harus lebih besar dari waktu mulai.");
        }
        if (!isWholeHour(start) || !isWholeHour(end)) {
            throw new ValidationException("Waktu mulai dan waktu selesai harus tepat pada jam (menit 00).");
        }
        if (checkDate && date.isBefore(LocalDate.now())) {
            throw new ValidationException("Tanggal booking tidak boleh di masa lalu.");
        }
    }

    private static boolean isWholeHour(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    private static void requireQuantity(int quantity) {
        if (quantity < 1) {
            throw new ValidationException("Jumlah alat minimal 1.");
        }
    }

    /** Salinan terurut (deterministik) dan tervalidasi: id &gt; 0, quantity &ge; 1; null &rarr; kosong. */
    private static Map<Integer, Integer> normalizeQuantities(Map<Integer, Integer> input) {
        Map<Integer, Integer> result = new TreeMap<>();
        if (input == null) {
            return result;
        }
        for (Map.Entry<Integer, Integer> entry : input.entrySet()) {
            Integer instrumentId = entry.getKey();
            Integer quantity = entry.getValue();
            if (instrumentId == null || instrumentId <= 0) {
                throw new ValidationException("Id alat tidak valid.");
            }
            if (quantity == null || quantity < 1) {
                throw new ValidationException("Jumlah alat minimal 1.");
            }
            result.put(instrumentId, quantity);
        }
        return result;
    }

    private static void requireRoomBookable(Room room) {
        if (!room.isAvailable()) {
            throw new BusinessRuleException("Ruangan '" + room.getRoomName()
                    + "' sedang MAINTENANCE dan tidak dapat dipesan.");
        }
    }

    private static void requireInstrumentSelectable(Instrument instrument) {
        if (!instrument.isAvailable()) {
            throw new BusinessRuleException("Alat '" + instrument.getName()
                    + "' sedang MAINTENANCE dan tidak dapat dipilih.");
        }
    }

    /**
     * Membangun booking di memori lengkap dengan detail (snapshot harga saat ini) dan total terhitung
     * (BR-09/10/11). Aturan alat (ada, tidak MAINTENANCE, quantity) diperiksa di sini agar pesan ramah;
     * model tetap menjaga invariant yang sama sebagai pertahanan berlapis (D-030).
     */
    private Booking buildPricedBooking(Customer customer, Room room, LocalDate date, LocalTime start,
                                       LocalTime end, Map<Integer, Integer> quantities,
                                       InstrumentLookup lookup) throws SQLException {
        Booking booking = new Booking(customer, room, date, start, end);
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            Instrument instrument = lookup.find(entry.getKey()).orElseThrow(() ->
                    new ValidationException("Alat dengan id " + entry.getKey() + " tidak ditemukan."));
            requireInstrumentSelectable(instrument);
            try {
                booking.addInstrument(instrument, entry.getValue());
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage(), e);
            }
        }
        try {
            booking.setTotalCost(booking.calculateTotalCost());
        } catch (IllegalStateException e) {
            throw new ValidationException(e.getMessage(), e);
        }
        return booking;
    }

    /**
     * Mengunci room milik booking lalu membaca ulang booking (urutan penting: kunci dulu) dan
     * memastikan statusnya masih boleh diubah (BR-19).
     */
    private Booking lockAndLoadEditable(Connection conn, int roomId, int bookingId) throws SQLException {
        roomDAO.findByIdForUpdate(conn, roomId);
        Booking booking = bookingDAO.findById(conn, bookingId).orElseThrow(BookingService::bookingNotFound);
        BookingStatus status = booking.getStatus();
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Detail alat hanya dapat diubah pada booking berstatus PENDING "
                    + "atau CONFIRMED (status saat ini: " + status + ").");
        }
        return booking;
    }

    /** Memuat ulang detail dari database, menghitung ulang total (BR-11/19), dan menyimpannya. */
    private void recalculateTotal(Connection conn, Booking booking) throws SQLException {
        booking.setInstruments(bookingInstrumentDAO.findByBookingId(conn, booking.getBookingId()));
        BigDecimal total;
        try {
            total = booking.calculateTotalCost();
        } catch (IllegalStateException e) {
            ServiceSupport.LOG.log(Level.SEVERE, "Data waktu booking " + booking.getBookingId()
                    + " tidak valid untuk perhitungan biaya", e);
            throw new ServiceException("Total biaya tidak dapat dihitung karena data waktu booking tidak valid.", e);
        }
        if (!bookingDAO.updateTotalCost(conn, booking.getBookingId(), total)) {
            throw bookingNotFound();
        }
        booking.setTotalCost(total);
    }

    private void requireDetailBelongsTo(Connection conn, int bookingInstrumentId, int bookingId) throws SQLException {
        BookingInstrument detail = bookingInstrumentDAO.findById(conn, bookingInstrumentId)
                .orElseThrow(BookingService::detailNotFound);
        if (detail.getBookingId() != bookingId) {
            throw detailNotFound(); // detail dipindah/dihapus di antara pembacaan awal dan penguncian
        }
    }

    /** Pembacaan awal (koneksi sendiri) untuk mengetahui room yang harus dikunci. */
    private int roomIdOfBooking(int bookingId) {
        try {
            return bookingDAO.findById(bookingId).orElseThrow(BookingService::bookingNotFound).getRoom().getRoomId();
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca booking", e);
        }
    }

    /** Pembacaan awal (koneksi sendiri) untuk mengetahui booking pemilik detail. */
    private int bookingIdOfDetail(int bookingInstrumentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return bookingInstrumentDAO.findById(conn, bookingInstrumentId)
                    .orElseThrow(BookingService::detailNotFound).getBookingId();
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca detail alat booking", e);
        }
    }

    private static ValidationException roomNotFound() {
        return new ValidationException("Ruangan tidak ditemukan.");
    }

    private static ValidationException bookingNotFound() {
        return new ValidationException("Booking tidak ditemukan.");
    }

    private static ValidationException detailNotFound() {
        return new ValidationException("Detail alat booking tidak ditemukan.");
    }
}
