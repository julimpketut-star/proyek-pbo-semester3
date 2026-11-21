package com.studiomusik.dao;

import com.studiomusik.model.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomerDAOTest {

    private final CustomerDAO dao = new CustomerDAO();
    private final List<Integer> created = new ArrayList<>();

    @BeforeAll
    static void requireDb() {
        DaoTestSupport.requireDatabaseWithSeed();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (int id : created) {
            dao.delete(id);
        }
        created.clear();
    }

    private int insertNew(String email) throws SQLException {
        int id = dao.insert(new Customer(0, "Customer Uji", "081200000000", email, "hash-uji"));
        created.add(id);
        return id;
    }

    @Test
    @DisplayName("findById dan findByEmail memetakan seed customer 1 (semua kolom)")
    void mapsSeedCustomer() throws Exception {
        Customer c = dao.findById(1).orElseThrow();
        assertEquals(1, c.getId());
        assertEquals("Made Ayu Lestari", c.getName());
        assertEquals("081234567801", c.getPhone());
        assertEquals("ayu.lestari@example.com", c.getEmail());
        assertTrue(c.getPasswordHash().startsWith("pbkdf2_sha256$"));
        assertEquals("Customer", c.getRole());
        assertEquals(1, dao.findByEmail("ayu.lestari@example.com").orElseThrow().getId());
        assertTrue(dao.findById(999_999).isEmpty());
        assertTrue(dao.findByEmail("tidak.ada@example.com").isEmpty());
    }

    @Test
    @DisplayName("insert mengembalikan generated key; find, findAll, count konsisten")
    void insertAndFind() throws Exception {
        long before = dao.count();
        String email = DaoTestSupport.uniqueEmail();
        int id = insertNew(email);
        assertTrue(id > 6);
        Customer c = dao.findById(id).orElseThrow();
        assertEquals(email, c.getEmail());
        assertEquals("hash-uji", c.getPasswordHash());
        assertEquals(before + 1, dao.count());
        assertTrue(dao.findAll().stream().anyMatch(x -> x.getId() == id));
    }

    @Test
    @DisplayName("update hanya name, phone, email; password_hash tidak berubah; id tidak ada -> false")
    void updateOnlyProfileFields() throws Exception {
        int id = insertNew(DaoTestSupport.uniqueEmail());
        String newEmail = DaoTestSupport.uniqueEmail();
        Customer edit = new Customer(id, "Nama Baru", "089900001111", newEmail, "HASH-DIABAIKAN");
        assertTrue(dao.update(edit));
        Customer c = dao.findById(id).orElseThrow();
        assertEquals("Nama Baru", c.getName());
        assertEquals("089900001111", c.getPhone());
        assertEquals(newEmail, c.getEmail());
        assertEquals("hash-uji", c.getPasswordHash());
        assertFalse(dao.update(new Customer(999_999, "x", "1", DaoTestSupport.uniqueEmail(), "h")));
    }

    @Test
    @DisplayName("delete customer tanpa booking berhasil; id tidak ada -> false")
    void deleteUnused() throws Exception {
        int id = insertNew(DaoTestSupport.uniqueEmail());
        assertTrue(dao.delete(id));
        assertTrue(dao.findById(id).isEmpty());
        assertFalse(dao.delete(id));
    }

    @Test
    @DisplayName("Email unik: insert duplikat -> SQLException (uq_customer_email)")
    void duplicateEmailRejected() throws Exception {
        String email = DaoTestSupport.uniqueEmail();
        insertNew(email);
        SQLException e = assertThrows(SQLException.class, () -> insertNew(email));
        assertTrue(e.getSQLState().startsWith("23"), "SQLState: " + e.getSQLState());
        assertThrows(SQLException.class, () -> dao.insert(
                new Customer(0, "Dup Seed", "0812", "ayu.lestari@example.com", "h")));
    }

    @Test
    @DisplayName("existsByEmail dan existsByEmailExcludingId")
    void existsByEmail() throws Exception {
        assertTrue(dao.existsByEmail("ayu.lestari@example.com"));
        assertFalse(dao.existsByEmail(DaoTestSupport.uniqueEmail()));
        assertFalse(dao.existsByEmailExcludingId("ayu.lestari@example.com", 1));
        assertTrue(dao.existsByEmailExcludingId("ayu.lestari@example.com", 2));
    }

    @Test
    @DisplayName("FK RESTRICT: customer yang dipakai booking tidak dapat dihapus dan tetap ada")
    void deleteUsedCustomerRestricted() throws Exception {
        long before = dao.count();
        SQLException e = assertThrows(SQLException.class, () -> dao.delete(1));
        assertTrue(e.getSQLState().startsWith("23"), "SQLState: " + e.getSQLState());
        assertTrue(dao.findById(1).isPresent());
        assertEquals(before, dao.count());
    }
}
