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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.studiomusik.service.ServiceTestSupport.bd;
import static com.studiomusik.service.ServiceTestSupport.detailCountForBooking;
import static com.studiomusik.service.ServiceTestSupport.futureDate;
import static com.studiomusik.service.ServiceTestSupport.qty;
import static com.studiomusik.service.ServiceTestSupport.t;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tes integrasi CRUD detail BookingInstrument (BookingService): AC-R04 (alat MAINTENANCE), AC-R10
 * (snapshot harga), AC-R15 (duplikat dan quantity &lt; 1), AC-R16 (booking final dan hitung ulang total),
 * serta AC-C01…C04. Room 150000/jam, booking 10-12 = 300000 sebelum alat.
 */
class BookingInstrumentServiceTest {

    private static final BigDecimal ROOM_PART = bd("300000.00");

    private final BookingService service = new BookingService();
    private final BookingDAO bookingDAO = new BookingDAO();

    private Customer customer;
    private Room room;
    private Instrument guitar;   // 25000
    private Instrument drum;     // 50000
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
        guitar = ServiceTestSupport.newInstrument("25000.00", InstrumentStatus.AVAILABLE);
        drum = ServiceTestSupport.newInstrument("50000.00", InstrumentStatus.AVAILABLE);
    }

    @AfterEach
    void cleanUp() {
        ServiceTestSupport.cleanUp();
    }

    private Booking bookWithoutInstruments() {
        return service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12), Map.of());
    }

    private BigDecimal storedTotal(Booking booking) throws SQLException {
        return bookingDAO.findById(booking.getBookingId()).orElseThrow().getTotalCost();
    }

    // ------------------------------------------------------------ CREATE (AC-C01)

    @Test
    @DisplayName("AC-C01: addInstrumentToBooking menambah baris dengan snapshot harga dan menghitung ulang total")
    void addCreatesRowAndRecalculates() throws Exception {
        Booking booking = bookWithoutInstruments();
        assertEquals(ROOM_PART, storedTotal(booking));

        BookingInstrument added = service.addInstrumentToBooking(booking.getBookingId(),
                guitar.getInstrumentId(), 2);

        assertTrue(added.getBookingInstrumentId() > 0);
        assertEquals(booking.getBookingId(), added.getBookingId());
        assertEquals(2, added.getQuantity());
        assertEquals(bd("25000.00"), added.getRentalPrice());
        assertEquals(1, detailCountForBooking(booking.getBookingId()));
        assertEquals(bd("350000.00"), storedTotal(booking), "300000 + 2 x 25000");

        service.addInstrumentToBooking(booking.getBookingId(), drum.getInstrumentId(), 1);
        assertEquals(bd("400000.00"), storedTotal(booking));
        assertEquals(2, detailCountForBooking(booking.getBookingId()));
    }

    @Test
    @DisplayName("BR-19: booking CONFIRMED masih boleh diubah detailnya")
    void confirmedBookingIsEditable() throws Exception {
        Booking booking = bookWithoutInstruments();
        service.updateBookingStatus(booking.getBookingId(), BookingStatus.CONFIRMED);

        service.addInstrumentToBooking(booking.getBookingId(), guitar.getInstrumentId(), 1);
        assertEquals(bd("325000.00"), storedTotal(booking));
    }

    // ------------------------------------------------------------ AC-R15 (BR-17/18)

    @Test
    @DisplayName("AC-R15: menambah alat yang sama dua kali ditolak; total dan baris tidak berubah")
    void duplicateInstrumentIsRejected() throws Exception {
        Booking booking = bookWithoutInstruments();
        service.addInstrumentToBooking(booking.getBookingId(), guitar.getInstrumentId(), 1);

        assertThrows(BusinessRuleException.class,
                () -> service.addInstrumentToBooking(booking.getBookingId(), guitar.getInstrumentId(), 3));
        assertEquals(1, detailCountForBooking(booking.getBookingId()));
        assertEquals(bd("325000.00"), storedTotal(booking));
        assertEquals(1, service.getBookingInstruments(booking.getBookingId()).get(0).getQuantity());
    }

    @Test
    @DisplayName("AC-R15: quantity < 1 ditolak pada tambah dan ubah; data tidak berubah")
    void quantityBelowOneIsRejected() throws Exception {
        Booking booking = bookWithoutInstruments();
        assertThrows(ValidationException.class,
                () -> service.addInstrumentToBooking(booking.getBookingId(), guitar.getInstrumentId(), 0));
        assertThrows(ValidationException.class,
                () -> service.addInstrumentToBooking(booking.getBookingId(), guitar.getInstrumentId(), -3));
        assertEquals(0, detailCountForBooking(booking.getBookingId()));

        BookingInstrument added = service.addInstrumentToBooking(booking.getBookingId(), guitar.getInstrumentId(), 2);
        assertThrows(ValidationException.class,
                () -> service.updateBookingInstrumentQuantity(added.getBookingInstrumentId(), 0));
        assertEquals(2, service.getBookingInstruments(booking.getBookingId()).get(0).getQuantity());
    }

    // ------------------------------------------------------------ AC-R04 (BR-06)

    @Test
    @DisplayName("AC-R04: menambah alat MAINTENANCE ditolak; id alat/booking tidak ada -> ValidationException")
    void maintenanceAndUnknownReferencesAreRejected() throws Exception {
        Instrument closed = ServiceTestSupport.newInstrument("25000.00", InstrumentStatus.MAINTENANCE);
        Booking booking = bookWithoutInstruments();

        assertThrows(BusinessRuleException.class,
                () -> service.addInstrumentToBooking(booking.getBookingId(), closed.getInstrumentId(), 1));
        assertThrows(ValidationException.class,
                () -> service.addInstrumentToBooking(booking.getBookingId(), 999_999_999, 1));
        assertThrows(ValidationException.class,
                () -> service.addInstrumentToBooking(999_999_999, guitar.getInstrumentId(), 1));
        assertEquals(0, detailCountForBooking(booking.getBookingId()));
        assertEquals(ROOM_PART, storedTotal(booking));
    }

    // ------------------------------------------------------------ READ (AC-C02)

    @Test
    @DisplayName("AC-C02: getBookingInstruments mengembalikan semua detail (JOIN); booking tidak ada -> ValidationException")
    void readReturnsAllDetails() {
        Booking booking = service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12),
                qty(guitar.getInstrumentId(), 2, drum.getInstrumentId(), 1));

        List<BookingInstrument> details = service.getBookingInstruments(booking.getBookingId());
        assertEquals(2, details.size());
        assertTrue(details.stream().anyMatch(d -> d.getInstrument().getInstrumentId() == guitar.getInstrumentId()
                && d.getQuantity() == 2 && d.getInstrument().getName().equals(guitar.getName())));
        assertTrue(details.stream().anyMatch(d -> d.getInstrument().getInstrumentId() == drum.getInstrumentId()
                && d.getQuantity() == 1));
        assertThrows(ValidationException.class, () -> service.getBookingInstruments(999_999_999));
    }

    // ------------------------------------------------------------ UPDATE (AC-C03, AC-R16, BR-12)

    @Test
    @DisplayName("AC-C03/R16: updateBookingInstrumentQuantity mengubah quantity dan menghitung ulang total")
    void updateQuantityRecalculatesTotal() throws Exception {
        Booking booking = service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12),
                qty(guitar.getInstrumentId(), 1));
        assertEquals(bd("325000.00"), storedTotal(booking));
        BookingInstrument detail = service.getBookingInstruments(booking.getBookingId()).get(0);

        service.updateBookingInstrumentQuantity(detail.getBookingInstrumentId(), 4);
        assertEquals(4, service.getBookingInstruments(booking.getBookingId()).get(0).getQuantity());
        assertEquals(bd("400000.00"), storedTotal(booking), "300000 + 4 x 25000");

        service.updateBookingInstrumentQuantity(detail.getBookingInstrumentId(), 1);
        assertEquals(bd("325000.00"), storedTotal(booking));
        assertThrows(ValidationException.class, () -> service.updateBookingInstrumentQuantity(999_999_999, 2));
    }

    @Test
    @DisplayName("AC-R10: perubahan harga alat tidak mengubah snapshot; ubah quantity tetap memakai snapshot lama")
    void priceChangeDoesNotAffectSnapshot() throws Exception {
        Booking booking = service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12),
                qty(guitar.getInstrumentId(), 2));
        assertEquals(bd("350000.00"), storedTotal(booking));

        new InstrumentService().updateInstrument(new Instrument(guitar.getInstrumentId(), guitar.getName(),
                guitar.getType(), bd("99999.00"), InstrumentStatus.AVAILABLE));

        BookingInstrument detail = service.getBookingInstruments(booking.getBookingId()).get(0);
        assertEquals(bd("25000.00"), detail.getRentalPrice(), "snapshot lama utuh");
        assertEquals(bd("99999.00"), detail.getInstrument().getRentalPrice(), "harga instrument sekarang berubah");
        assertEquals(bd("350000.00"), storedTotal(booking), "total booking lama tidak berubah");

        service.updateBookingInstrumentQuantity(detail.getBookingInstrumentId(), 3);
        BookingInstrument after = service.getBookingInstruments(booking.getBookingId()).get(0);
        assertEquals(bd("25000.00"), after.getRentalPrice(), "ubah quantity tidak mengubah snapshot");
        assertEquals(bd("375000.00"), storedTotal(booking), "300000 + 3 x 25000 (bukan 99999)");

        // alat baru pada booking yang sama memakai harga SAAT INI sebagai snapshot barunya
        Booking other = service.createBooking(customer.getId(), room.getRoomId(), date.plusDays(1), t(10), t(12),
                qty(guitar.getInstrumentId(), 1));
        assertEquals(bd("99999.00"), service.getBookingInstruments(other.getBookingId()).get(0).getRentalPrice());
    }

    // ------------------------------------------------------------ DELETE (AC-C04)

    @Test
    @DisplayName("AC-C04/R16: removeInstrumentFromBooking menghapus baris, menghitung ulang total, booking tetap ada")
    void removeRecalculatesTotal() throws Exception {
        Booking booking = service.createBooking(customer.getId(), room.getRoomId(), date, t(10), t(12),
                qty(guitar.getInstrumentId(), 2, drum.getInstrumentId(), 1));
        assertEquals(bd("400000.00"), storedTotal(booking));
        BookingInstrument guitarDetail = service.getBookingInstruments(booking.getBookingId()).stream()
                .filter(d -> d.getInstrument().getInstrumentId() == guitar.getInstrumentId()).findFirst().orElseThrow();

        service.removeInstrumentFromBooking(guitarDetail.getBookingInstrumentId());
        assertEquals(1, detailCountForBooking(booking.getBookingId()));
        assertEquals(bd("350000.00"), storedTotal(booking), "300000 + 1 x 50000");

        BookingInstrument last = service.getBookingInstruments(booking.getBookingId()).get(0);
        service.removeInstrumentFromBooking(last.getBookingInstrumentId());
        assertEquals(0, detailCountForBooking(booking.getBookingId()));
        assertEquals(ROOM_PART, storedTotal(booking));
        assertEquals(BookingStatus.PENDING, bookingDAO.findById(booking.getBookingId()).orElseThrow().getStatus(),
                "booking tidak dihapus");

        assertThrows(ValidationException.class, () -> service.removeInstrumentFromBooking(999_999_999));
        assertThrows(ValidationException.class,
                () -> service.removeInstrumentFromBooking(last.getBookingInstrumentId()), "sudah terhapus");
    }

    // ------------------------------------------------------------ AC-R16 (BR-19)

    @Test
    @DisplayName("AC-R16: booking CANCELLED dan COMPLETED menolak tambah/ubah/hapus detail; data tidak berubah")
    void finalBookingsRejectDetailChanges() throws Exception {
        for (BookingStatus finalStatus : new BookingStatus[]{BookingStatus.CANCELLED, BookingStatus.COMPLETED}) {
            Booking booking = service.createBooking(customer.getId(), room.getRoomId(),
                    date.plusDays(finalStatus.ordinal()), t(10), t(12), qty(guitar.getInstrumentId(), 2));
            BigDecimal totalBefore = storedTotal(booking);
            BookingInstrument detail = service.getBookingInstruments(booking.getBookingId()).get(0);

            if (finalStatus == BookingStatus.COMPLETED) {
                service.updateBookingStatus(booking.getBookingId(), BookingStatus.CONFIRMED);
            }
            service.updateBookingStatus(booking.getBookingId(), finalStatus);

            String label = finalStatus.name();
            assertThrows(BusinessRuleException.class, () -> service.addInstrumentToBooking(
                    booking.getBookingId(), drum.getInstrumentId(), 1), label + " tambah");
            assertThrows(BusinessRuleException.class, () -> service.updateBookingInstrumentQuantity(
                    detail.getBookingInstrumentId(), 5), label + " ubah");
            assertThrows(BusinessRuleException.class, () -> service.removeInstrumentFromBooking(
                    detail.getBookingInstrumentId()), label + " hapus");

            List<BookingInstrument> after = service.getBookingInstruments(booking.getBookingId());
            assertEquals(1, after.size(), label);
            assertEquals(2, after.get(0).getQuantity(), label);
            assertEquals(totalBefore, storedTotal(booking), label);
        }
    }
}
