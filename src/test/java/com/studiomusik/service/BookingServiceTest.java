package com.studiomusik.service;

import com.studiomusik.dao.BookingDAO;
import com.studiomusik.model.Booking;
import com.studiomusik.model.BookingInstrument;
import com.studiomusik.model.Customer;
import com.studiomusik.model.Instrument;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.BookingStatus;
import com.studiomusik.model.enums.InstrumentStatus;
import com.studiomusik.model.enums.RoomStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.studiomusik.service.ServiceTestSupport.bd;
import static com.studiomusik.service.ServiceTestSupport.bookingCountForRoom;
import static com.studiomusik.service.ServiceTestSupport.futureDate;
import static com.studiomusik.service.ServiceTestSupport.qty;
import static com.studiomusik.service.ServiceTestSupport.t;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tes integrasi BookingService terhadap MySQL (di-skip bila DB tidak siap). Setiap tes memakai Room
 * dan Customer uji sendiri, sehingga tidak bertabrakan dengan booking seed dan tidak memerlukan reset.sql.
 * Cakupan: AC-R03, R05–R09, R11, R12, R14, plus availability, estimasi, dan serialisasi pemesan bersamaan.
 */
class BookingServiceTest {

    private final BookingService service = new BookingService();
    private final BookingDAO bookingDAO = new BookingDAO();

    private Customer customer;
    private Room room;                    // Rp150.000/jam, AVAILABLE
    private final LocalDate date = futureDate(30);

    @BeforeAll
    static void requireDb() {
        ServiceTestSupport.requireDatabaseWithSeed();
        ServiceTestSupport.cleanUp();
    }

    @BeforeEach
    void setUp() throws Exception {
        customer = ServiceTestSupport.newCustomer();
        room = ServiceTestSupport.newRoom("150000.00", RoomStatus.AVAILABLE);
    }

    @AfterEach
    void cleanUp() {
        ServiceTestSupport.cleanUp();
    }

    private Booking book(int startHour, int endHour) {
        return service.createBooking(customer.getId(), room.getRoomId(), date, t(startHour), t(endHour), Map.of());
    }

    // ------------------------------------------------------------ AC-R11 (BR-13)

    @Test
    @DisplayName("AC-R11: booking baru berstatus PENDING dan tersimpan lengkap")
    void newBookingIsPending() throws Exception {
        Booking created = book(10, 12);

        assertTrue(created.getBookingId() > 0);
        assertEquals(BookingStatus.PENDING, created.getStatus());
        assertNotNull(created.getCreatedAt());
        assertEquals(customer.getId(), created.getCustomer().getId());
        assertNull(created.getCustomer().getPasswordHash());
        assertEquals(room.getRoomId(), created.getRoom().getRoomId());
        assertEquals(date, created.getBookingDate());
        assertEquals(t(10), created.getStartTime());
        assertEquals(t(12), created.getEndTime());
        assertTrue(created.getInstruments().isEmpty());

        Booking stored = bookingDAO.findById(created.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.PENDING, stored.getStatus());
        assertEquals(bd("300000.00"), stored.getTotalCost());
    }

    // ------------------------------------------------------------ AC-R09 (BR-09/10/11)

    @Test
    @DisplayName("AC-R09: biaya ruangan = harga/jam x durasi (tanpa alat)")
    void roomCostIsPriceTimesDuration() {
        assertEquals(bd("450000.00"), service.estimateTotalCost(room.getRoomId(), t(9), t(12), Map.of()));
        assertEquals(bd("450000.00"), service.estimateTotalCost(room.getRoomId(), t(9), t(12), null));
        assertEquals(bd("150000.00"), service.estimateTotalCost(room.getRoomId(), t(22), t(23), Map.of()));
        assertEquals(bd("450000.00"), book(9, 12).getTotalCost());
    }

