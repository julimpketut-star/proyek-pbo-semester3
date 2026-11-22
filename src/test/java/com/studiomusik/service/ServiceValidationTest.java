package com.studiomusik.service;

import com.studiomusik.model.Instrument;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.InstrumentStatus;
import com.studiomusik.model.enums.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tes MURNI (tanpa database; selalu berjalan): seluruh validasi input dilakukan sebelum DAO/koneksi
 * disentuh. Mencakup AC-R01 (end &lt;= start), AC-R02 (menit bukan 00), AC-R18 (tanggal lampau),
 * bagian validasi AC-R15 (quantity &lt; 1) dan validasi field semua Service.
 */
class ServiceValidationTest {

    private static final LocalDate FUTURE = LocalDate.now().plusDays(30);
    private final BookingService booking = new BookingService();

    private static LocalTime t(int h) {
        return LocalTime.of(h, 0);
    }

    // ------------------------------------------------------------ hierarki exception

    @Test
    @DisplayName("Hierarki: Validation/BusinessRule adalah ServiceException (unchecked)")
    void exceptionHierarchy() {
        assertTrue(RuntimeException.class.isAssignableFrom(ServiceException.class));
        assertTrue(ServiceException.class.isAssignableFrom(ValidationException.class));
        assertTrue(ServiceException.class.isAssignableFrom(BusinessRuleException.class));
        Throwable cause = new IllegalStateException("x");
        assertEquals(cause, new ServiceException("m", cause).getCause());
    }

    // ------------------------------------------------------------ AC-R01 (BR-03)

    @Test
    @DisplayName("AC-R01: endTime <= startTime ditolak (isRoomAvailable, createBooking, estimate)")
    void endNotAfterStartIsRejected() {
        assertThrows(ValidationException.class, () -> booking.isRoomAvailable(1, FUTURE, t(12), t(12)));
        assertThrows(ValidationException.class, () -> booking.isRoomAvailable(1, FUTURE, t(12), t(10)));
        assertThrows(ValidationException.class,
                () -> booking.createBooking(1, 1, FUTURE, t(12), t(12), Map.of()));
        assertThrows(ValidationException.class,
                () -> booking.createBooking(1, 1, FUTURE, t(14), t(9), Map.of()));
        assertThrows(ValidationException.class, () -> booking.estimateTotalCost(1, t(12), t(11), Map.of()));
    }

    @Test
    @DisplayName("Waktu/tanggal null ditolak")
    void nullScheduleIsRejected() {
        assertThrows(ValidationException.class, () -> booking.isRoomAvailable(1, null, t(10), t(12)));
        assertThrows(ValidationException.class, () -> booking.isRoomAvailable(1, FUTURE, null, t(12)));
        assertThrows(ValidationException.class, () -> booking.isRoomAvailable(1, FUTURE, t(10), null));
        assertThrows(ValidationException.class, () -> booking.estimateTotalCost(1, null, null, null));
    }

    // ------------------------------------------------------------ AC-R02 (BR-04)

    @Test
    @DisplayName("AC-R02: waktu bukan menit 00 ditolak (mulai, selesai, detik)")
    void nonWholeHourIsRejected() {
        assertThrows(ValidationException.class,
                () -> booking.isRoomAvailable(1, FUTURE, LocalTime.of(10, 30), t(12)));
        assertThrows(ValidationException.class,
                () -> booking.isRoomAvailable(1, FUTURE, t(10), LocalTime.of(12, 15)));
        assertThrows(ValidationException.class,
                () -> booking.createBooking(1, 1, FUTURE, LocalTime.of(10, 30), LocalTime.of(11, 30), Map.of()));
        assertThrows(ValidationException.class,
                () -> booking.isRoomAvailable(1, FUTURE, t(10), LocalTime.of(12, 0, 30)));
        assertThrows(ValidationException.class,
                () -> booking.estimateTotalCost(1, LocalTime.of(9, 45), t(11), Map.of()));
    }

