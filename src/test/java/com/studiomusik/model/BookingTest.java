package com.studiomusik.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.studiomusik.model.enums.BookingStatus;

/** Booking: durasi (A-05), total biaya (BR-09..11), snapshot (BR-12), duplikat/quantity (BR-17/18). */
class BookingTest {

    private static final LocalDate DATE = LocalDate.of(2026, 12, 1);

    private static Booking booking(String roomPrice, int startHour, int endHour) {
        return new Booking(Fixtures.customer(), Fixtures.room(roomPrice), DATE,
                LocalTime.of(startHour, 0), LocalTime.of(endHour, 0));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(new BigDecimal(expected), actual);
    }

    // ---------- keadaan awal booking baru ----------

    @Test
    void newBookingStartsPendingWithZeroTotalAndNoDetails() {
        Booking b = booking("100000", 10, 12);
        assertEquals(0, b.getBookingId());
        assertEquals(BookingStatus.PENDING, b.getStatus());           // BR-13
        assertMoney("0.00", b.getTotalCost());
        assertTrue(b.getInstruments().isEmpty());
        assertNull(b.getCreatedAt());
    }

    // ---------- durasi ----------

    @Test
    void durationIsWholeHours() {
        assertEquals(2, booking("1", 10, 12).calculateDuration());
        assertEquals(1, booking("1", 9, 10).calculateDuration());
        assertEquals(12, booking("1", 8, 20).calculateDuration());
    }

    @Test
    void durationRejectsEndNotAfterStart() {
        assertThrows(IllegalStateException.class, () -> booking("1", 10, 10).calculateDuration()); // sama
        assertThrows(IllegalStateException.class, () -> booking("1", 12, 10).calculateDuration()); // terbalik
    }

    @Test
    void durationRejectsMissingOrNonWholeHourTimes() {
        Booking noTimes = new Booking(Fixtures.customer(), Fixtures.room("1"), DATE, null, null);
        assertThrows(IllegalStateException.class, noTimes::calculateDuration);

        Booking halfStart = new Booking(Fixtures.customer(), Fixtures.room("1"), DATE,
                LocalTime.of(10, 30), LocalTime.of(12, 0));
        Booking halfEnd = new Booking(Fixtures.customer(), Fixtures.room("1"), DATE,
                LocalTime.of(10, 0), LocalTime.of(11, 30));
        Booking withSeconds = new Booking(Fixtures.customer(), Fixtures.room("1"), DATE,
                LocalTime.of(10, 0, 30), LocalTime.of(12, 0));
        assertThrows(IllegalStateException.class, halfStart::calculateDuration);
        assertThrows(IllegalStateException.class, halfEnd::calculateDuration);
        assertThrows(IllegalStateException.class, withSeconds::calculateDuration);
    }

    // ---------- total biaya (AC-M03) ----------

    @Test
    void exampleFromContractRoom100kTwoHoursPlusTwoInstrumentsAt25kIs250k() {
        Booking b = booking("100000", 10, 12);
        Instrument guitar = Fixtures.instrument(2, "Gitar Elektrik", "25000");
        b.addInstrument(guitar, 2);                                  // 2 unit x 25.000
        assertMoney("250000.00", b.calculateTotalCost());
    }

    @Test
    void sameTotalWhenTwoDifferentInstrumentsAreUsedOnceEach() {
        Booking b = booking("100000", 10, 12);
        b.addInstrument(Fixtures.instrument(2, "Gitar", "25000"), 1);
        b.addInstrument(Fixtures.instrument(3, "Bass", "25000"), 1);
        assertMoney("250000.00", b.calculateTotalCost());
    }

    @Test
    void totalWithoutInstrumentsIsRoomCostOnly() {
        assertMoney("300000.00", booking("150000", 10, 12).calculateTotalCost());
    }

    @Test
    void totalIsBigDecimalWithScaleTwo() throws Exception {
        assertEquals(BigDecimal.class, Booking.class.getMethod("calculateTotalCost").getReturnType());
        assertEquals(2, booking("100000", 10, 12).calculateTotalCost().scale());
    }

    @Test
    void totalRoundsEachComponentHalfUpThenSums() {
        Booking b = new Booking(Fixtures.customer(), Fixtures.room("0.005"), DATE,
                LocalTime.of(10, 0), LocalTime.of(11, 0));          // ruangan 0.005 -> 0.01
        b.addInstrument(Fixtures.instrument(2, "Murah", "0.005"), 1); // alat 0.005 -> 0.01
        assertMoney("0.02", b.calculateTotalCost());
    }

    @Test
    void calculateTotalCostDoesNotMutateTotalCostField() {
        Booking b = booking("100000", 10, 12);
        b.calculateTotalCost();
        assertMoney("0.00", b.getTotalCost());
        b.setTotalCost(b.calculateTotalCost());                       // Service yang menyimpan
        assertMoney("200000.00", b.getTotalCost());
    }

    @Test
    void totalRequiresRoom() {
        Booking b = new Booking(Fixtures.customer(), null, DATE, LocalTime.of(10, 0), LocalTime.of(12, 0));
        assertThrows(IllegalStateException.class, b::calculateTotalCost);
    }

