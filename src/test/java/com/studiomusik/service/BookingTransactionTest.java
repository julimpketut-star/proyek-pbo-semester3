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
import com.studiomusik.model.enums.InstrumentStatus;
import com.studiomusik.model.enums.RoomStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.studiomusik.service.ServiceTestSupport.bd;
import static com.studiomusik.service.ServiceTestSupport.bookingCountForRoom;
import static com.studiomusik.service.ServiceTestSupport.count;
import static com.studiomusik.service.ServiceTestSupport.detailCountForBooking;
import static com.studiomusik.service.ServiceTestSupport.futureDate;
import static com.studiomusik.service.ServiceTestSupport.qty;
import static com.studiomusik.service.ServiceTestSupport.t;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AC-R13 (BR-15): transaksi atomik. Kegagalan disimulasikan dengan DAO turunan yang melempar
 * {@link SQLException} SETELAH sebagian penulisan benar-benar terjadi pada koneksi transaksi, sehingga
 * tes membuktikan rollback sungguhan (bukan sekadar tidak ada penulisan).
 */
class BookingTransactionTest {

    private final LocalDate date = futureDate(30);
    private Customer customer;
    private Room room;
    private Instrument guitar;
    private Instrument drum;

    /** Menyisipkan detail pertama dengan benar, lalu gagal pada detail berikutnya. */
    private static final class FailAfterFirstDetailDAO extends BookingInstrumentDAO {
        @Override
        public void insertAll(Connection conn, int bookingId, List<BookingInstrument> details) throws SQLException {
            BookingInstrument first = details.get(0);
            insert(conn, new BookingInstrument(0, bookingId, first.getInstrument(), first.getQuantity(),
                    first.getRentalPrice()));
            throw new SQLException("simulasi: insert detail kedua gagal");
        }
    }

    /** Gagal saat memperbarui total_cost (langkah terakhir CRUD detail). */
    private static final class FailingTotalDAO extends BookingDAO {
        @Override
        public boolean updateTotalCost(Connection conn, int bookingId, BigDecimal totalCost) throws SQLException {
            throw new SQLException("simulasi: update total_cost gagal");
        }
    }

    @BeforeAll
    static void requireDb() {
        ServiceTestSupport.requireDatabaseWithSeed();
        ServiceTestSupport.cleanUp();
    }

    @BeforeEach
    void setUp() throws Exception {
        customer = ServiceTestSupport.newCustomer();
        room = ServiceTestSupport.newRoom("150000.00", RoomStatus.AVAILABLE);
        guitar = ServiceTestSupport.newInstrument("25000.00", InstrumentStatus.AVAILABLE);
        drum = ServiceTestSupport.newInstrument("50000.00", InstrumentStatus.AVAILABLE);
    }

    @AfterEach
    void cleanUp() {
        ServiceTestSupport.cleanUp();
    }

    private BookingService failingInsertAllService() {
        return new BookingService(new BookingDAO(), new FailAfterFirstDetailDAO(), new RoomDAO(),
                new InstrumentDAO(), new CustomerDAO());
    }

    @Test
    @DisplayName("AC-R13: gagal menyimpan satu detail alat membatalkan seluruh booking (tidak ada booking yatim)")
    void failedDetailInsertRollsBackWholeBooking() throws Exception {
        Map<Integer, Integer> items = qty(guitar.getInstrumentId(), 1, drum.getInstrumentId(), 2);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> failingInsertAllService().createBooking(customer.getId(), room.getRoomId(), date,
                        t(10), t(12), items));

        assertEquals(ServiceException.class, ex.getClass(), "kegagalan teknis, bukan Validation/BusinessRule");
        assertFalse(ex.getMessage().contains("simulasi"), "detail teknis tidak bocor ke pesan pengguna");
        assertTrue(ex.getCause() instanceof SQLException);

