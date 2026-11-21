package com.studiomusik.dao;

import com.studiomusik.model.Customer;
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
 * Akses data tabel {@code customer}. Kolom {@code customer_id, name, phone, email, password_hash}
 * dipetakan ke {@code id, name, phone, email, passwordHash} (D-014).
 * Tidak berisi aturan bisnis (validasi, BR-22, dsb. ada di Service). Semua method melempar
 * {@link SQLException}, termasuk pelanggaran UNIQUE email dan FK RESTRICT saat hapus.
 */
public class CustomerDAO {

    private static final String SELECT_BASE =
            "SELECT customer_id, name, phone, email, password_hash FROM customer";

    /** @return customer_id yang dibangkitkan database */
    public int insert(Customer customer) throws SQLException {
        String sql = "INSERT INTO customer (name, phone, email, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Insert customer tidak mengembalikan generated key.");
            }
        }
    }

    public Optional<Customer> findById(int customerId) throws SQLException {
        String sql = SELECT_BASE + " WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Customer> findByEmail(String email) throws SQLException {
        String sql = SELECT_BASE + " WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Customer> findAll() throws SQLException {
        String sql = SELECT_BASE + " ORDER BY customer_id";
        List<Customer> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    /** Memperbarui name, phone, email saja (password_hash tidak disentuh). */
    public boolean update(Customer customer) throws SQLException {
        String sql = "UPDATE customer SET name = ?, phone = ?, email = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setInt(4, customer.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** @return true bila baris terhapus; FK RESTRICT (masih dipakai booking) menghasilkan SQLException. */
    public boolean delete(int customerId) throws SQLException {
        String sql = "DELETE FROM customer WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM customer WHERE email = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByEmailExcludingId(String email, int customerId) throws SQLException {
        String sql = "SELECT 1 FROM customer WHERE email = ? AND customer_id <> ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM customer";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static Customer map(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("password_hash"));
    }
}