    @Test
    void reproducesSeedTotalsFromDatabaseContract() {
        // seed booking 1: Studio A 2 jam, Drum x1 @50000, Gitar x2 @25000 => 400000
        Booking b1 = booking("150000", 10, 12);
        b1.addInstrument(Fixtures.instrument(1, "Drum Set", "50000"), 1);
        b1.addInstrument(Fixtures.instrument(2, "Gitar", "25000"), 2);
        assertMoney("400000.00", b1.calculateTotalCost());

        // seed booking 5: Studio B 3 jam @120000 + 25000 + 25000 + 50000 + 10000 => 470000
        Booking b5 = booking("120000", 15, 18);
        b5.addInstrument(Fixtures.instrument(2, "Gitar", "25000"), 1);
        b5.addInstrument(Fixtures.instrument(3, "Bass", "25000"), 1);
        b5.addInstrument(Fixtures.instrument(1, "Drum Set", "50000"), 1);
        b5.addInstrument(Fixtures.instrument(5, "Mikrofon", "10000"), 1);
        assertMoney("470000.00", b5.calculateTotalCost());

        // seed booking 4 (snapshot lama 8000 vs harga sekarang 10000): 3 x 150000 + 2 x 8000 + 30000 => 496000
        Booking b4 = booking("150000", 10, 13);
        Instrument mic = Fixtures.instrument(5, "Mikrofon", "10000");
        Instrument amp = Fixtures.instrument(6, "Amplifier", "30000");
        b4.setInstruments(List.of(
                new BookingInstrument(5, 4, mic, 2, new BigDecimal("8000.00")),
                new BookingInstrument(6, 4, amp, 1, new BigDecimal("30000.00"))));
        assertMoney("496000.00", b4.calculateTotalCost());
    }

    // ---------- snapshot harga ----------

    @Test
    void addInstrumentStoresPriceSnapshotAndLaterPriceChangeDoesNotAffectTotal() {
        Booking b = booking("100000", 10, 12);
        Instrument guitar = Fixtures.instrument(2, "Gitar Elektrik", "25000");
        b.addInstrument(guitar, 2);

        guitar.setRentalPrice(new BigDecimal("99999"));              // harga alat berubah sesudahnya

        BookingInstrument detail = b.getInstruments().get(0);
        assertEquals(new BigDecimal("25000"), detail.getRentalPrice());
        assertMoney("250000.00", b.calculateTotalCost());
    }

    @Test
    void instrumentAddedLaterGetsTheNewCurrentPrice() {
        Booking b = booking("100000", 10, 12);
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        Instrument bass = Fixtures.instrument(3, "Bass", "20000");
        b.addInstrument(guitar, 1);
        bass.setRentalPrice(new BigDecimal("22000"));
        b.addInstrument(bass, 1);
        assertEquals(new BigDecimal("22000"), b.getInstruments().get(1).getRentalPrice());
    }

    // ---------- quantity, duplikat, maintenance (BR-06, BR-17, BR-18) ----------

    @Test
    void addInstrumentRejectsQuantityBelowOne() {
        Booking b = booking("100000", 10, 12);
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        assertThrows(IllegalArgumentException.class, () -> b.addInstrument(guitar, 0));
        assertThrows(IllegalArgumentException.class, () -> b.addInstrument(guitar, -1));
        assertTrue(b.getInstruments().isEmpty());
    }

    @Test
    void addInstrumentRejectsNullInstrument() {
        Booking b = booking("100000", 10, 12);
        assertThrows(IllegalArgumentException.class, () -> b.addInstrument(null, 1));
        assertTrue(b.getInstruments().isEmpty());
    }

    @Test
    void addInstrumentRejectsDuplicateInstrumentAndKeepsFirstDetailUntouched() {
        Booking b = booking("100000", 10, 12);
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        b.addInstrument(guitar, 2);
        assertThrows(IllegalArgumentException.class, () -> b.addInstrument(guitar, 1));
        // objek berbeda tetapi instrumentId sama juga dianggap duplikat
        assertThrows(IllegalArgumentException.class,
                () -> b.addInstrument(Fixtures.instrument(2, "Gitar (salinan)", "25000"), 1));
        assertEquals(1, b.getInstruments().size());
        assertEquals(2, b.getInstruments().get(0).getQuantity());
    }

    @Test
    void addInstrumentRejectsMaintenanceInstrument() {
        Booking b = booking("100000", 10, 12);
        Instrument broken = Fixtures.maintenanceInstrument(7);
        assertFalse(broken.isAvailable());
        assertThrows(IllegalArgumentException.class, () -> b.addInstrument(broken, 1));
        assertTrue(b.getInstruments().isEmpty());
    }

    @Test
    void maintenanceRoomIsReportedUnavailableByRoomButBookingModelDoesNotBlockIt() {
        // BR-05 ditegakkan Service; model hanya menyediakan Room.isAvailable()
        Room room = Fixtures.room("100000");
        room.setStatus(com.studiomusik.model.enums.RoomStatus.MAINTENANCE);
        Booking b = new Booking(Fixtures.customer(), room, DATE, LocalTime.of(10, 0), LocalTime.of(12, 0));
        assertFalse(b.getRoom().isAvailable());
    }

