package com.studiomusik.dao;

import com.studiomusik.model.Room;
import com.studiomusik.model.enums.RoomStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomDAOTest {

    private final RoomDAO dao = new RoomDAO();
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

    private int insertNew(String name, String price, RoomStatus status) throws SQLException {
        int id = dao.insert(new Room(0, name, "Tipe Uji", new BigDecimal(price), status));
        created.add(id);
        return id;
    }

    @Test
    @DisplayName("findById memetakan seed Studio A dan Studio D (MAINTENANCE)")
    void mapsSeed() throws Exception {
        Room a = dao.findById(1).orElseThrow();
        assertEquals("Studio A", a.getRoomName());
        assertEquals("Band Rehearsal", a.getRoomType());
        assertEquals(0, new BigDecimal("150000.00").compareTo(a.getPricePerHour()));
        assertEquals(RoomStatus.AVAILABLE, a.getStatus());
        Room d = dao.findById(5).orElseThrow();
        assertEquals(RoomStatus.MAINTENANCE, d.getStatus());
        assertTrue(dao.findById(999_999).isEmpty());
    }

    @Test
    @DisplayName("insert, findAll, findByStatus, count")
    void insertAndQueries() throws Exception {
        long before = dao.count();
        String name = "Uji-" + DaoTestSupport.uniqueSuffix();
        int id = insertNew(name, "99999.50", RoomStatus.MAINTENANCE);
        Room r = dao.findById(id).orElseThrow();
        assertEquals(name, r.getRoomName());
        assertEquals(new BigDecimal("99999.50"), r.getPricePerHour());
        assertEquals(RoomStatus.MAINTENANCE, r.getStatus());
        assertEquals(before + 1, dao.count());
        assertTrue(dao.findAll().stream().anyMatch(x -> x.getRoomId() == id));
        assertTrue(dao.findByStatus(RoomStatus.MAINTENANCE).stream().anyMatch(x -> x.getRoomId() == id));
        assertTrue(dao.findByStatus(RoomStatus.AVAILABLE).stream().noneMatch(x -> x.getRoomId() == id));
        assertTrue(dao.findByStatus(RoomStatus.AVAILABLE).stream().allMatch(x -> x.getStatus() == RoomStatus.AVAILABLE));
    }

    @Test
    @DisplayName("update mengubah semua kolom; id tidak ada -> false")
    void update() throws Exception {
        int id = insertNew("Uji-" + DaoTestSupport.uniqueSuffix(), "10000", RoomStatus.AVAILABLE);
        Room edit = new Room(id, "Nama Baru", "Tipe Baru", new BigDecimal("12345.67"), RoomStatus.MAINTENANCE);
        assertTrue(dao.update(edit));
        Room r = dao.findById(id).orElseThrow();
        assertEquals("Nama Baru", r.getRoomName());
        assertEquals("Tipe Baru", r.getRoomType());
        assertEquals(new BigDecimal("12345.67"), r.getPricePerHour());
        assertEquals(RoomStatus.MAINTENANCE, r.getStatus());
        assertFalse(dao.update(new Room(999_999, "x", "y", BigDecimal.ONE, RoomStatus.AVAILABLE)));
    }

    @Test
    @DisplayName("delete room tidak terpakai berhasil; FK RESTRICT untuk room yang dipakai booking")
    void deleteAndRestrict() throws Exception {
        int id = insertNew("Uji-" + DaoTestSupport.uniqueSuffix(), "1000", RoomStatus.AVAILABLE);
        assertTrue(dao.delete(id));
        assertTrue(dao.findById(id).isEmpty());
        assertFalse(dao.delete(id));

        long before = dao.count();
        SQLException e = assertThrows(SQLException.class, () -> dao.delete(1));
        assertTrue(e.getSQLState().startsWith("23"), "SQLState: " + e.getSQLState());
        assertTrue(dao.findById(1).isPresent());
        assertEquals(before, dao.count());
    }

    @Test
    @DisplayName("CHECK harga >= 0 ditolak database")
    void negativePriceRejectedByDatabase() {
        assertThrows(SQLException.class, () -> DaoTestSupport.exec(
                "INSERT INTO room (name, type, price_per_hour, status) VALUES (?, ?, ?, ?)",
                "Uji-" + DaoTestSupport.uniqueSuffix(), "Tipe", new BigDecimal("-1.00"), "AVAILABLE"));
    }

    @Test
    @DisplayName("findByIdForUpdate: membaca pada koneksi transaksi, tidak menutup koneksi, mengunci baris")
    void findByIdForUpdate() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            Room r = dao.findByIdForUpdate(conn, 1).orElseThrow();
            assertEquals("Studio A", r.getRoomName());
            assertEquals(0, new BigDecimal("150000.00").compareTo(r.getPricePerHour()));
            assertTrue(dao.findByIdForUpdate(conn, 999_999).isEmpty());
            assertFalse(conn.isClosed(), "DAO tidak boleh menutup koneksi transaksi");
            assertFalse(conn.getAutoCommit(), "DAO tidak boleh mengubah autoCommit");
        });
    }

    @Test
    @DisplayName("findByIdForUpdate benar-benar mengunci: transaksi kedua menunggu lalu kena lock timeout")
    void findByIdForUpdateLocksRow() throws Exception {
        DaoTestSupport.inRollbackTx(first -> {
            dao.findByIdForUpdate(first, 2).orElseThrow();
            try (Connection second = com.studiomusik.util.DatabaseConnection.getConnection()) {
                second.setAutoCommit(false);
                try {
                    DaoTestSupport.exec(second, "SET SESSION innodb_lock_wait_timeout = 1");
                    SQLException e = assertThrows(SQLException.class, () -> dao.findByIdForUpdate(second, 2));
                    assertTrue(e.getMessage() != null && !e.getMessage().isEmpty());
                } finally {
                    second.rollback();
                }
            }
        });
    }
}
