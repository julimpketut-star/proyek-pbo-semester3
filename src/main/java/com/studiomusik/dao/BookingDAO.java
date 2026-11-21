package com.studiomusik.dao;

import com.studiomusik.model.Booking;
import com.studiomusik.model.Customer;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.BookingStatus;
import com.studiomusik.model.enums.RoomStatus;
import com.studiomusik.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Akses data tabel {@code booking}. Booking hasil query diisi lewat JOIN dengan {@code customer} dan
 * {@code room}; detail alat TIDAK dimuat (gunakan {@link BookingInstrumentDAO#findByBookingId(int)}
 * lalu {@code Booking.setInstruments}). {@code Customer} hasil JOIN tidak membawa password_hash
 * (passwordHash = null) agar hash tidak menyebar ke objek booking.
 *
 * <p>Tanggal/waktu dipetakan dengan {@code setObject/getObject} bertipe {@code java.time}
 * (tanpa {@code java.sql.Timestamp}) agar tidak terkena konversi zona waktu driver.
 * Tidak berisi aturan bisnis; {@link SQLException} dibiarkan naik ke Service.
 */
public class BookingDAO {

    private static final String SELECT_JOINED =
            "SELECT b.booking_id, b.customer_id, b.room_id, b.booking_date, b.start_time, b.end_time, "
                    + "b.status AS booking_status, b.total_cost, b.created_at, "
                    + "c.name AS customer_name, c.phone AS customer_phone, c.email AS customer_email, "
                    + "r.name AS room_name, r.type AS room_type, r.price_per_hour, r.status AS room_status "
                    + "FROM booking b "
                    + "JOIN customer c ON c.customer_id = b.customer_id "
                    + "JOIN room r ON r.room_id = b.room_id";

    /** Urutan kontrak: booking_date DESC, start_time DESC (booking_id DESC sebagai pemutus seri). */
    private static final String ORDER_NEWEST_FIRST =
            " ORDER BY b.booking_date DESC, b.start_time DESC, b.booking_id DESC";

    private static final String OVERLAP_SQL =
            "SELECT 1 FROM booking "
                    + "WHERE room_id = ? AND booking_date = ? AND status IN ('PENDING', 'CONFIRMED') "
                    + "AND start_time < ? AND end_time > ? LIMIT 1";

    /**
     * Menyimpan booking (tanpa detail alat) pada koneksi transaksi milik pemanggil.
     * Bila {@code createdAt} null, database mengisi default {@code CURRENT_TIMESTAMP}.
     *
     * @return booking_id yang dibangkitkan database
     */
    public int insert(Connection conn, Booking booking) throws SQLException {
        boolean withCreatedAt = booking.getCreatedAt() != null;
        String sql = withCreatedAt
                ? "INSERT INTO booking (customer_id, room_id, booking_date, start_time, end_time, status, "
                        + "total_cost, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO booking (customer_id, room_id, booking_date, start_time, end_time, status, "
                        + "total_cost) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, booking.getCustomer().getId());
            ps.setInt(2, booking.getRoom().getRoomId());
            ps.setObject(3, booking.getBookingDate());
            ps.setObject(4, booking.getStartTime());
            ps.setObject(5, booking.getEndTime());
            ps.setString(6, booking.getStatus().name());
            ps.setBigDecimal(7, booking.getTotalCost());
            if (withCreatedAt) {
                ps.setObject(8, booking.getCreatedAt());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Insert booking tidak mengembalikan generated key.");
            }
        }
    }

    public Optional<Booking> findById(int bookingId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, bookingId);
        }
    }

    /** Memakai koneksi milik pemanggil; koneksi TIDAK ditutup di sini. */
    public Optional<Booking> findById(Connection conn, int bookingId) throws SQLException {
        String sql = SELECT_JOINED + " WHERE b.booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Booking> findAll() throws SQLException {
        String sql = SELECT_JOINED + ORDER_NEWEST_FIRST;
        List<Booking> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public List<Booking> findByCustomerId(int customerId) throws SQLException {
        String sql = SELECT_JOINED + " WHERE b.customer_id = ?" + ORDER_NEWEST_FIRST;
        List<Booking> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    /**
     * Cek tumpang tindih jadwal (DATABASE_CONTRACT §4): status PENDING/CONFIRMED dan
     * {@code existingStart < requestedEnd AND existingEnd > requestedStart}. Booking berurutan
     * (akhir = awal berikutnya) bukan overlap. Memakai koneksi/transaksi milik pemanggil.
     */
    public boolean existsOverlap(Connection conn, int roomId, LocalDate date,
                                 LocalTime start, LocalTime end) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(OVERLAP_SQL)) {
            ps.setInt(1, roomId);
            ps.setObject(2, date);
            ps.setObject(3, end);     // existingStart < requestedEnd
            ps.setObject(4, start);   // existingEnd   > requestedStart
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Versi baca-saja untuk pengecekan UI; membuka dan menutup koneksinya sendiri. */
    public boolean existsOverlap(int roomId, LocalDate date, LocalTime start, LocalTime end) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return existsOverlap(conn, roomId, date, start, end);
        }
    }

    public boolean updateStatus(Connection conn, int bookingId, BookingStatus status) throws SQLException {
        String sql = "UPDATE booking SET status = ? WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateTotalCost(Connection conn, int bookingId, BigDecimal totalCost) throws SQLException {
        String sql = "UPDATE booking SET total_cost = ? WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, totalCost);
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        }
    }

    public long countByStatus(BookingStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM booking WHERE status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static Booking map(ResultSet rs) throws SQLException {
        Customer customer = new Customer(
                rs.getInt("customer_id"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                rs.getString("customer_email"),
                null);
        Room room = new Room(
                rs.getInt("room_id"),
                rs.getString("room_name"),
                rs.getString("room_type"),
                rs.getBigDecimal("price_per_hour"),
                RoomStatus.valueOf(rs.getString("room_status")));
        return new Booking(
                rs.getInt("booking_id"),
                customer,
                room,
                rs.getObject("booking_date", LocalDate.class),
                rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_time", LocalTime.class),
                BookingStatus.valueOf(rs.getString("booking_status")),
                rs.getBigDecimal("total_cost"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