    // ---------- removeInstrument ----------

    @Test
    void removeInstrumentRemovesDetailAndReducesTotal() {
        Booking b = booking("100000", 10, 12);
        b.addInstrument(Fixtures.instrument(2, "Gitar", "25000"), 2);
        b.addInstrument(Fixtures.instrument(3, "Bass", "25000"), 1);
        assertMoney("275000.00", b.calculateTotalCost());

        b.removeInstrument(2);

        assertEquals(1, b.getInstruments().size());
        assertEquals(3, b.getInstruments().get(0).getInstrument().getInstrumentId());
        assertMoney("225000.00", b.calculateTotalCost());
    }

    @Test
    void removeInstrumentThatIsNotInBookingIsRejected() {
        Booking b = booking("100000", 10, 12);
        b.addInstrument(Fixtures.instrument(2, "Gitar", "25000"), 1);
        assertThrows(IllegalArgumentException.class, () -> b.removeInstrument(99));
        assertEquals(1, b.getInstruments().size());
    }

    @Test
    void instrumentRemovedCanBeAddedAgain() {
        Booking b = booking("100000", 10, 12);
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        b.addInstrument(guitar, 1);
        b.removeInstrument(2);
        b.addInstrument(guitar, 3);
        assertEquals(3, b.getInstruments().get(0).getQuantity());
    }

    // ---------- komposisi / enkapsulasi daftar detail ----------

    @Test
    void getInstrumentsIsAnUnmodifiableView() {
        Booking b = booking("100000", 10, 12);
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        assertThrows(UnsupportedOperationException.class,
                () -> b.getInstruments().add(new BookingInstrument(0, 0, guitar, 1, BigDecimal.TEN)));
        b.addInstrument(guitar, 1);
        assertThrows(UnsupportedOperationException.class, () -> b.getInstruments().clear());
    }

    @Test
    void setInstrumentsCopiesTheGivenListAndReplacesPreviousDetails() {
        Booking b = booking("100000", 10, 12);
        b.addInstrument(Fixtures.instrument(2, "Gitar", "25000"), 1);

        List<BookingInstrument> loaded = new ArrayList<>();
        loaded.add(new BookingInstrument(1, 0, Fixtures.instrument(5, "Mic", "10000"), 1, new BigDecimal("10000")));
        b.setInstruments(loaded);
        loaded.clear();                                               // mengubah list asal tidak boleh berefek

        assertEquals(1, b.getInstruments().size());
        assertEquals(5, b.getInstruments().get(0).getInstrument().getInstrumentId());
        assertThrows(NullPointerException.class, () -> b.setInstruments(null));
    }

    @Test
    void setBookingIdPropagatesToExistingAndFutureDetails() {
        Booking b = booking("100000", 10, 12);
        b.addInstrument(Fixtures.instrument(2, "Gitar", "25000"), 1);
        assertEquals(0, b.getInstruments().get(0).getBookingId());

        b.setBookingId(42);                                           // mis. setelah INSERT memberi id
        b.addInstrument(Fixtures.instrument(3, "Bass", "25000"), 1);

        assertEquals(42, b.getBookingId());
        assertEquals(42, b.getInstruments().get(0).getBookingId());
        assertEquals(42, b.getInstruments().get(1).getBookingId());
    }

    // ---------- asosiasi, status, setter ----------

    @Test
    void associationsKeepTheSameReferences() {
        Customer customer = Fixtures.customer();
        Room room = Fixtures.room("100000");
        Booking b = new Booking(customer, room, DATE, LocalTime.of(10, 0), LocalTime.of(12, 0));
        assertSame(customer, b.getCustomer());
        assertSame(room, b.getRoom());
    }

    @Test
    void statusCanBeChangedAndNullIsRejected() {
        Booking b = booking("100000", 10, 12);
        b.setStatus(BookingStatus.CONFIRMED);
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertThrows(NullPointerException.class, () -> b.setStatus(null));
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertTrue(b.getStatus().blocksSchedule());
    }

    @Test
    void fullConstructorKeepsAllValues() {
        java.time.LocalDateTime created = java.time.LocalDateTime.of(2026, 11, 20, 9, 0);
        Booking b = new Booking(1, Fixtures.customer(), Fixtures.room("150000"), DATE,
                LocalTime.of(10, 0), LocalTime.of(12, 0), BookingStatus.CONFIRMED,
                new BigDecimal("400000.00"), created);
        assertEquals(1, b.getBookingId());
        assertEquals(DATE, b.getBookingDate());
        assertEquals(LocalTime.of(10, 0), b.getStartTime());
        assertEquals(LocalTime.of(12, 0), b.getEndTime());
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertMoney("400000.00", b.getTotalCost());
        assertEquals(created, b.getCreatedAt());
        assertThrows(NullPointerException.class, () -> new Booking(1, null, null, DATE, null, null,
                null, BigDecimal.ZERO, null));
    }
}
