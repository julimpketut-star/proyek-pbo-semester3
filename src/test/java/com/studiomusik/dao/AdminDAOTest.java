package com.studiomusik.dao;

import com.studiomusik.model.Admin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdminDAOTest {

    private final AdminDAO dao = new AdminDAO();

    @BeforeAll
    static void requireDb() {
        DaoTestSupport.requireDatabaseWithSeed();
    }

    @Test
    @DisplayName("findByUsername memetakan semua kolom seed; phone/email null (D-005)")
    void findByUsernameMapsSeed() throws Exception {
        Optional<Admin> found = dao.findByUsername("admin");
        assertTrue(found.isPresent());
        Admin a = found.get();
        assertEquals(1, a.getId());
        assertEquals("admin", a.getUsername());
        assertEquals("Administrator Studio", a.getName());
        assertTrue(a.getPasswordHash().startsWith("pbkdf2_sha256$600000$"));
        assertNull(a.getPhone());
        assertNull(a.getEmail());
        assertEquals("Admin", a.getRole());
    }

    @Test
    @DisplayName("findById ditemukan dan tidak ditemukan")
    void findById() throws Exception {
        assertEquals("admin", dao.findById(1).orElseThrow().getUsername());
        assertTrue(dao.findById(999_999).isEmpty());
        assertTrue(dao.findByUsername("tidak-ada-" + DaoTestSupport.uniqueSuffix()).isEmpty());
    }

    @Test
    @DisplayName("Admin baru (disisipkan lewat SQL setup karena kontrak DAO hanya baca) dapat ditemukan; username unik")
    void insertedAdminFoundAndUsernameUnique() throws Exception {
        String username = "adm-" + DaoTestSupport.uniqueSuffix();
        DaoTestSupport.exec("INSERT INTO admin (username, password_hash, name) VALUES (?, ?, ?)",
                username, "hash-uji", "Admin Uji");
        try {
            Admin a = dao.findByUsername(username).orElseThrow();
            assertEquals("Admin Uji", a.getName());
            assertEquals("hash-uji", a.getPasswordHash());
            assertEquals(a.getId(), dao.findById(a.getId()).orElseThrow().getId());

            SQLException dup = assertThrows(SQLException.class, () -> DaoTestSupport.exec(
                    "INSERT INTO admin (username, password_hash, name) VALUES (?, ?, ?)",
                    username, "x", "Duplikat"));
            assertTrue(dup.getSQLState().startsWith("23"), "SQLState integritas: " + dup.getSQLState());
        } finally {
            DaoTestSupport.exec("DELETE FROM admin WHERE username = ?", username);
        }
        assertTrue(dao.findByUsername(username).isEmpty());
    }
}
