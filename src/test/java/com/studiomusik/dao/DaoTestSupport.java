package com.studiomusik.dao;

import com.studiomusik.util.DatabaseConnection;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Pendukung tes integrasi DAO (bukan kelas tes; namanya sengaja tidak berpola Test*).
 *
 * <p>Strategi isolasi data:
 * <ul>
 *   <li>Tes method ber-{@code Connection} berjalan dalam transaksi lalu di-ROLLBACK ({@link #inRollbackTx}).</li>
 *   <li>Tes method tanpa Connection (DAO commit sendiri) memakai identitas unik (UUID) dan
 *       membersihkan barisnya di blok {@code finally}.</li>
 *   <li>Data seed hanya dibaca; tes tidak menjalankan reset.sql dan tidak menghapus/mengubah baris seed.</li>
 * </ul>
 * Tes di-skip otomatis (JUnit Assumptions) bila database tidak terjangkau atau seed belum dimuat,
 * sehingga {@code mvn test} tetap hijau di mesin tanpa MySQL.
 */
final class DaoTestSupport {

    /** Isi blok tes transaksional. */
    @FunctionalInterface
    interface TxBody {
        void run(Connection conn) throws Exception;
    }

    private static Boolean ready;
    private static String notReadyReason = "";

    private DaoTestSupport() {
    }

    /** Dipanggil dari {@code @BeforeAll}: skip seluruh kelas tes bila DB/seed tidak siap. */
    static synchronized void requireDatabaseWithSeed() {
        if (ready == null) {
            ready = probe();
        }
        Assumptions.assumeTrue(ready, notReadyReason);
    }

    private static boolean probe() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (!seedPresent(conn)) {
                notReadyReason = "Seed STEP 2 belum dimuat (jalankan reset.sql -> schema.sql -> seed.sql).";
                return false;
            }
            return true;
        } catch (SQLException | RuntimeException | LinkageError e) {
            notReadyReason = "Database tidak tersedia / config/db.properties belum siap: "
                    + e.getClass().getSimpleName();
            return false;
        }
    }

    private static boolean seedPresent(Connection conn) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM customer WHERE customer_id = 1), "
                + "(SELECT COUNT(*) FROM booking WHERE booking_id IN (1, 4, 5)), "
                + "(SELECT COUNT(*) FROM booking_instrument WHERE booking_instrument_id = 5), "
                + "(SELECT COUNT(*) FROM admin WHERE username = 'admin')";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) == 1 && rs.getInt(2) == 3 && rs.getInt(3) == 1 && rs.getInt(4) == 1;
        }
    }

    /**
     * Menjalankan {@code body} pada koneksi transaksi (autoCommit=false) dan SELALU rollback.
     * Setelah itu koneksi ditutup oleh pembuatnya (try-with-resources di sini), bukan oleh DAO.
     */
    static void inRollbackTx(TxBody body) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                body.run(conn);
            } finally {
                conn.rollback();
            }
        }
    }

    static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    static String uniqueEmail() {
        return "dao-test-" + uniqueSuffix() + "@example.com";
    }

    /** Menjalankan satu statement tulis mentah (khusus setup/cleanup tes, parameter selalu di-bind). */
    static int exec(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    static int exec(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return exec(conn, sql, params);
        }
    }
}
