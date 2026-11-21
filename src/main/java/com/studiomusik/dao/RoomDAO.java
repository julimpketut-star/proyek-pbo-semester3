package com.studiomusik.dao;

import com.studiomusik.model.Room;
import com.studiomusik.model.enums.RoomStatus;
import com.studiomusik.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Akses data tabel {@code room}. Pemetaan (D-014): {@code room_id→roomId, name→roomName,
 * type→roomType, price_per_hour→pricePerHour (BigDecimal), status→RoomStatus (name()/valueOf)}.
 * Tidak berisi aturan bisnis; {@link SQLException} dibiarkan naik ke Service.
 */
public class RoomDAO {

    private static final String SELECT_BASE =
            "SELECT room_id, name, type, price_per_hour, status FROM room";

    /** @return room_id yang dibangkitkan database */
    public int insert(Room room) throws SQLException {
        String sql = "INSERT INTO room (name, type, price_per_hour, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getRoomName());
            ps.setString(2, room.getRoomType());
            ps.setBigDecimal(3, room.getPricePerHour());
            ps.setString(4, room.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Insert room tidak mengembalikan generated key.");
            }
        }
    }

    public Optional<Room> findById(int roomId) throws SQLException {
        String sql = SELECT_BASE + " WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Membaca room dengan {@code SELECT ... FOR UPDATE} pada koneksi transaksi milik pemanggil.
     * Koneksi TIDAK ditutup, autoCommit TIDAK diubah, dan tidak ada commit/rollback di sini.
     */
    public Optional<Room> findByIdForUpdate(Connection conn, int roomId) throws SQLException {
        String sql = SELECT_BASE + " WHERE room_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Room> findAll() throws SQLException {
        String sql = SELECT_BASE + " ORDER BY room_id";
        List<Room> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public List<Room> findByStatus(RoomStatus status) throws SQLException {
        String sql = SELECT_BASE + " WHERE status = ? ORDER BY room_id";
        List<Room> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public boolean update(Room room) throws SQLException {
        String sql = "UPDATE room SET name = ?, type = ?, price_per_hour = ?, status = ? WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomName());
            ps.setString(2, room.getRoomType());
            ps.setBigDecimal(3, room.getPricePerHour());
            ps.setString(4, room.getStatus().name());
            ps.setInt(5, room.getRoomId());
            return ps.executeUpdate() > 0;
        }
    }

    /** @return true bila baris terhapus; FK RESTRICT (masih dipakai booking) menghasilkan SQLException. */
    public boolean delete(int roomId) throws SQLException {
        String sql = "DELETE FROM room WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            return ps.executeUpdate() > 0;
        }
    }

    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM room";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static Room map(ResultSet rs) throws SQLException {
        return new Room(
                rs.getInt("room_id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getBigDecimal("price_per_hour"),
                RoomStatus.valueOf(rs.getString("status")));
    }
}