        assertEquals(0, bookingCountForRoom(room.getRoomId()), "booking ikut di-rollback");
        assertEquals(0, count("SELECT COUNT(*) FROM booking_instrument WHERE instrument_id IN (?, ?)",
                guitar.getInstrumentId(), drum.getInstrumentId()), "detail pertama yang sempat masuk ikut di-rollback");
        assertEquals(0, count("SELECT COUNT(*) FROM booking WHERE customer_id = ?", customer.getId()));
    }

    @Test
    @DisplayName("AC-R13: setelah rollback, slot yang sama masih dapat dipesan (kunci/transaksi tidak tertinggal)")
    void slotIsReusableAfterRollback() throws Exception {
        assertThrows(ServiceException.class,
                () -> failingInsertAllService().createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12),
                        qty(guitar.getInstrumentId(), 1)));

        BookingService healthy = new BookingService();
        assertTrue(healthy.isRoomAvailable(room.getRoomId(), date, t(10), t(12)));
        Booking created = healthy.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12),
                qty(guitar.getInstrumentId(), 1, drum.getInstrumentId(), 2));
        assertNotNull(created.getCreatedAt());
        assertEquals(2, detailCountForBooking(created.getBookingId()));
        assertEquals(bd("425000.00"), created.getTotalCost(), "300000 + 25000 + 2 x 50000");
    }

    @Test
    @DisplayName("Booking tanpa alat tidak memanggil insert detail dan tersimpan utuh")
    void bookingWithoutInstrumentsCommits() throws Exception {
        // DAO palsu akan gagal bila insertAll dipanggil; booking tanpa alat tidak boleh memicunya
        Booking created = failingInsertAllService().createBooking(customer.getId(), room.getRoomId(), date,
                t(10), t(12), Map.of());
        assertEquals(BookingStatus.PENDING, created.getStatus());
        assertEquals(1, bookingCountForRoom(room.getRoomId()));
        assertEquals(0, detailCountForBooking(created.getBookingId()));
    }

    @Test
    @DisplayName("BR-19/BR-15: gagal menghitung ulang total membatalkan penambahan detail (atomik)")
    void failedRecalculationRollsBackDetailInsert() throws Exception {
        Booking booking = new BookingService().createBooking(customer.getId(), room.getRoomId(), date,
                t(10), t(12), Map.of());
        BookingService failing = new BookingService(new FailingTotalDAO(), new BookingInstrumentDAO(), new RoomDAO(),
                new InstrumentDAO(), new CustomerDAO());

        assertThrows(ServiceException.class,
                () -> failing.addInstrumentToBooking(booking.getBookingId(), guitar.getInstrumentId(), 2));

        assertEquals(0, detailCountForBooking(booking.getBookingId()), "detail yang sempat di-insert di-rollback");
        assertEquals(bd("300000.00"), new BookingDAO().findById(booking.getBookingId()).orElseThrow().getTotalCost());
    }

    @Test
    @DisplayName("BR-19/BR-15: gagal menghitung ulang total membatalkan ubah dan hapus detail")
    void failedRecalculationRollsBackUpdateAndDelete() throws Exception {
        BookingService healthy = new BookingService();
        Booking booking = healthy.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12),
                qty(guitar.getInstrumentId(), 2));
        BookingInstrument detail = healthy.getBookingInstruments(booking.getBookingId()).get(0);
        BookingService failing = new BookingService(new FailingTotalDAO(), new BookingInstrumentDAO(), new RoomDAO(),
                new InstrumentDAO(), new CustomerDAO());

        assertThrows(ServiceException.class,
                () -> failing.updateBookingInstrumentQuantity(detail.getBookingInstrumentId(), 5));
        assertEquals(2, healthy.getBookingInstruments(booking.getBookingId()).get(0).getQuantity());

        assertThrows(ServiceException.class, () -> failing.removeInstrumentFromBooking(detail.getBookingInstrumentId()));
        assertEquals(1, detailCountForBooking(booking.getBookingId()));
        assertEquals(bd("350000.00"), new BookingDAO().findById(booking.getBookingId()).orElseThrow().getTotalCost());
    }
}
