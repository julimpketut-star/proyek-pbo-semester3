package com.studiomusik.dao;

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
 * Akses data tabel {@code instrument}. Pemetaan (D-014): {@code instrument_id→instrumentId,
 * name→name, type→type, rental_price→rentalPrice (BigDecimal), status→InstrumentStatus}.
 * Tidak berisi aturan bisnis; {@link SQLException} dibiarkan naik ke Service.
 */
public class InstrumentDAO {

    private static final String SELECT_BASE =
            "SELECT instrument_id, name, type, rental_price, status FROM instrument";

    /** @return instrument_id yang dibangkitkan database */
    public int insert(Instrument instrument) throws SQLException {
        String sql = "INSERT INTO instrument (name, type, rental_price, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, instrument.getName());
            ps.setString(2, instrument.getType());
            ps.setBigDecimal(3, instrument.getRentalPrice());
            ps.setString(4, instrument.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Insert instrument tidak mengembalikan generated key.");
            }
        }
    }

    public Optional<Instrument> findById(int instrumentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, instrumentId);
        }
    }

    /** Memakai koneksi milik pemanggil; koneksi TIDAK ditutup di sini. */
    public Optional<Instrument> findById(Connection conn, int instrumentId) throws SQLException {
        String sql = SELECT_BASE + " WHERE instrument_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, instrumentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Instrument> findAll() throws SQLException {
        String sql = SELECT_BASE + " ORDER BY instrument_id";
        List<Instrument> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public List<Instrument> findByStatus(InstrumentStatus status) throws SQLException {
        String sql = SELECT_BASE + " WHERE status = ? ORDER BY instrument_id";
        List<Instrument> result = new ArrayList<>();
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

    public boolean update(Instrument instrument) throws SQLException {
        String sql = "UPDATE instrument SET name = ?, type = ?, rental_price = ?, status = ? WHERE instrument_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, instrument.getName());
            ps.setString(2, instrument.getType());
            ps.setBigDecimal(3, instrument.getRentalPrice());
            ps.setString(4, instrument.getStatus().name());
            ps.setInt(5, instrument.getInstrumentId());
            return ps.executeUpdate() > 0;
        }
    }

    /** @return true bila baris terhapus; FK RESTRICT (masih dipakai booking_instrument) menghasilkan SQLException. */
    public boolean delete(int instrumentId) throws SQLException {
        String sql = "DELETE FROM instrument WHERE instrument_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, instrumentId);
            return ps.executeUpdate() > 0;
        }
    }

    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM instrument";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static Instrument map(ResultSet rs) throws SQLException {
        return new Instrument(
                rs.getInt("instrument_id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getBigDecimal("rental_price"),
                InstrumentStatus.valueOf(rs.getString("status")));
    }
}
