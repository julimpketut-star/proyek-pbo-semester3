package com.studiomusik.service;

import com.studiomusik.model.Customer;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.RoomStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.studiomusik.service.ServiceTestSupport.futureDate;
import static com.studiomusik.service.ServiceTestSupport.t;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tes integrasi RoomService (CRUD, validasi harga, MAINTENANCE tidak ditawarkan, BR-22). */
class RoomServiceTest {

    private final RoomService rooms = new RoomService();

    @BeforeAll
    static void requireDb() {
        ServiceTestSupport.requireDatabaseWithSeed();
        ServiceTestSupport.cleanUp();
    }

    @AfterEach
    void cleanUp() {
        ServiceTestSupport.cleanUp();
    }

    private static Room draft(String price, RoomStatus status) {
        return new Room(0, "  " + ServiceTestSupport.ROOM_PREFIX + ServiceTestSupport.uniqueSuffix() + "  ",
                "  Band Rehearsal ", new BigDecimal(price), status);
    }

    @Test
    @DisplayName("Create/Read: addRoom (trim, skala 2), getRoomById, getAllRooms, countRooms")
    void createAndRead() {
        long before = rooms.countRooms();
        Room created = rooms.addRoom(draft("123456.785", RoomStatus.AVAILABLE));

        assertTrue(created.getRoomId() > 0);
        assertFalse(created.getRoomName().startsWith(" "));
        assertEquals("Band Rehearsal", created.getRoomType());
        assertEquals(new BigDecimal("123456.79"), created.getPricePerHour(), "HALF_UP skala 2");

        Room read = rooms.getRoomById(created.getRoomId());
        assertEquals(created.getRoomName(), read.getRoomName());
        assertEquals(new BigDecimal("123456.79"), read.getPricePerHour());
        assertEquals(before + 1, rooms.countRooms());
        assertTrue(rooms.getAllRooms().stream().anyMatch(r -> r.getRoomId() == created.getRoomId()));
        assertThrows(ValidationException.class, () -> rooms.getRoomById(999_999_999));
    }

    @Test
    @DisplayName("Harga 0 diterima (pricePerHour >= 0)")
    void zeroPriceIsAccepted() {
        Room created = rooms.addRoom(draft("0", RoomStatus.AVAILABLE));
        assertEquals(new BigDecimal("0.00"), created.getPricePerHour());
    }

    @Test
    @DisplayName("AC-R03: getBookableRooms hanya AVAILABLE; getAllRooms memuat MAINTENANCE juga")
    void bookableRoomsExcludeMaintenance() {
        Room open = rooms.addRoom(draft("100000", RoomStatus.AVAILABLE));
        Room closed = rooms.addRoom(draft("100000", RoomStatus.MAINTENANCE));

        List<Room> bookable = rooms.getBookableRooms();
        assertTrue(bookable.stream().anyMatch(r -> r.getRoomId() == open.getRoomId()));
        assertTrue(bookable.stream().noneMatch(r -> r.getRoomId() == closed.getRoomId()));
        assertTrue(bookable.stream().allMatch(Room::isAvailable));
        assertTrue(rooms.getAllRooms().stream().anyMatch(r -> r.getRoomId() == closed.getRoomId()));
    }

    @Test
    @DisplayName("Update: nama/tipe/harga/status berubah; id tidak ada -> ValidationException")
    void update() {
        Room created = rooms.addRoom(draft("100000", RoomStatus.AVAILABLE));
        Room change = new Room(created.getRoomId(), created.getRoomName() + " v2", "Recording",
                new BigDecimal("175000.5"), RoomStatus.MAINTENANCE);

        Room updated = rooms.updateRoom(change);
        assertEquals(RoomStatus.MAINTENANCE, updated.getStatus());

        Room read = rooms.getRoomById(created.getRoomId());
        assertEquals("Recording", read.getRoomType());
        assertEquals(new BigDecimal("175000.50"), read.getPricePerHour());
        assertEquals(RoomStatus.MAINTENANCE, read.getStatus());
        assertTrue(rooms.getBookableRooms().stream().noneMatch(r -> r.getRoomId() == created.getRoomId()));

        assertThrows(ValidationException.class, () -> rooms.updateRoom(
                new Room(999_999_999, "Hantu", "Band", new BigDecimal("1"), RoomStatus.AVAILABLE)));
    }

    @Test
    @DisplayName("Delete: room yang belum dipakai terhapus; id tidak ada -> ValidationException")
    void deleteUnused() {
        Room created = rooms.addRoom(draft("100000", RoomStatus.AVAILABLE));
        rooms.deleteRoom(created.getRoomId());
        assertThrows(ValidationException.class, () -> rooms.getRoomById(created.getRoomId()));
        assertThrows(ValidationException.class, () -> rooms.deleteRoom(created.getRoomId()));
    }

    @Test
    @DisplayName("AC-R19: hapus Room yang dipakai Booking ditolak; pesan menyarankan MAINTENANCE; data tetap")
    void deleteUsedRoomIsRejected() throws Exception {
        Customer customer = ServiceTestSupport.newCustomer();
        Room room = ServiceTestSupport.newRoom("100000.00", RoomStatus.AVAILABLE);
        new BookingService().createBooking(customer.getId(), room.getRoomId(), futureDate(30), t(10), t(12), Map.of());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> rooms.deleteRoom(room.getRoomId()));
        assertTrue(ex.getMessage().contains("MAINTENANCE"), ex.getMessage());
        assertEquals(room.getRoomName(), rooms.getRoomById(room.getRoomId()).getRoomName());
    }

    @Test
    @DisplayName("AC-R19: Room seed yang dipakai booking (Studio A, id 1) tidak dapat dihapus")
    void seedRoomWithBookingCannotBeDeleted() {
        assertThrows(BusinessRuleException.class, () -> rooms.deleteRoom(1));
        assertEquals("Studio A", rooms.getRoomById(1).getRoomName());
    }
}
