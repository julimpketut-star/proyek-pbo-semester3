package com.studiomusik.dao;

import com.studiomusik.model.BookingInstrument;
import com.studiomusik.model.Instrument;
import com.studiomusik.model.enums.InstrumentStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tes CRUD relasi M:N. Booking 6 (Studio C, seed) tidak punya detail alat sehingga dipakai sebagai
 * sasaran tulis di dalam transaksi yang selalu di-rollback.
 */
class BookingInstrumentDAOTest {

    private static final int EMPTY_BOOKING = 6;

    private final BookingInstrumentDAO dao = new BookingInstrumentDAO();

    @BeforeAll
    static void requireDb() {
        DaoTestSupport.requireDatabaseWithSeed();
    }

    private static Instrument ref(int id) {
        return new Instrument(id, "ref", "ref", BigDecimal.ONE, InstrumentStatus.AVAILABLE);
    }

    private static BookingInstrument detail(int bookingId, int instrumentId, int qty, String snapshot) {
        return new BookingInstrument(0, bookingId, ref(instrumentId), qty, new BigDecimal(snapshot));
    }

    // ------------------------------------------------------------------ READ

    @Test
    @DisplayName("READ (JOIN): booking 5 memuat 4 detail alat lengkap dengan data instrument")
    void readBooking5() throws Exception {
        List<BookingInstrument> list = dao.findByBookingId(5);
        assertEquals(4, list.size());
        assertEquals(List.of(7, 8, 9, 10), list.stream().map(BookingInstrument::getBookingInstrumentId).toList());
        assertTrue(list.stream().allMatch(d -> d.getBookingId() == 5));

        BookingInstrument mic = list.get(3);
        assertEquals(5, mic.getInstrument().getInstrumentId());
        assertEquals("Mikrofon Shure SM58", mic.getInstrument().getName());
        assertEquals("Mikrofon", mic.getInstrument().getType());
        assertEquals(InstrumentStatus.AVAILABLE, mic.getInstrument().getStatus());
        assertEquals(1, mic.getQuantity());
        assertEquals(0, new BigDecimal("10000.00").compareTo(mic.getRentalPrice()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(mic.calculateSubtotal()));
    }

    @Test
    @DisplayName("READ: snapshot 8000 pada booking 4 berbeda dari harga instrument sekarang 10000 (BR-12)")
    void snapshotDiffersFromCurrentPrice() throws Exception {
        List<BookingInstrument> list = dao.findByBookingId(4);
        assertEquals(2, list.size());
        BookingInstrument snap = list.get(0);
        assertEquals(5, snap.getBookingInstrumentId());
        assertEquals(2, snap.getQuantity());
        assertEquals(0, new BigDecimal("8000.00").compareTo(snap.getRentalPrice()), "snapshot dari booking_instrument");
        assertEquals(0, new BigDecimal("10000.00").compareTo(snap.getInstrument().getRentalPrice()),
                "harga sekarang dari instrument");
        assertEquals(0, new BigDecimal("16000.00").compareTo(snap.calculateSubtotal()));
    }

    @Test
    @DisplayName("READ: booking tanpa alat / tidak ada -> list kosong")
    void readEmpty() throws Exception {
        assertTrue(dao.findByBookingId(EMPTY_BOOKING).isEmpty());
        assertTrue(dao.findByBookingId(999_999).isEmpty());
    }

    @Test
    @DisplayName("READ: findById dan findByBookingIdAndInstrumentId (koneksi pemanggil tidak ditutup)")
    void readByIds() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            BookingInstrument byId = dao.findById(conn, 5).orElseThrow();
            assertEquals(4, byId.getBookingId());
            assertEquals(5, byId.getInstrument().getInstrumentId());

            BookingInstrument byPair = dao.findByBookingIdAndInstrumentId(conn, 4, 5).orElseThrow();
            assertEquals(5, byPair.getBookingInstrumentId());

            assertTrue(dao.findById(conn, 999_999).isEmpty());
            assertTrue(dao.findByBookingIdAndInstrumentId(conn, 4, 1).isEmpty());
            assertEquals(2, dao.findByBookingId(conn, 4).size());
            assertFalse(conn.isClosed());
            assertFalse(conn.getAutoCommit());
        });
    }

    // ---------------------------------------------------------------- CREATE

    @Test
    @DisplayName("CREATE: insert mengembalikan generated key; baris dapat dibaca kembali lewat JOIN")
    void createDetail() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            int id = dao.insert(conn, detail(EMPTY_BOOKING, 2, 3, "25000.00"));
            assertTrue(id > 12);
            BookingInstrument read = dao.findById(conn, id).orElseThrow();
            assertEquals(EMPTY_BOOKING, read.getBookingId());
            assertEquals(2, read.getInstrument().getInstrumentId());
            assertEquals("Gitar Elektrik Fender", read.getInstrument().getName());
            assertEquals(3, read.getQuantity());
            assertEquals(0, new BigDecimal("25000.00").compareTo(read.getRentalPrice()));
            assertEquals(1, dao.findByBookingId(conn, EMPTY_BOOKING).size());
            assertFalse(conn.isClosed());
            assertFalse(conn.getAutoCommit());
        });
        assertTrue(dao.findByBookingId(EMPTY_BOOKING).isEmpty(), "rollback: tidak ada sisa data uji");
    }

    @Test
    @DisplayName("CREATE: insertAll (batch) memakai bookingId parameter dan menyimpan snapshot tiap detail")
    void createAll() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            dao.insertAll(conn, EMPTY_BOOKING, List.of(
                    detail(0, 1, 1, "50000.00"),
                    detail(0, 3, 2, "25000.00"),
                    detail(0, 4, 1, "35000.00")));
            List<BookingInstrument> list = dao.findByBookingId(conn, EMPTY_BOOKING);
            assertEquals(3, list.size());
            assertEquals(List.of(1, 3, 4), list.stream().map(d -> d.getInstrument().getInstrumentId()).toList());
            assertEquals(List.of(1, 2, 1), list.stream().map(BookingInstrument::getQuantity).toList());
            assertTrue(list.stream().allMatch(d -> d.getBookingId() == EMPTY_BOOKING));
            assertEquals(0, new BigDecimal("35000.00").compareTo(list.get(2).getRentalPrice()));

            dao.insertAll(conn, EMPTY_BOOKING, List.of());
            assertEquals(3, dao.findByBookingId(conn, EMPTY_BOOKING).size(), "list kosong = tidak berbuat apa-apa");
        });
    }

    @Test
    @DisplayName("CREATE: pasangan (booking, instrument) duplikat ditolak (uq_booking_instrument_pair)")
    void duplicatePairRejected() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            dao.insert(conn, detail(EMPTY_BOOKING, 2, 1, "25000.00"));
            SQLException dup = assertThrows(SQLException.class,
                    () -> dao.insert(conn, detail(EMPTY_BOOKING, 2, 2, "25000.00")));
            assertTrue(dup.getSQLState().startsWith("23"), "SQLState: " + dup.getSQLState());
            assertThrows(SQLException.class, () -> dao.insertAll(conn, EMPTY_BOOKING,
                    List.of(detail(0, 2, 1, "25000.00"))));
            assertEquals(1, dao.findByBookingId(conn, EMPTY_BOOKING).size());
        });
    }

    @Test
    @DisplayName("CREATE: FK booking/instrument tidak ada ditolak")
    void foreignKeysRejected() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            assertThrows(SQLException.class, () -> dao.insert(conn, detail(999_999, 2, 1, "25000.00")));
            assertThrows(SQLException.class, () -> dao.insert(conn, detail(EMPTY_BOOKING, 999_999, 1, "25000.00")));
        });
    }

    // ---------------------------------------------------------------- UPDATE

    @Test
    @DisplayName("UPDATE quantity: rental_price snapshot TIDAK berubah walau harga instrument sudah berubah (BR-12)")
    void updateQuantityKeepsSnapshot() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            int id = dao.insert(conn, detail(EMPTY_BOOKING, 2, 1, "25000.00"));
            // harga instrument berubah SETELAH booking dibuat
            DaoTestSupport.exec(conn, "UPDATE instrument SET rental_price = ? WHERE instrument_id = ?",
                    new BigDecimal("99999.00"), 2);

            assertTrue(dao.updateQuantity(conn, id, 5));

            BookingInstrument after = dao.findById(conn, id).orElseThrow();
            assertEquals(5, after.getQuantity());
            assertEquals(0, new BigDecimal("25000.00").compareTo(after.getRentalPrice()),
                    "snapshot harus tetap 25000.00");
            assertEquals(0, new BigDecimal("99999.00").compareTo(after.getInstrument().getRentalPrice()),
                    "harga instrument sekarang berubah, tetapi tidak memengaruhi snapshot");
            assertEquals(0, new BigDecimal("125000.00").compareTo(after.calculateSubtotal()));
            assertFalse(conn.isClosed());
        });
    }

    @Test
    @DisplayName("UPDATE quantity pada seed (booking_instrument 5): snapshot 8000 tetap; id tidak ada -> false")
    void updateQuantityOnSeedRow() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            assertTrue(dao.updateQuantity(conn, 5, 7));
            BookingInstrument after = dao.findById(conn, 5).orElseThrow();
            assertEquals(7, after.getQuantity());
            assertEquals(0, new BigDecimal("8000.00").compareTo(after.getRentalPrice()));
            assertFalse(dao.updateQuantity(conn, 999_999, 2));
        });
        assertEquals(2, dao.findByBookingId(4).get(0).getQuantity(), "seed utuh setelah rollback");
    }

    @Test
    @DisplayName("UPDATE quantity 0 ditolak database (chk_bi_quantity)")
    void updateQuantityZeroRejectedByDatabase() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            int id = dao.insert(conn, detail(EMPTY_BOOKING, 2, 1, "25000.00"));
            assertThrows(SQLException.class, () -> dao.updateQuantity(conn, id, 0));
            assertEquals(1, dao.findById(conn, id).orElseThrow().getQuantity());
        });
    }

    // ---------------------------------------------------------------- DELETE

    @Test
    @DisplayName("DELETE: detail terhapus; hapus lagi -> false; detail lain tidak ikut terhapus")
    void deleteDetail() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            int a = dao.insert(conn, detail(EMPTY_BOOKING, 2, 1, "25000.00"));
            int b = dao.insert(conn, detail(EMPTY_BOOKING, 3, 1, "25000.00"));
            assertTrue(dao.delete(conn, a));
            assertTrue(dao.findById(conn, a).isEmpty());
            assertTrue(dao.findById(conn, b).isPresent());
            assertFalse(dao.delete(conn, a));
            assertEquals(1, dao.findByBookingId(conn, EMPTY_BOOKING).size());
            assertFalse(conn.isClosed());
        });
    }

    @Test
    @DisplayName("CASCADE: hapus booking menghapus detailnya (di dalam transaksi, lalu rollback)")
    void cascadeOnBookingDelete() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            assertEquals(4, dao.findByBookingId(conn, 5).size());
            DaoTestSupport.exec(conn, "DELETE FROM booking WHERE booking_id = ?", 5);
            assertTrue(dao.findByBookingId(conn, 5).isEmpty());
        });
        assertEquals(4, dao.findByBookingId(5).size(), "rollback memulihkan seed");
    }

    @Test
    @DisplayName("RESTRICT: instrument yang dipakai detail tidak dapat dihapus (SQLException)")
    void instrumentInUseRestricted() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            SQLException e = assertThrows(SQLException.class,
                    () -> DaoTestSupport.exec(conn, "DELETE FROM instrument WHERE instrument_id = ?", 5));
            assertTrue(e.getSQLState().startsWith("23"), "SQLState: " + e.getSQLState());
        });
    }
}
