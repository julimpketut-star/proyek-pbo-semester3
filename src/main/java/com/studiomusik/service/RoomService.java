package com.studiomusik.service;

import com.studiomusik.dao.RoomDAO;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.RoomStatus;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * CRUD Room (API_CONTRACTS §4.3). Validasi nama/tipe tidak kosong dan {@code pricePerHour >= 0}
 * (skala 2, HALF_UP). Room {@code MAINTENANCE} tidak ditawarkan untuk dipesan
 * ({@link #getBookableRooms()}; penegakan akhir di {@code BookingService}, BR-05). Room yang sudah
 * dipakai Booking tidak dapat dihapus (BR-22) dan pesan menyarankan status MAINTENANCE.
 */
public class RoomService {

    private final RoomDAO roomDAO;

    public RoomService() {
        this(new RoomDAO());
    }

    public RoomService(RoomDAO roomDAO) {
        this.roomDAO = roomDAO;
    }

    /** Semua room (Customer & Admin, untuk info harga), termasuk yang MAINTENANCE. */
    public List<Room> getAllRooms() {
        try {
            return roomDAO.findAll();
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca daftar ruangan", e);
        }
    }

    /** Hanya room berstatus AVAILABLE (dapat dipesan). */
    public List<Room> getBookableRooms() {
        try {
            return roomDAO.findByStatus(RoomStatus.AVAILABLE);
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca daftar ruangan tersedia", e);
        }
    }

    /** @throws ValidationException bila room tidak ditemukan */
    public Room getRoomById(int roomId) {
        try {
            return roomDAO.findById(roomId).orElseThrow(RoomService::notFound);
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca ruangan", e);
        }
    }

    /**
     * @return room tersimpan (objek baru dengan id; argumen tidak diubah)
     * @throws ValidationException bila nama/tipe kosong atau harga tidak valid
     */
    public Room addRoom(Room room) {
        Room candidate = validated(room, 0);
        try {
            int id = roomDAO.insert(candidate);
            candidate.setRoomId(id);
            return candidate;
        } catch (SQLException e) {
            throw ServiceSupport.dbError("menyimpan ruangan", e);
        }
    }

    /**
     * @return room hasil pembaruan (objek baru)
     * @throws ValidationException bila input tidak valid atau room tidak ditemukan
     */
    public Room updateRoom(Room room) {
        if (room == null) {
            throw new ValidationException("Data ruangan wajib diisi.");
        }
        Room candidate = validated(room, room.getRoomId());
        try {
            roomDAO.findById(candidate.getRoomId()).orElseThrow(RoomService::notFound);
            if (!roomDAO.update(candidate)) {
                throw notFound();
            }
            return candidate;
        } catch (SQLException e) {
            throw ServiceSupport.dbError("mengubah ruangan", e);
        }
    }

    /**
     * @throws BusinessRuleException bila room sudah dipakai Booking (BR-22; FK RESTRICT)
     * @throws ValidationException   bila room tidak ditemukan
     */
    public void deleteRoom(int roomId) {
        try {
            if (!roomDAO.delete(roomId)) {
                throw notFound();
            }
        } catch (SQLException e) {
            if (ServiceSupport.isReferencedByChild(e)) {
                throw new BusinessRuleException("Ruangan tidak dapat dihapus karena sudah dipakai pada booking. "
                        + "Ubah statusnya menjadi MAINTENANCE bila tidak ingin dipesan lagi.", e);
            }
            throw ServiceSupport.dbError("menghapus ruangan", e);
        }
    }

    public long countRooms() {
        try {
            return roomDAO.count();
        } catch (SQLException e) {
            throw ServiceSupport.dbError("menghitung ruangan", e);
        }
    }

    private static Room validated(Room room, int roomId) {
        if (room == null) {
            throw new ValidationException("Data ruangan wajib diisi.");
        }
        String name = ServiceSupport.requireText(room.getRoomName(), "Nama ruangan", ServiceSupport.MAX_ASSET_NAME);
        String type = ServiceSupport.requireText(room.getRoomType(), "Tipe ruangan", ServiceSupport.MAX_ASSET_TYPE);
        BigDecimal price = ServiceSupport.requireMoney(room.getPricePerHour(), "Harga per jam");
        return new Room(roomId, name, type, price, room.getStatus());
    }

    private static ValidationException notFound() {
        return new ValidationException("Ruangan tidak ditemukan.");
    }
}
