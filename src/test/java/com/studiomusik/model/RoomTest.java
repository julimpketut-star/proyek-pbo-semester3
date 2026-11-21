package com.studiomusik.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.studiomusik.model.enums.RoomStatus;

/** Room: ketersediaan (BR-05), biaya BigDecimal HALF_UP skala 2 (BR-09). */
class RoomTest {

    private static void assertMoney(String expected, BigDecimal actual) {
        // BigDecimal.equals peka terhadap skala, sehingga ini juga memastikan skala = 2
        assertEquals(new BigDecimal(expected), actual);
    }

    @Test
    void availableRoomIsAvailableAndMaintenanceRoomIsNot() {
        Room room = Fixtures.room("100000");
        assertTrue(room.isAvailable());
        room.setStatus(RoomStatus.MAINTENANCE);
        assertFalse(room.isAvailable());
        room.setStatus(RoomStatus.AVAILABLE);
        assertTrue(room.isAvailable());
    }

    @Test
    void maintenanceRoomCreatedDirectlyIsNotAvailable() {
        Room room = new Room(5, "Studio D", "Band Rehearsal", new BigDecimal("100000.00"), RoomStatus.MAINTENANCE);
        assertFalse(room.isAvailable());
    }

    @Test
    void costIsPricePerHourTimesHoursWithScaleTwo() {
        assertMoney("200000.00", Fixtures.room("100000").calculateCost(2));
        assertMoney("450000.00", Fixtures.room("150000").calculateCost(3));
        assertMoney("0.00", Fixtures.room("150000").calculateCost(0));
    }

    @Test
    void costUsesHalfUpRoundingOnExactDecimals() {
        assertMoney("0.01", Fixtures.room("0.005").calculateCost(1));   // tepat di tengah -> naik
        assertMoney("0.00", Fixtures.room("0.004").calculateCost(1));   // di bawah tengah -> turun
        assertMoney("1.01", Fixtures.room("1.005").calculateCost(1));   // double akan salah di sini; BigDecimal benar
        assertMoney("100.00", Fixtures.room("33.333").calculateCost(3)); // 99.999 -> 100.00
        assertMoney("0.03", Fixtures.room("0.015").calculateCost(2));    // 0.030
    }

    @Test
    void priceFieldAndCostAreBigDecimalNotDouble() throws Exception {
        assertEquals(BigDecimal.class, Room.class.getMethod("getPricePerHour").getReturnType());
        assertEquals(BigDecimal.class, Room.class.getMethod("calculateCost", int.class).getReturnType());
    }

    @Test
    void negativeHoursAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Fixtures.room("100000").calculateCost(-1));
    }

    @Test
    void invalidConstructorAndSetterValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Room(1, "A", "B", new BigDecimal("-1"), RoomStatus.AVAILABLE));
        assertThrows(NullPointerException.class, () -> new Room(1, "A", "B", null, RoomStatus.AVAILABLE));
        assertThrows(NullPointerException.class, () -> new Room(1, "A", "B", BigDecimal.TEN, null));

        Room room = Fixtures.room("100000");
        assertThrows(IllegalArgumentException.class, () -> room.setPricePerHour(new BigDecimal("-0.01")));
        assertThrows(NullPointerException.class, () -> room.setPricePerHour(null));
        assertThrows(NullPointerException.class, () -> room.setStatus(null));
        assertMoney("200000.00", room.calculateCost(2)); // state tidak berubah setelah penolakan
    }

    @Test
    void gettersAndSettersRoundTrip() {
        Room room = Fixtures.room("100000");
        room.setRoomId(7);
        room.setRoomName("Studio X");
        room.setRoomType("Recording");
        room.setPricePerHour(new BigDecimal("250000.00"));
        assertEquals(7, room.getRoomId());
        assertEquals("Studio X", room.getRoomName());
        assertEquals("Recording", room.getRoomType());
        assertEquals(new BigDecimal("250000.00"), room.getPricePerHour());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }
}
