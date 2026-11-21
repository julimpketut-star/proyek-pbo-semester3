package com.studiomusik.dao;

import com.studiomusik.model.Booking;
import com.studiomusik.model.Customer;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.BookingStatus;
import com.studiomusik.model.enums.RoomStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tes BookingDAO terhadap seed STEP 2 (baca) dan transaksi-rollback (tulis). */
class BookingDAOTest {

    private static final LocalDate DEC_1 = LocalDate.of(2026, 12, 1);

    private final BookingDAO dao = new BookingDAO();

    @BeforeAll
    static void requireDb() {
        DaoTestSupport.requireDatabaseWithSeed();
    }

    private static LocalTime t(int hour) {
        return LocalTime.of(hour, 0);
    }

    private static Booking newBooking(int customerId, int roomId, LocalDate date, int startHour, int endHour) {
        Customer c = new Customer(customerId, "x", "x", "x@example.com", null);
        Room r = new Room(roomId, "x", "x", BigDecimal.ONE, RoomStatus.AVAILABLE);
        return new Booking(c, r, date, t(startHour), t(endHour));
    }

    // ------------------------------------------------------------- findById

    @Test
    @DisplayName("findById(1): pemetaan seluruh kolom + JOIN customer & room; tanpa detail alat; hash tidak bocor")
    void findByIdBooking1() throws Exception {
        Booking b = dao.findById(1).orElseThrow();
        assertEquals(1, b.getBookingId());
        assertEquals(DEC_1, b.getBookingDate());
        assertEquals(t(10), b.getStartTime());
        assertEquals(t(12), b.getEndTime());
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertEquals(0, new BigDecimal("400000.00").compareTo(b.getTotalCost()));
        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), b.getCreatedAt());

        assertEquals(1, b.getCustomer().getId());
        assertEquals("Made Ayu Lestari", b.getCustomer().getName());
        assertEquals("081234567801", b.getCustomer().getPhone());
        assertEquals("ayu.lestari@example.com", b.getCustomer().getEmail());
        assertNull(b.getCustomer().getPasswordHash());

        assertEquals(1, b.getRoom().getRoomId());
        assertEquals("Studio A", b.getRoom().getRoomName());
        assertEquals("Band Rehearsal", b.getRoom().getRoomType());
        assertEquals(0, new BigDecimal("150000.00").compareTo(b.getRoom().getPricePerHour()));
        assertEquals(RoomStatus.AVAILABLE, b.getRoom().getStatus());

        assertTrue(b.getInstruments().isEmpty(), "detail alat tidak dimuat oleh BookingDAO");
    }

    @Test
    @DisplayName("findById(4) COMPLETED dan findById(5) PENDING di Studio B")
    void findByIdBooking4And5() throws Exception {
        Booking b4 = dao.findById(4).orElseThrow();
        assertEquals(BookingStatus.COMPLETED, b4.getStatus());
        assertEquals(LocalDate.of(2026, 11, 10), b4.getBookingDate());
        assertEquals(t(10), b4.getStartTime());
        assertEquals(t(13), b4.getEndTime());
        assertEquals(0, new BigDecimal("496000.00").compareTo(b4.getTotalCost()));

        Booking b5 = dao.findById(5).orElseThrow();
        assertEquals(BookingStatus.PENDING, b5.getStatus());
        assertEquals(2, b5.getRoom().getRoomId());
        assertEquals("Studio B", b5.getRoom().getRoomName());
        assertEquals(5, b5.getCustomer().getId());
        assertEquals(t(15), b5.getStartTime());
        assertEquals(t(18), b5.getEndTime());
        assertEquals(0, new BigDecimal("470000.00").compareTo(b5.getTotalCost()));
    }

    @Test
    @DisplayName("findById id tidak ada -> Optional kosong")
    void findByIdMissing() throws Exception {
        assertTrue(dao.findById(999_999).isEmpty());
    }

    // ------------------------------------------------- findAll/findByCustomerId

    @Test
    @DisplayName("findAll memuat seed 1..7 dengan urutan booking_date DESC, start_time DESC")
    void findAllOrdered() throws Exception {
        List<Booking> all = dao.findAll();
        assertTrue(all.size() >= 7);
        for (int id = 1; id <= 7; id++) {
            final int expected = id;
            assertTrue(all.stream().anyMatch(b -> b.getBookingId() == expected), "booking " + id);
        }
        for (int i = 1; i < all.size(); i++) {
            Booking prev = all.get(i - 1);
            Booking cur = all.get(i);
            int byDate = prev.getBookingDate().compareTo(cur.getBookingDate());
            assertTrue(byDate >= 0, "tanggal harus menurun");
            if (byDate == 0) {
                assertTrue(prev.getStartTime().compareTo(cur.getStartTime()) >= 0, "jam mulai harus menurun");
            }
        }
        assertTrue(all.stream().allMatch(b -> b.getCustomer() != null && b.getRoom() != null));
    }

    @Test
    @DisplayName("findByCustomerId(1) = booking 6 lalu 1 (terbaru dulu); customer 6 tanpa booking")
    void findByCustomerId() throws Exception {
        List<Booking> mine = dao.findByCustomerId(1);
        assertTrue(mine.stream().allMatch(b -> b.getCustomer().getId() == 1));
        List<Integer> ids = mine.stream().map(Booking::getBookingId).toList();
        assertTrue(ids.containsAll(List.of(1, 6)));
        assertTrue(ids.indexOf(6) < ids.indexOf(1), "2026-12-03 harus sebelum 2026-12-01");
        assertEquals(List.of(2, 7), dao.findByCustomerId(2).stream().map(Booking::getBookingId).sorted().toList());
        assertTrue(dao.findByCustomerId(6).isEmpty());
        assertTrue(dao.findByCustomerId(999_999).isEmpty());
    }

    @Test
    @DisplayName("countByStatus sesuai minimal seed")
    void countByStatus() throws Exception {
        assertTrue(dao.countByStatus(BookingStatus.CONFIRMED) >= 2);
        assertTrue(dao.countByStatus(BookingStatus.PENDING) >= 2);
        assertTrue(dao.countByStatus(BookingStatus.COMPLETED) >= 1);
        assertTrue(dao.countByStatus(BookingStatus.CANCELLED) >= 2);
    }

    // --------------------------------------------------------- existsOverlap

    @Test
    @DisplayName("existsOverlap: sebagian, mengandung, dikandung, identik -> true (Studio A 2026-12-01)")
    void overlapCases() throws Exception {
        assertTrue(dao.existsOverlap(1, DEC_1, t(11), t(13)), "sebagian (11-13)");
        assertTrue(dao.existsOverlap(1, DEC_1, t(9), t(15)), "mengandung (9-15)");
        assertTrue(dao.existsOverlap(1, DEC_1, LocalTime.of(10, 30), LocalTime.of(11, 30)), "dikandung");
        assertTrue(dao.existsOverlap(1, DEC_1, t(10), t(12)), "identik dengan booking 1");
        assertTrue(dao.existsOverlap(1, DEC_1, t(13), t(14)), "dikandung booking 2 (12-14)");
    }

    @Test
    @DisplayName("existsOverlap: booking berurutan (akhir = awal) BUKAN overlap")
    void sequentialIsNotOverlap() throws Exception {
        assertFalse(dao.existsOverlap(1, DEC_1, t(8), t(10)), "berakhir tepat saat booking 1 mulai");
        assertFalse(dao.existsOverlap(1, DEC_1, t(14), t(16)), "mulai tepat saat booking 2 berakhir");
        assertFalse(dao.existsOverlap(2, LocalDate.of(2026, 12, 2), t(18), t(20)),
                "mulai tepat saat booking 5 (15-18) berakhir");
        assertFalse(dao.existsOverlap(2, LocalDate.of(2026, 12, 2), t(13), t(15)),
                "berakhir tepat saat booking 5 mulai");
    }

    @Test
    @DisplayName("existsOverlap: CANCELLED/COMPLETED tidak memblokir; room/tanggal berbeda tidak overlap")
    void nonBlockingAndOtherRoomOrDate() throws Exception {
        assertFalse(dao.existsOverlap(4, LocalDate.of(2026, 12, 5), t(13), t(17)), "hanya booking CANCELLED");
        assertFalse(dao.existsOverlap(1, LocalDate.of(2026, 11, 10), t(10), t(13)), "hanya booking COMPLETED");
        assertFalse(dao.existsOverlap(2, DEC_1, t(10), t(12)), "room lain");
        assertFalse(dao.existsOverlap(1, LocalDate.of(2026, 12, 2), t(10), t(12)), "tanggal lain");
        assertTrue(dao.existsOverlap(2, LocalDate.of(2026, 12, 2), t(16), t(17)), "booking 5 PENDING memblokir");
    }

    // ------------------------------------------- tulis (transaksi + rollback)

    @Test
    @DisplayName("insert + findById(conn) + existsOverlap(conn) + updateStatus + updateTotalCost; koneksi tidak ditutup")
    void insertAndUpdateInTransaction() throws Exception {
        LocalDate date = LocalDate.of(2027, 3, 15);
        DaoTestSupport.inRollbackTx(conn -> {
            Booking nb = newBooking(6, 3, date, 10, 12);
            int id = dao.insert(conn, nb);
            assertTrue(id > 7);

            Booking found = dao.findById(conn, id).orElseThrow();
            assertEquals(BookingStatus.PENDING, found.getStatus());
            assertEquals(0, BigDecimal.ZERO.compareTo(found.getTotalCost()));
            assertEquals(6, found.getCustomer().getId());
            assertEquals("Wayan Tirta Yasa", found.getCustomer().getName());
            assertEquals("Studio C", found.getRoom().getRoomName());
            assertNotNull(found.getCreatedAt(), "created_at default dari database");

            assertTrue(dao.existsOverlap(conn, 3, date, t(11), t(13)));
            assertFalse(dao.existsOverlap(conn, 3, date, t(12), t(14)));

            assertTrue(dao.updateTotalCost(conn, id, new BigDecimal("123456.78")));
            assertEquals(new BigDecimal("123456.78"), dao.findById(conn, id).orElseThrow().getTotalCost());

            assertTrue(dao.updateStatus(conn, id, BookingStatus.CONFIRMED));
            assertEquals(BookingStatus.CONFIRMED, dao.findById(conn, id).orElseThrow().getStatus());
            assertTrue(dao.existsOverlap(conn, 3, date, t(11), t(13)), "CONFIRMED tetap memblokir");

            assertTrue(dao.updateStatus(conn, id, BookingStatus.CANCELLED));
            assertFalse(dao.existsOverlap(conn, 3, date, t(11), t(13)), "CANCELLED tidak memblokir");

            assertFalse(conn.isClosed(), "DAO tidak boleh menutup koneksi transaksi");
            assertFalse(conn.getAutoCommit(), "DAO tidak boleh mengubah autoCommit");
        });
        assertFalse(dao.existsOverlap(3, date, t(10), t(12)), "setelah rollback tidak ada sisa data uji");
    }

    @Test
    @DisplayName("insert dengan createdAt eksplisit tersimpan apa adanya (tanpa pergeseran zona waktu)")
    void insertWithExplicitCreatedAt() throws Exception {
        LocalDateTime created = LocalDateTime.of(2027, 1, 2, 3, 4, 5);
        DaoTestSupport.inRollbackTx(conn -> {
            Booking nb = new Booking(0, new Customer(6, "x", "x", "x@example.com", null),
                    new Room(3, "x", "x", BigDecimal.ONE, RoomStatus.AVAILABLE),
                    LocalDate.of(2027, 3, 16), t(9), t(10), BookingStatus.PENDING,
                    new BigDecimal("80000.00"), created);
            int id = dao.insert(conn, nb);
            Booking found = dao.findById(conn, id).orElseThrow();
            assertEquals(created, found.getCreatedAt());
            assertEquals(LocalDate.of(2027, 3, 16), found.getBookingDate());
            assertEquals(t(9), found.getStartTime());
            assertEquals(0, new BigDecimal("80000.00").compareTo(found.getTotalCost()));
        });
    }

    @Test
    @DisplayName("updateStatus/updateTotalCost pada id tidak ada -> false")
    void updateMissing() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            assertFalse(dao.updateStatus(conn, 999_999, BookingStatus.CANCELLED));
            assertFalse(dao.updateTotalCost(conn, 999_999, BigDecimal.TEN));
        });
    }

    @Test
    @DisplayName("FK: customer/room tidak ada ditolak; CHECK end > start ditolak (SQLException)")
    void constraintsRejected() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            LocalDate date = LocalDate.of(2027, 3, 17);
            SQLException noCustomer = assertThrows(SQLException.class,
                    () -> dao.insert(conn, newBooking(999_999, 3, date, 10, 11)));
            assertTrue(noCustomer.getSQLState().startsWith("23"));
            SQLException noRoom = assertThrows(SQLException.class,
                    () -> dao.insert(conn, newBooking(6, 999_999, date, 10, 11)));
            assertTrue(noRoom.getSQLState().startsWith("23"));

            Booking reversed = new Booking(0, new Customer(6, "x", "x", "x@example.com", null),
                    new Room(3, "x", "x", BigDecimal.ONE, RoomStatus.AVAILABLE), date, t(12), t(10),
                    BookingStatus.PENDING, BigDecimal.ZERO, null);
            assertThrows(SQLException.class, () -> dao.insert(conn, reversed));
            assertFalse(conn.isClosed());
        });
    }
}
