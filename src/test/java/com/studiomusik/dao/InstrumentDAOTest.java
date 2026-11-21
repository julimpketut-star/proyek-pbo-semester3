package com.studiomusik.dao;

import com.studiomusik.model.Instrument;
import com.studiomusik.model.enums.InstrumentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentDAOTest {

    private final InstrumentDAO dao = new InstrumentDAO();
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

    private int insertNew(String price, InstrumentStatus status) throws SQLException {
        int id = dao.insert(new Instrument(0, "Uji-" + DaoTestSupport.uniqueSuffix(), "Tipe Uji",
                new BigDecimal(price), status));
        created.add(id);
        return id;
    }

    @Test
    @DisplayName("findById memetakan seed instrument 5 (harga sekarang 10000) dan 7 (MAINTENANCE)")
    void mapsSeed() throws Exception {
        Instrument mic = dao.findById(5).orElseThrow();
        assertEquals("Mikrofon Shure SM58", mic.getName());
        assertEquals("Mikrofon", mic.getType());
        assertEquals(0, new BigDecimal("10000.00").compareTo(mic.getRentalPrice()));
        assertEquals(InstrumentStatus.AVAILABLE, mic.getStatus());
        assertEquals(InstrumentStatus.MAINTENANCE, dao.findById(7).orElseThrow().getStatus());
        assertTrue(dao.findById(999_999).isEmpty());
    }

    @Test
    @DisplayName("findById(Connection) memakai koneksi pemanggil dan tidak menutupnya")
    void findByIdWithConnection() throws Exception {
        DaoTestSupport.inRollbackTx(conn -> {
            assertEquals("Drum Set Pearl", dao.findById(conn, 1).orElseThrow().getName());
            assertTrue(dao.findById(conn, 999_999).isEmpty());
            assertFalse(conn.isClosed());
            assertFalse(conn.getAutoCommit());
        });
    }

    @Test
    @DisplayName("insert, findAll, findByStatus, count")
    void insertAndQueries() throws Exception {
        long before = dao.count();
        int id = insertNew("45000.25", InstrumentStatus.MAINTENANCE);
        Instrument i = dao.findById(id).orElseThrow();
        assertEquals(new BigDecimal("45000.25"), i.getRentalPrice());
        assertEquals(InstrumentStatus.MAINTENANCE, i.getStatus());
        assertEquals(before + 1, dao.count());
        assertTrue(dao.findAll().stream().anyMatch(x -> x.getInstrumentId() == id));
        assertTrue(dao.findByStatus(InstrumentStatus.MAINTENANCE).stream().anyMatch(x -> x.getInstrumentId() == id));
        assertTrue(dao.findByStatus(InstrumentStatus.AVAILABLE).stream().noneMatch(x -> x.getInstrumentId() == id));
    }

    @Test
    @DisplayName("update mengubah semua kolom; id tidak ada -> false")
    void update() throws Exception {
        int id = insertNew("1000", InstrumentStatus.AVAILABLE);
        assertTrue(dao.update(new Instrument(id, "Nama Baru", "Tipe Baru", new BigDecimal("2500.50"),
                InstrumentStatus.MAINTENANCE)));
        Instrument i = dao.findById(id).orElseThrow();
        assertEquals("Nama Baru", i.getName());
        assertEquals("Tipe Baru", i.getType());
        assertEquals(new BigDecimal("2500.50"), i.getRentalPrice());
        assertEquals(InstrumentStatus.MAINTENANCE, i.getStatus());
        assertFalse(dao.update(new Instrument(999_999, "x", "y", BigDecimal.ONE, InstrumentStatus.AVAILABLE)));
    }

    @Test
    @DisplayName("delete instrument tidak terpakai berhasil; FK RESTRICT untuk instrument yang dipakai booking")
    void deleteAndRestrict() throws Exception {
        int id = insertNew("1000", InstrumentStatus.AVAILABLE);
        assertTrue(dao.delete(id));
        assertTrue(dao.findById(id).isEmpty());
        assertFalse(dao.delete(id));

        long before = dao.count();
        SQLException e = assertThrows(SQLException.class, () -> dao.delete(1));
        assertTrue(e.getSQLState().startsWith("23"), "SQLState: " + e.getSQLState());
        assertTrue(dao.findById(1).isPresent());
        assertEquals(before, dao.count());
    }
}