    // ------------------------------------------------------------ AC-R18 (BR-21)

    @Test
    @DisplayName("AC-R18: tanggal lampau ditolak")
    void pastDateIsRejected() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertThrows(ValidationException.class, () -> booking.isRoomAvailable(1, yesterday, t(10), t(12)));
        assertThrows(ValidationException.class,
                () -> booking.createBooking(1, 1, yesterday, t(10), t(12), Map.of()));
        assertThrows(ValidationException.class,
                () -> booking.createBooking(1, 1, LocalDate.of(2020, 1, 1), t(10), t(12), Map.of()));
    }

    // ------------------------------------------------------------ AC-R15 (BR-18)

    @Test
    @DisplayName("AC-R15: quantity < 1 atau id alat tidak valid ditolak")
    void invalidQuantityIsRejected() {
        Map<Integer, Integer> zero = Map.of(1, 0);
        Map<Integer, Integer> negative = Map.of(1, -2);
        Map<Integer, Integer> nullQty = new HashMap<>();
        nullQty.put(1, null);
        Map<Integer, Integer> badId = Map.of(0, 1);

        assertThrows(ValidationException.class, () -> booking.createBooking(1, 1, FUTURE, t(10), t(12), zero));
        assertThrows(ValidationException.class, () -> booking.createBooking(1, 1, FUTURE, t(10), t(12), negative));
        assertThrows(ValidationException.class, () -> booking.createBooking(1, 1, FUTURE, t(10), t(12), nullQty));
        assertThrows(ValidationException.class, () -> booking.createBooking(1, 1, FUTURE, t(10), t(12), badId));
        assertThrows(ValidationException.class, () -> booking.estimateTotalCost(1, t(10), t(12), zero));
        assertThrows(ValidationException.class, () -> booking.addInstrumentToBooking(1, 1, 0));
        assertThrows(ValidationException.class, () -> booking.addInstrumentToBooking(1, 1, -1));
        assertThrows(ValidationException.class, () -> booking.updateBookingInstrumentQuantity(1, 0));
    }

    @Test
    @DisplayName("Status baru null ditolak")
    void nullStatusIsRejected() {
        assertThrows(ValidationException.class, () -> booking.updateBookingStatus(1, null));
    }

    // ------------------------------------------------------------ Auth / Customer

    @Test
    @DisplayName("AuthService: login dengan input kosong -> ValidationException")
    void blankLoginIsRejected() {
        AuthService auth = new AuthService();
        assertThrows(ValidationException.class, () -> auth.loginAdmin(null, "x"));
        assertThrows(ValidationException.class, () -> auth.loginAdmin("  ", "x"));
        assertThrows(ValidationException.class, () -> auth.loginAdmin("admin", ""));
        assertThrows(ValidationException.class, () -> auth.loginCustomer(null, "x"));
        assertThrows(ValidationException.class, () -> auth.loginCustomer("a@b.co", null));
    }

    @Test
    @DisplayName("AuthService/CustomerService: validasi nama, telepon, email, password")
    void customerFieldValidation() {
        AuthService auth = new AuthService();
        CustomerService customers = new CustomerService();
        String ok = "Password123";

        assertThrows(ValidationException.class, () -> auth.registerCustomer("", "081234567890", "a@b.co", ok));
        assertThrows(ValidationException.class, () -> auth.registerCustomer(null, "081234567890", "a@b.co", ok));
        assertThrows(ValidationException.class, () -> auth.registerCustomer("A", "", "a@b.co", ok));
        assertThrows(ValidationException.class, () -> auth.registerCustomer("A", "abc123", "a@b.co", ok));
        assertThrows(ValidationException.class, () -> auth.registerCustomer("A", "123", "a@b.co", ok));
        assertThrows(ValidationException.class, () -> auth.registerCustomer("A", "081234567890", "", ok));
        assertThrows(ValidationException.class, () -> auth.registerCustomer("A", "081234567890", "bukan-email", ok));
        assertThrows(ValidationException.class, () -> auth.registerCustomer("A", "081234567890", "a@b", ok));
        assertThrows(ValidationException.class, () -> auth.registerCustomer("A", "081234567890", "a b@c.co", ok));
        assertThrows(ValidationException.class, () -> auth.registerCustomer("A", "081234567890", "a@b.co", ""));
        assertThrows(ValidationException.class, () -> auth.registerCustomer("A", "081234567890", "a@b.co", "short"));
        assertThrows(ValidationException.class,
                () -> auth.registerCustomer("x".repeat(101), "081234567890", "a@b.co", ok));

        assertThrows(ValidationException.class, () -> customers.addCustomer("", "081234567890", "a@b.co", ok));
        assertThrows(ValidationException.class, () -> customers.updateCustomer(1, " ", "081234567890", "a@b.co"));
        assertThrows(ValidationException.class, () -> customers.updateCustomer(1, "A", "xx", "a@b.co"));
        assertThrows(ValidationException.class, () -> customers.updateCustomer(1, "A", "081234567890", "salah"));
    }

    // ------------------------------------------------------------ Room / Instrument

    @Test
    @DisplayName("RoomService: nama/tipe kosong, harga null/negatif/terlalu besar, objek null")
    void roomFieldValidation() {
        RoomService rooms = new RoomService();
        BigDecimal price = new BigDecimal("100000.00");
        assertThrows(ValidationException.class, () -> rooms.addRoom(null));
        assertThrows(ValidationException.class, () -> rooms.addRoom(new Room(0, "", "Band", price, RoomStatus.AVAILABLE)));
        assertThrows(ValidationException.class, () -> rooms.addRoom(new Room(0, "  ", "Band", price, RoomStatus.AVAILABLE)));
        assertThrows(ValidationException.class, () -> rooms.addRoom(new Room(0, "A", "", price, RoomStatus.AVAILABLE)));
        assertThrows(ValidationException.class, () -> rooms.addRoom(new Room(0, null, "Band", price, RoomStatus.AVAILABLE)));
        assertThrows(ValidationException.class,
                () -> rooms.addRoom(new Room(0, "x".repeat(51), "Band", price, RoomStatus.AVAILABLE)));
        assertThrows(ValidationException.class,
                () -> rooms.addRoom(new Room(0, "A", "Band", new BigDecimal("100000000.00"), RoomStatus.AVAILABLE)));
        assertThrows(ValidationException.class, () -> rooms.updateRoom(null));
        assertThrows(ValidationException.class, () -> rooms.updateRoom(new Room(1, "", "Band", price, RoomStatus.AVAILABLE)));
    }

    @Test
    @DisplayName("InstrumentService: nama/tipe kosong, harga negatif/terlalu besar, objek null")
    void instrumentFieldValidation() {
        InstrumentService instruments = new InstrumentService();
        BigDecimal price = new BigDecimal("25000.00");
        assertThrows(ValidationException.class, () -> instruments.addInstrument(null));
        assertThrows(ValidationException.class,
                () -> instruments.addInstrument(new Instrument(0, "", "Gitar", price, InstrumentStatus.AVAILABLE)));
        assertThrows(ValidationException.class,
                () -> instruments.addInstrument(new Instrument(0, "Gitar", " ", price, InstrumentStatus.AVAILABLE)));
        assertThrows(ValidationException.class,
                () -> instruments.addInstrument(new Instrument(0, "Gitar", "Gitar", new BigDecimal("1000000000"),
                        InstrumentStatus.AVAILABLE)));
        assertThrows(ValidationException.class, () -> instruments.updateInstrument(null));
        assertThrows(ValidationException.class,
                () -> instruments.updateInstrument(new Instrument(1, null, "Gitar", price, InstrumentStatus.AVAILABLE)));
    }
}
