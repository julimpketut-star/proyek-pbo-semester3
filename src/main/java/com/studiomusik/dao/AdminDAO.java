package com.studiomusik.dao;

import com.studiomusik.model.Admin;
import com.studiomusik.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Akses data tabel {@code admin} (hanya baca; akun Admin berasal dari seed).
 * Kolom {@code admin_id, username, password_hash, name} dipetakan ke
 * {@code id, username, passwordHash, name}; {@code phone} dan {@code email} selalu {@code null} (D-005).
 * Tidak berisi aturan bisnis; {@link SQLException} dibiarkan naik ke Service.
 */
public class AdminDAO {

    private static final String SELECT_BASE =
            "SELECT admin_id, username, password_hash, name FROM admin";

    public Optional<Admin> findByUsername(String username) throws SQLException {
        String sql = SELECT_BASE + " WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Admin> findById(int adminId) throws SQLException {
        String sql = SELECT_BASE + " WHERE admin_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static Admin map(ResultSet rs) throws SQLException {
        return new Admin(
                rs.getInt("admin_id"),
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("password_hash"));
    }
}
