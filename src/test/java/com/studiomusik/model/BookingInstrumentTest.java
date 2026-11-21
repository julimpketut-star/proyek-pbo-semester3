package com.studiomusik.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** BookingInstrument: snapshot harga (BR-12), quantity >= 1 (BR-18). */
class BookingInstrumentTest {

    @Test
    void subtotalUsesSnapshotPriceNotCurrentInstrumentPrice() {
        Instrument guitar = Fixtures.instrument(2, "Gitar Elektrik", "25000");
        BookingInstrument detail = new BookingInstrument(0, 1, guitar, 2, guitar.getRentalPrice());

        guitar.setRentalPrice(new BigDecimal("30000")); // harga alat naik setelah booking dibuat

        assertEquals(new BigDecimal("50000.00"), detail.calculateSubtotal());
        assertEquals(new BigDecimal("25000"), detail.getRentalPrice());
        assertEquals(new BigDecimal("30000"), guitar.getRentalPrice());
    }

    @Test
    void snapshotCanDifferFromCurrentPriceWhenLoadedFromDatabase() {
        // Meniru seed: booking_instrument 5 menyimpan 8000, harga instrument sekarang 10000
        Instrument mic = Fixtures.instrument(5, "Mikrofon", "10000");
        BookingInstrument detail = new BookingInstrument(5, 4, mic, 2, new BigDecimal("8000.00"));
        assertEquals(new BigDecimal("16000.00"), detail.calculateSubtotal());
    }

    @Test
    void rentalPriceHasNoSetterAndIsFinal() throws Exception {
        assertThrows(NoSuchMethodException.class,
                () -> BookingInstrument.class.getMethod("setRentalPrice", BigDecimal.class));
        assertTrue(Modifier.isFinal(BookingInstrument.class.getDeclaredField("rentalPrice").getModifiers()));
        assertTrue(Modifier.isFinal(BookingInstrument.class.getDeclaredField("instrument").getModifiers()));
    }

    @Test
    void quantityBelowOneIsRejectedInConstructor() {
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        assertThrows(IllegalArgumentException.class, () -> new BookingInstrument(0, 1, guitar, 0, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> new BookingInstrument(0, 1, guitar, -3, BigDecimal.TEN));
    }

    @Test
    void setQuantityValidatesAndKeepsOldValueOnRejection() {
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        BookingInstrument detail = new BookingInstrument(0, 1, guitar, 2, guitar.getRentalPrice());

        assertThrows(IllegalArgumentException.class, () -> detail.setQuantity(0));
        assertEquals(2, detail.getQuantity());

        detail.setQuantity(3);
        assertEquals(3, detail.getQuantity());
        assertEquals(new BigDecimal("75000.00"), detail.calculateSubtotal());
    }

    @Test
    void nullInstrumentNullOrNegativePriceAreRejected() {
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        assertThrows(NullPointerException.class, () -> new BookingInstrument(0, 1, null, 1, BigDecimal.TEN));
        assertThrows(NullPointerException.class, () -> new BookingInstrument(0, 1, guitar, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> new BookingInstrument(0, 1, guitar, 1, new BigDecimal("-1")));
    }

    @Test
    void subtotalIsBigDecimalWithScaleTwoAndHalfUp() throws Exception {
        assertEquals(BigDecimal.class, BookingInstrument.class.getMethod("calculateSubtotal").getReturnType());
        Instrument item = Fixtures.instrument(1, "A", "1.005");
        BookingInstrument detail = new BookingInstrument(0, 1, item, 1, new BigDecimal("1.005"));
        assertEquals(new BigDecimal("1.01"), detail.calculateSubtotal());
    }

    @Test
    void idsAndInstrumentReferenceAreExposed() {
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        BookingInstrument detail = new BookingInstrument(11, 5, guitar, 1, guitar.getRentalPrice());
        assertEquals(11, detail.getBookingInstrumentId());
        assertEquals(5, detail.getBookingId());
        assertSame(guitar, detail.getInstrument());
        detail.setBookingInstrumentId(12);
        detail.setBookingId(6);
        assertEquals(12, detail.getBookingInstrumentId());
        assertEquals(6, detail.getBookingId());
    }
}
