package com.studiomusik.service;

import com.studiomusik.model.Booking;
import com.studiomusik.model.Customer;
import com.studiomusik.model.Instrument;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.InstrumentStatus;
import com.studiomusik.model.enums.RoomStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.studiomusik.service.ServiceTestSupport.futureDate;
import static com.studiomusik.service.ServiceTestSupport.qty;
import static com.studiomusik.service.ServiceTestSupport.t;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tes integrasi InstrumentService (CRUD, validasi harga, MAINTENANCE tidak dipilih, BR-22, snapshot BR-12). */
class InstrumentServiceTest {

    private final InstrumentService instruments = new InstrumentService();

    @BeforeAll
    static void requireDb() {
        ServiceTestSupport.requireDatabaseWithSeed();
        ServiceTestSupport.cleanUp();
    }

    @AfterEach
    void cleanUp() {
        ServiceTestSupport.cleanUp();
    }

    private static Instrument draft(String price, InstrumentStatus status) {
        return new Instrument(0, " " + ServiceTestSupport.INSTRUMENT_PREFIX + ServiceTestSupport.uniqueSuffix() + " ",
                " Gitar ", new BigDecimal(price), status);
    }

    @Test
    @DisplayName("Create/Read: addInstrument (trim, skala 2), getInstrumentById, getAllInstruments, countInstruments")
    void createAndRead() {
        long before = instruments.countInstruments();
        Instrument created = instruments.addInstrument(draft("12345.675", InstrumentStatus.AVAILABLE));

        assertTrue(created.getInstrumentId() > 0);
        assertEquals("Gitar", created.getType());
        assertEquals(new BigDecimal("12345.68"), created.getRentalPrice(), "HALF_UP skala 2");

        Instrument read = instruments.getInstrumentById(created.getInstrumentId());
        assertEquals(created.getName(), read.getName());
        assertEquals(new BigDecimal("12345.68"), read.getRentalPrice());
        assertEquals(before + 1, instruments.countInstruments());
        assertTrue(instruments.getAllInstruments().stream()
                .anyMatch(i -> i.getInstrumentId() == created.getInstrumentId()));
        assertThrows(ValidationException.class, () -> instruments.getInstrumentById(999_999_999));
    }

    @Test
    @DisplayName("AC-R04: getSelectableInstruments hanya AVAILABLE; getAllInstruments memuat MAINTENANCE juga")
    void selectableExcludesMaintenance() {
        Instrument open = instruments.addInstrument(draft("10000", InstrumentStatus.AVAILABLE));
        Instrument closed = instruments.addInstrument(draft("10000", InstrumentStatus.MAINTENANCE));

        List<Instrument> selectable = instruments.getSelectableInstruments();
        assertTrue(selectable.stream().anyMatch(i -> i.getInstrumentId() == open.getInstrumentId()));
        assertTrue(selectable.stream().noneMatch(i -> i.getInstrumentId() == closed.getInstrumentId()));
        assertTrue(selectable.stream().allMatch(Instrument::isAvailable));
        assertTrue(instruments.getAllInstruments().stream()
                .anyMatch(i -> i.getInstrumentId() == closed.getInstrumentId()));
    }

    @Test
    @DisplayName("Update: nama/tipe/harga/status berubah; id tidak ada -> ValidationException")
    void update() {
        Instrument created = instruments.addInstrument(draft("10000", InstrumentStatus.AVAILABLE));
        Instrument change = new Instrument(created.getInstrumentId(), created.getName() + " v2", "Bass",
                new BigDecimal("22500"), InstrumentStatus.MAINTENANCE);

        instruments.updateInstrument(change);
        Instrument read = instruments.getInstrumentById(created.getInstrumentId());
        assertEquals("Bass", read.getType());
        assertEquals(new BigDecimal("22500.00"), read.getRentalPrice());
        assertEquals(InstrumentStatus.MAINTENANCE, read.getStatus());

        assertThrows(ValidationException.class, () -> instruments.updateInstrument(
                new Instrument(999_999_999, "Hantu", "Gitar", BigDecimal.ONE, InstrumentStatus.AVAILABLE)));
    }

    @Test
    @DisplayName("Delete: alat yang belum dipakai terhapus; id tidak ada -> ValidationException")
    void deleteUnused() {
        Instrument created = instruments.addInstrument(draft("10000", InstrumentStatus.AVAILABLE));
        instruments.deleteInstrument(created.getInstrumentId());
        assertThrows(ValidationException.class, () -> instruments.getInstrumentById(created.getInstrumentId()));
        assertThrows(ValidationException.class, () -> instruments.deleteInstrument(created.getInstrumentId()));
    }

    @Test
    @DisplayName("AC-R19: hapus Instrument yang dipakai Booking ditolak; pesan menyarankan MAINTENANCE; data tetap")
    void deleteUsedInstrumentIsRejected() throws Exception {
        Customer customer = ServiceTestSupport.newCustomer();
        Room room = ServiceTestSupport.newRoom("100000.00", RoomStatus.AVAILABLE);
        Instrument instrument = ServiceTestSupport.newInstrument("25000.00", InstrumentStatus.AVAILABLE);
        Booking booking = new BookingService().createBooking(customer.getId(), room.getRoomId(), futureDate(30),
                t(10), t(12), qty(instrument.getInstrumentId(), 1));
        assertEquals(1, booking.getInstruments().size());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> instruments.deleteInstrument(instrument.getInstrumentId()));
        assertTrue(ex.getMessage().contains("MAINTENANCE"), ex.getMessage());
        assertEquals(instrument.getName(), instruments.getInstrumentById(instrument.getInstrumentId()).getName());
    }

    @Test
    @DisplayName("AC-R19: Instrument seed yang dipakai booking (Drum Set, id 1) tidak dapat dihapus")
    void seedInstrumentWithBookingCannotBeDeleted() {
        assertThrows(BusinessRuleException.class, () -> instruments.deleteInstrument(1));
        assertEquals("Drum Set Pearl", instruments.getInstrumentById(1).getName());
    }
}