    @Test
    @DisplayName("AC-R09: biaya alat = quantity x harga; total = ruangan + alat (HALF_UP, skala 2); estimasi = tersimpan")
    void instrumentCostAndTotal() throws Exception {
        Instrument guitar = ServiceTestSupport.newInstrument("12345.67", InstrumentStatus.AVAILABLE);
        Instrument drum = ServiceTestSupport.newInstrument("25000.00", InstrumentStatus.AVAILABLE);
        Map<Integer, Integer> items = qty(guitar.getInstrumentId(), 3, drum.getInstrumentId(), 1);

        // 2 jam x 150000 = 300000; gitar 3 x 12345.67 = 37037.01; drum 1 x 25000 = 25000 => 362037.01
        BigDecimal expected = bd("362037.01");
        BigDecimal estimate = service.estimateTotalCost(room.getRoomId(), t(10), t(12), items);
        assertEquals(expected, estimate);
        assertEquals(2, estimate.scale());

        Booking created = service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12), items);
        assertEquals(expected, created.getTotalCost());
        assertEquals(2, created.getInstruments().size());
        assertEquals(expected, bookingDAO.findById(created.getBookingId()).orElseThrow().getTotalCost());

        for (BookingInstrument detail : created.getInstruments()) {
            BigDecimal unit = detail.getInstrument().getInstrumentId() == guitar.getInstrumentId()
                    ? bd("12345.67") : bd("25000.00");
            assertEquals(unit, detail.getRentalPrice(), "snapshot harga satuan");
        }
    }

    @Test
    @DisplayName("Estimasi bersifat read-only: tidak ada booking tersimpan")
    void estimateDoesNotPersist() {
        service.estimateTotalCost(room.getRoomId(), t(10), t(12), Map.of());
        assertEquals(0, countBookings());
    }

    // ------------------------------------------------------------ AC-R05 / R08 (BR-07/08)

    @Test
    @DisplayName("AC-R05/R08: overlap sebagian, mengandung, identik, dan terkandung ditolak (PENDING memblokir)")
    void overlappingIsRejectedWhenPending() {
        book(10, 12);

        int[][] overlapping = {{11, 13}, {9, 11}, {10, 12}, {9, 13}, {10, 11}, {11, 12}};
        for (int[] slot : overlapping) {
            String label = slot[0] + "-" + slot[1];
            assertFalse(service.isRoomAvailable(room.getRoomId(), date, t(slot[0]), t(slot[1])), label);
            assertThrows(BusinessRuleException.class,
                    () -> service.createBooking(customer.getId(), room.getRoomId(), date, t(slot[0]), t(slot[1]),
                            Map.of()), label);
        }
        assertEquals(1, countBookings(), "tidak ada booking tambahan yang tersimpan");
    }

    @Test
    @DisplayName("AC-R05: booking CONFIRMED juga memblokir jadwal")
    void confirmedBlocks() {
        Booking first = book(10, 12);
        service.updateBookingStatus(first.getBookingId(), BookingStatus.CONFIRMED);

        assertFalse(service.isRoomAvailable(room.getRoomId(), date, t(11), t(13)));
        assertThrows(BusinessRuleException.class, () -> book(11, 13));
    }

    // ------------------------------------------------------------ AC-R07 (BR-08)

    @Test
    @DisplayName("AC-R07: booking berurutan (akhir = awal berikutnya) diterima di kedua sisi")
    void backToBackIsAccepted() {
        book(10, 12);

        assertTrue(service.isRoomAvailable(room.getRoomId(), date, t(12), t(14)));
        assertTrue(service.isRoomAvailable(room.getRoomId(), date, t(8), t(10)));
        Booking after = book(12, 14);
        Booking before = book(8, 10);
        assertEquals(BookingStatus.PENDING, after.getStatus());
        assertEquals(BookingStatus.PENDING, before.getStatus());
        assertEquals(3, countBookings());
    }

    @Test
    @DisplayName("Jam sama di tanggal lain atau di room lain tidak bertabrakan")
    void otherDateOrRoomDoesNotCollide() throws Exception {
        book(10, 12);
        Room other = ServiceTestSupport.newRoom("100000.00", RoomStatus.AVAILABLE);

        assertTrue(service.isRoomAvailable(room.getRoomId(), date.plusDays(1), t(10), t(12)));
        assertTrue(service.isRoomAvailable(other.getRoomId(), date, t(10), t(12)));
        service.createBooking(customer.getId(), other.getRoomId(), date, t(10), t(12), Map.of());
        service.createBooking(customer.getId(), room.getRoomId(), date.plusDays(1), t(10), t(12), Map.of());
    }

    // ------------------------------------------------------------ AC-R06 (BR-07)

    @Test
    @DisplayName("AC-R06: booking CANCELLED tidak memblokir jadwal")
    void cancelledDoesNotBlock() {
        Booking first = book(10, 12);
        service.updateBookingStatus(first.getBookingId(), BookingStatus.CANCELLED);

        assertTrue(service.isRoomAvailable(room.getRoomId(), date, t(10), t(12)));
        assertEquals(BookingStatus.PENDING, book(10, 12).getStatus());
    }

    @Test
    @DisplayName("AC-R06: booking COMPLETED tidak memblokir jadwal")
    void completedDoesNotBlock() {
        Booking first = book(10, 12);
        service.updateBookingStatus(first.getBookingId(), BookingStatus.CONFIRMED);
        service.updateBookingStatus(first.getBookingId(), BookingStatus.COMPLETED);

        assertTrue(service.isRoomAvailable(room.getRoomId(), date, t(10), t(12)));
        assertEquals(BookingStatus.PENDING, book(10, 12).getStatus());
    }

    @Test
    @DisplayName("Jadwal seed Studio A 2026-12-01 10-12 (CONFIRMED) memblokir; CANCELLED seed tidak jadi alasan")
    void seedScheduleIsRespected() {
        LocalDate seedDate = LocalDate.of(2026, 12, 1);
        // tanggal seed yang sudah lewat ditolak BR-21 lebih dulu (D-027): tes ini dilewati saat itu
        Assumptions.assumeFalse(seedDate.isBefore(LocalDate.now()), "tanggal seed sudah lewat");
        assertFalse(service.isRoomAvailable(1, seedDate, t(10), t(12)));
        assertFalse(service.isRoomAvailable(1, seedDate, t(11), t(13)));
        assertTrue(service.isRoomAvailable(1, seedDate, t(14), t(16)), "setelah booking 2 (12-14)");
    }

    // ------------------------------------------------------------ AC-R03 (BR-05) & AC-R04 (BR-06)

    @Test
    @DisplayName("AC-R03: Room MAINTENANCE tidak dapat dipesan (cek, estimasi, create); tidak ada yang tersimpan")
    void maintenanceRoomIsRejected() throws Exception {
        Room closed = ServiceTestSupport.newRoom("100000.00", RoomStatus.MAINTENANCE);

        assertThrows(BusinessRuleException.class,
                () -> service.isRoomAvailable(closed.getRoomId(), date, t(10), t(12)));
        assertThrows(BusinessRuleException.class,
                () -> service.estimateTotalCost(closed.getRoomId(), t(10), t(12), Map.of()));
        assertThrows(BusinessRuleException.class,
                () -> service.createBooking(customer.getId(), closed.getRoomId(), date, t(10), t(12), Map.of()));
        assertEquals(0, bookingCountForRoom(closed.getRoomId()));
    }

    @Test
    @DisplayName("AC-R03: Studio D seed (MAINTENANCE, id 5) tidak dapat dipesan")
    void seedMaintenanceRoomIsRejected() {
        assertThrows(BusinessRuleException.class,
                () -> service.createBooking(customer.getId(), 5, date, t(10), t(12), Map.of()));
    }

    @Test
    @DisplayName("AC-R04: Instrument MAINTENANCE tidak dapat dipilih (estimasi dan create); tidak ada booking tersimpan")
    void maintenanceInstrumentIsRejected() throws Exception {
        Instrument closed = ServiceTestSupport.newInstrument("25000.00", InstrumentStatus.MAINTENANCE);
        Instrument open = ServiceTestSupport.newInstrument("25000.00", InstrumentStatus.AVAILABLE);
        Map<Integer, Integer> items = qty(open.getInstrumentId(), 1, closed.getInstrumentId(), 1);

        assertThrows(BusinessRuleException.class, () -> service.estimateTotalCost(room.getRoomId(), t(10), t(12), items));
        assertThrows(BusinessRuleException.class,
                () -> service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12), items));
        assertEquals(0, countBookings(), "tidak ada booking (atau detail) yang tersisa");
    }

    @Test
    @DisplayName("Id room/customer/alat yang tidak ada -> ValidationException; tidak ada yang tersimpan")
    void unknownReferencesAreRejected() throws Exception {
        assertThrows(ValidationException.class,
                () -> service.createBooking(customer.getId(), 999_999_999, date, t(10), t(12), Map.of()));
        assertThrows(ValidationException.class,
                () -> service.createBooking(999_999_999, room.getRoomId(), date, t(10), t(12), Map.of()));
        assertThrows(ValidationException.class,
                () -> service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12),
                        qty(999_999_999, 1)));
        assertThrows(ValidationException.class, () -> service.isRoomAvailable(999_999_999, date, t(10), t(12)));
        assertEquals(0, countBookings());
    }

    // ------------------------------------------------------------ AC-R12 (BR-14)

    @Test
    @DisplayName("AC-R12: transisi valid diterima (PENDING->CONFIRMED->COMPLETED, PENDING->CANCELLED, CONFIRMED->CANCELLED)")
    void validTransitionsAreAccepted() {
        Booking a = book(8, 9);
        service.updateBookingStatus(a.getBookingId(), BookingStatus.CONFIRMED);
        assertEquals(BookingStatus.CONFIRMED, statusOf(a));
        service.updateBookingStatus(a.getBookingId(), BookingStatus.COMPLETED);
        assertEquals(BookingStatus.COMPLETED, statusOf(a));

        Booking b = book(10, 11);
        service.updateBookingStatus(b.getBookingId(), BookingStatus.CANCELLED);
        assertEquals(BookingStatus.CANCELLED, statusOf(b));

        Booking c = book(12, 13);
        service.updateBookingStatus(c.getBookingId(), BookingStatus.CONFIRMED);
        service.updateBookingStatus(c.getBookingId(), BookingStatus.CANCELLED);
        assertEquals(BookingStatus.CANCELLED, statusOf(c));
    }

    @Test
    @DisplayName("AC-R12: transisi tidak valid ditolak (COMPLETED->PENDING, final, loncat, sama) dan status tidak berubah")
    void invalidTransitionsAreRejected() {
        Booking completed = book(8, 9);
        service.updateBookingStatus(completed.getBookingId(), BookingStatus.CONFIRMED);
        service.updateBookingStatus(completed.getBookingId(), BookingStatus.COMPLETED);
        for (BookingStatus target : BookingStatus.values()) {
            assertThrows(BusinessRuleException.class,
                    () -> service.updateBookingStatus(completed.getBookingId(), target), "COMPLETED->" + target);
        }
        assertEquals(BookingStatus.COMPLETED, statusOf(completed));

        Booking cancelled = book(10, 11);
        service.updateBookingStatus(cancelled.getBookingId(), BookingStatus.CANCELLED);
        for (BookingStatus target : BookingStatus.values()) {
            assertThrows(BusinessRuleException.class,
                    () -> service.updateBookingStatus(cancelled.getBookingId(), target), "CANCELLED->" + target);
        }

        Booking pending = book(12, 13);
        assertThrows(BusinessRuleException.class,
                () -> service.updateBookingStatus(pending.getBookingId(), BookingStatus.COMPLETED));
        assertThrows(BusinessRuleException.class,
                () -> service.updateBookingStatus(pending.getBookingId(), BookingStatus.PENDING));
        assertEquals(BookingStatus.PENDING, statusOf(pending));

        Booking confirmed = book(14, 15);
        service.updateBookingStatus(confirmed.getBookingId(), BookingStatus.CONFIRMED);
        assertThrows(BusinessRuleException.class,
                () -> service.updateBookingStatus(confirmed.getBookingId(), BookingStatus.PENDING));
        assertEquals(BookingStatus.CONFIRMED, statusOf(confirmed));
    }

    @Test
    @DisplayName("updateBookingStatus: booking tidak ada -> ValidationException")
    void statusOfUnknownBooking() {
        assertThrows(ValidationException.class,
                () -> service.updateBookingStatus(999_999_999, BookingStatus.CONFIRMED));
    }

    // ------------------------------------------------------------ AC-R14 (BR-16)

    @Test
    @DisplayName("AC-R14: Customer A tidak dapat membaca booking Customer B; pemilik dapat (dengan detail alat)")
    void customersSeeOnlyTheirOwnBookings() throws Exception {
        Customer other = ServiceTestSupport.newCustomer();
        Instrument item = ServiceTestSupport.newInstrument("25000.00", InstrumentStatus.AVAILABLE);
        Booking mine = service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12),
                qty(item.getInstrumentId(), 2));

        Booking read = service.getBookingForCustomer(mine.getBookingId(), customer.getId());
        assertEquals(mine.getBookingId(), read.getBookingId());
        assertEquals(1, read.getInstruments().size());

        BusinessRuleException denied = assertThrows(BusinessRuleException.class,
                () -> service.getBookingForCustomer(mine.getBookingId(), other.getId()));
        BusinessRuleException missing = assertThrows(BusinessRuleException.class,
                () -> service.getBookingForCustomer(999_999_999, other.getId()));
        assertEquals(missing.getMessage(), denied.getMessage(), "keberadaan booking orang lain tidak terungkap");

        assertTrue(service.getBookingsByCustomer(customer.getId()).stream()
                .anyMatch(b -> b.getBookingId() == mine.getBookingId()));
        assertTrue(service.getBookingsByCustomer(other.getId()).isEmpty());
    }

    // ------------------------------------------------------------ pembacaan & ringkasan

    @Test
    @DisplayName("getAllBookings memuat booking baru (terbaru lebih dulu); getBookingDetail memuat alat; id tidak ada -> ValidationException")
    void readAllAndDetail() throws Exception {
        Instrument item = ServiceTestSupport.newInstrument("25000.00", InstrumentStatus.AVAILABLE);
        Booking later = service.createBooking(customer.getId(), room.getRoomId(), date.plusDays(1), t(10), t(12),
                qty(item.getInstrumentId(), 1));
        Booking earlier = book(10, 12);

        List<Integer> ids = new ArrayList<>();
        service.getAllBookings().forEach(b -> ids.add(b.getBookingId()));
        assertTrue(ids.contains(later.getBookingId()) && ids.contains(earlier.getBookingId()));
        assertTrue(ids.indexOf(later.getBookingId()) < ids.indexOf(earlier.getBookingId()), "tanggal terbaru dulu");

        Booking detail = service.getBookingDetail(later.getBookingId());
        assertEquals(1, detail.getInstruments().size());
        assertEquals(item.getInstrumentId(), detail.getInstruments().get(0).getInstrument().getInstrumentId());
        assertThrows(ValidationException.class, () -> service.getBookingDetail(999_999_999));
    }

    @Test
    @DisplayName("getBookingStatusSummary: keempat status selalu ada dan berubah mengikuti transisi")
    void statusSummary() {
        Map<BookingStatus, Long> before = service.getBookingStatusSummary();
        for (BookingStatus status : BookingStatus.values()) {
            assertTrue(before.containsKey(status), "status " + status);
            assertTrue(before.get(status) >= 0);
        }

        Booking created = book(10, 12);
        Map<BookingStatus, Long> afterCreate = service.getBookingStatusSummary();
        assertEquals(before.get(BookingStatus.PENDING) + 1, afterCreate.get(BookingStatus.PENDING));

        service.updateBookingStatus(created.getBookingId(), BookingStatus.CONFIRMED);
        Map<BookingStatus, Long> afterConfirm = service.getBookingStatusSummary();
        assertEquals(before.get(BookingStatus.PENDING), afterConfirm.get(BookingStatus.PENDING));
        assertEquals(before.get(BookingStatus.CONFIRMED) + 1, afterConfirm.get(BookingStatus.CONFIRMED));
    }

    // ------------------------------------------------------------ BR-15 / D-019: serialisasi

    @Test
    @DisplayName("Pemesan bersamaan pada slot yang sama: tepat satu berhasil (kunci room sebelum cek overlap)")
    void concurrentRequestsForSameSlotYieldOneBooking() throws Exception {
        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Boolean> task = () -> {
                    ready.countDown();
                    go.await();
                    try {
                        service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12), Map.of());
                        return true;
                    } catch (BusinessRuleException e) {
                        return false;
                    }
                };
                results.add(pool.submit(task));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS));
            go.countDown();

            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get(60, TimeUnit.SECONDS)) {
                    successes++;
                }
            }
            assertEquals(1, successes);
            assertEquals(1, countBookings());
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------ util tes

    private long countBookings() {
        try {
            return bookingCountForRoom(room.getRoomId());
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private BookingStatus statusOf(Booking booking) {
        return service.getBookingDetail(booking.getBookingId()).getStatus();
    }
}
