package com.studiomusik.dao;

import com.studiomusik.model.BookingInstrument;
import com.studiomusik.model.Instrument;
import com.studiomusik.model.enums.InstrumentStatus;
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
 * CRUD relasi Many-to-Many Booking–Instrument lewat tabel {@code booking_instrument}.
 *
 * <ul>
 *   <li>CREATE: {@link #insert}, {@link #insertAll}</li>
 *   <li>READ: JOIN {@code booking_instrument} ↔ {@code booking} ↔ {@code instrument}
 *       ({@code booking_id} dan {@code instrument_id}); {@code BookingInstrument.rentalPrice} diisi
 *       dari snapshot {@code booking_instrument.rental_price}, sedangkan {@code Instrument}
 *       (harga saat ini) dari tabel {@code instrument}</li>
 *   <li>UPDATE: hanya {@code quantity}; {@code rental_price} snapshot tidak pernah diubah (BR-12)</li>
 *   <li>DELETE: satu baris detail berdasarkan {@code booking_instrument_id}</li>
 * </ul>
 *
 * Semua operasi tulis memakai koneksi transaksi milik pemanggil (tidak ditutup, tidak di-commit,
 * autoCommit tidak diubah). Tidak berisi aturan bisnis; {@link SQLException} dibiarkan naik.
 */
public class BookingInstrumentDAO {

    private static final String SELECT_JOINED =
            "SELECT bi.booking_instrument_id, bi.booking_id, bi.instrument_id, bi.quantity, "
                    + "bi.rental_price AS snapshot_price, "
                    + "i.name AS instrument_name, i.type AS instrument_type, "
                    + "i.rental_price AS current_price, i.status AS instrument_status "
                    + "FROM booking_instrument bi "
                    + "JOIN booking b ON b.booking_id = bi.booking_id "
                    + "JOIN instrument i ON i.instrument_id = bi.instrument_id";

    // ---------------------------------------------------------------- CREATE

    /** @return booking_instrument_id yang dibangkitkan database */
    public int insert(Connection conn, BookingInstrument detail) throws SQLException {
        String sql = "INSERT INTO booking_instrument (booking_id, instrument_id, quantity, rental_price) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detail.getBookingId());
            ps.setInt(2, detail.getInstrument().getInstrumentId());
            ps.setInt(3, detail.getQuantity());
            ps.setBigDecimal(4, detail.getRentalPrice());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Insert booking_instrument tidak mengembalikan generated key.");
            }
        }
    }

    /** Batch insert semua detail untuk {@code bookingId} (nilai bookingId pada objek detail diabaikan). */
    public void insertAll(Connection conn, int bookingId, List<BookingInstrument> details) throws SQLException {
        if (details == null || details.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO booking_instrument (booking_id, instrument_id, quantity, rental_price) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (BookingInstrument detail : details) {
                ps.setInt(1, bookingId);
                ps.setInt(2, detail.getInstrument().getInstrumentId());
                ps.setInt(3, detail.getQuantity());
                ps.setBigDecimal(4, detail.getRentalPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ------------------------------------------------------------------ READ

    public List<BookingInstrument> findByBookingId(int bookingId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByBookingId(conn, bookingId);
        }
    }

    public List<BookingInstrument> findByBookingId(Connection conn, int bookingId) throws SQLException {
        String sql = SELECT_JOINED + " WHERE bi.booking_id = ? ORDER BY bi.booking_instrument_id";
        List<BookingInstrument> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public Optional<BookingInstrument> findById(Connection conn, int bookingInstrumentId) throws SQLException {
        String sql = SELECT_JOINED + " WHERE bi.booking_instrument_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingInstrumentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<BookingInstrument> findByBookingIdAndInstrumentId(Connection conn, int bookingId,
                                                                      int instrumentId) throws SQLException {
        String sql = SELECT_JOINED + " WHERE bi.booking_id = ? AND bi.instrument_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, instrumentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    // ---------------------------------------------------------------- UPDATE

    /** Hanya mengubah quantity; kolom rental_price (snapshot) tidak disentuh (BR-12). */
    public boolean updateQuantity(Connection conn, int bookingInstrumentId, int quantity) throws SQLException {
        String sql = "UPDATE booking_instrument SET quantity = ? WHERE booking_instrument_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, bookingInstrumentId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------- DELETE

    public boolean delete(Connection conn, int bookingInstrumentId) throws SQLException {
        String sql = "DELETE FROM booking_instrument WHERE booking_instrument_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingInstrumentId);
            return ps.executeUpdate() > 0;
        }
    }

    // --------------------------------------------------------------- mapping

    private static BookingInstrument map(ResultSet rs) throws SQLException {
        Instrument instrument = new Instrument(
                rs.getInt("instrument_id"),
                rs.getString("instrument_name"),
                rs.getString("instrument_type"),
                rs.getBigDecimal("current_price"),
                InstrumentStatus.valueOf(rs.getString("instrument_status")));
        return new BookingInstrument(
                rs.getInt("booking_instrument_id"),
                rs.getInt("booking_id"),
                instrument,
                rs.getInt("quantity"),
                rs.getBigDecimal("snapshot_price"));
    }
}
