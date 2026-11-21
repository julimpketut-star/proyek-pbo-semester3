package com.studiomusik.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.studiomusik.model.enums.InstrumentStatus;

/** Instrument: ketersediaan (BR-06), biaya BigDecimal HALF_UP skala 2 (BR-10). */
class InstrumentTest {

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(new BigDecimal(expected), actual);
    }

    @Test
    void availableAndMaintenanceInstruments() {
        Instrument guitar = Fixtures.instrument(2, "Gitar Elektrik", "25000");
        assertTrue(guitar.isAvailable());
        assertFalse(Fixtures.maintenanceInstrument(7).isAvailable());
        guitar.setStatus(InstrumentStatus.MAINTENANCE);
        assertFalse(guitar.isAvailable());
    }

    @Test
    void costIsRentalPriceTimesQuantity() {
        Instrument guitar = Fixtures.instrument(2, "Gitar Elektrik", "25000");
        assertMoney("50000.00", guitar.calculateCost(2));
        assertMoney("25000.00", guitar.calculateCost(1));
        assertMoney("0.00", guitar.calculateCost(0));
    }

    @Test
    void costUsesHalfUpRounding() {
        assertMoney("0.01", Fixtures.instrument(1, "A", "0.005").calculateCost(1));
        assertMoney("0.00", Fixtures.instrument(1, "A", "0.004").calculateCost(1));
        assertMoney("1.01", Fixtures.instrument(1, "A", "1.005").calculateCost(1));
        assertMoney("10.00", Fixtures.instrument(1, "A", "3.3333").calculateCost(3)); // 9.9999 -> 10.00
    }

    @Test
    void priceAndCostAreBigDecimalNotDouble() throws Exception {
        assertEquals(BigDecimal.class, Instrument.class.getMethod("getRentalPrice").getReturnType());
        assertEquals(BigDecimal.class, Instrument.class.getMethod("calculateCost", int.class).getReturnType());
    }

    @Test
    void negativeQuantityIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Fixtures.instrument(2, "Gitar", "25000").calculateCost(-1));
    }

    @Test
    void invalidConstructorAndSetterValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Instrument(1, "A", "B", new BigDecimal("-1"), InstrumentStatus.AVAILABLE));
        assertThrows(NullPointerException.class, () -> new Instrument(1, "A", "B", null, InstrumentStatus.AVAILABLE));
        assertThrows(NullPointerException.class, () -> new Instrument(1, "A", "B", BigDecimal.TEN, null));
        Instrument guitar = Fixtures.instrument(2, "Gitar", "25000");
        assertThrows(IllegalArgumentException.class, () -> guitar.setRentalPrice(new BigDecimal("-5")));
        assertThrows(NullPointerException.class, () -> guitar.setStatus(null));
    }

    @Test
    void gettersAndSettersRoundTrip() {
        Instrument item = Fixtures.instrument(2, "Gitar", "25000");
        item.setInstrumentId(8);
        item.setName("Gitar Baru");
        item.setType("Gitar");
        item.setRentalPrice(new BigDecimal("30000.00"));
        assertEquals(8, item.getInstrumentId());
        assertEquals("Gitar Baru", item.getName());
        assertEquals("Gitar", item.getType());
        assertEquals(new BigDecimal("30000.00"), item.getRentalPrice());
    }
}
