package com.studiomusik.service;

import com.studiomusik.dao.CustomerDAO;
import com.studiomusik.dao.InstrumentDAO;
import com.studiomusik.dao.RoomDAO;
import com.studiomusik.model.Customer;
import com.studiomusik.model.Instrument;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.InstrumentStatus;
import com.studiomusik.model.enums.RoomStatus;
import com.studiomusik.util.DatabaseConnection;
import com.studiomusik.util.PasswordUtil;
import org.junit.jupiter.api.Assumptions;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pendukung tes Service (bukan kelas tes; namanya sengaja tidak berpola Test*).
 *
 * <p>Strategi isolasi data (tanpa {@code reset.sql}, seed tidak pernah diubah):
 * <ul>
 *   <li>Setiap tes membuat Customer/Room/Instrument/Admin <b>baru</b> berawalan unik
 *       ({@code svc-test-…@example.com}, {@code SvcTest Room …}, {@code SvcTest Instr …},
 *       {@code svctest_…}); booking uji hanya memakai room uji sehingga tidak pernah bertabrakan
 *       dengan booking seed.</li>
 *   <li>{@link #cleanUp()} (dipanggil {@code @AfterEach}) menghapus HANYA baris berawalan itu —
 *       booking-nya dulu (detail ikut terhapus lewat CASCADE), lalu customer/room/instrument/admin.
 *       Baris seed tidak cocok dengan pola sehingga tidak tersentuh.</li>
 *   <li>Tes yang butuh MySQL di-skip otomatis (JUnit Assumptions) bila database tidak terjangkau atau
 *       seed STEP 2 belum dimuat, sehingga {@code mvn test} tetap hijau di mesin tanpa database.</li>
 * </ul>
 * SQL mentah di sini hanya untuk setup/cleanup tes dan hitung baris; semua parameter di-bind.
 */
final class ServiceTestSupport {

    /** Password uji sementara (bukan password demo; D-024). */
    static final String TEST_PASSWORD = "Uji-Password-123";

    static final String EMAIL_PREFIX = "svc-test-";
    static final String ROOM_PREFIX = "SvcTest Room ";
    static final String INSTRUMENT_PREFIX = "SvcTest Instr ";
    static final String ADMIN_PREFIX = "svctest_";

    private static Boolean ready;
    private static String notReadyReason = "";
    private static String sharedHash;

    private ServiceTestSupport() {
    }

    // ------------------------------------------------------------- kesiapan DB

    /** Skip tes bila DB/seed tidak siap (dicache; probe hanya sekali per JVM). */
    static synchronized void requireDatabaseWithSeed() {
        if (ready == null) {
            ready = probe();
        }
        Assumptions.assumeTrue(ready, notReadyReason);
    }

    private static boolean probe() {
        String sql = "SELECT (SELECT COUNT(*) FROM customer WHERE customer_id = 1), "
                + "(SELECT COUNT(*) FROM booking WHERE booking_id IN (1, 4, 5)), "
                + "(SELECT COUNT(*) FROM admin WHERE username = 'admin')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) != 1 || rs.getInt(2) != 3 || rs.getInt(3) != 1) {
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

    // ------------------------------------------------------------------ factory

    /** Hash PBKDF2 password uji, dibuat sekali agar tes tetap cepat (iterasi 600000). */
    static synchronized String testPasswordHash() {
        if (sharedHash == null) {
            sharedHash = PasswordUtil.hash(TEST_PASSWORD);
        }
        return sharedHash;
    }

    static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    static String uniqueEmail() {
        return EMAIL_PREFIX + uniqueSuffix() + "@example.com";
    }

    /** Customer uji (disimpan lewat CustomerDAO dengan hash password uji). */
    static Customer newCustomer() throws SQLException {
        Customer c = new Customer(0, "Customer Uji " + uniqueSuffix(), "081200000000", uniqueEmail(),
                testPasswordHash());
        c.setId(new CustomerDAO().insert(c));
        return c;
    }

    static Room newRoom(String pricePerHour, RoomStatus status) throws SQLException {
        Room r = new Room(0, ROOM_PREFIX + uniqueSuffix(), "Uji", new BigDecimal(pricePerHour), status);
        r.setRoomId(new RoomDAO().insert(r));
        return r;
    }

    static Instrument newInstrument(String rentalPrice, InstrumentStatus status) throws SQLException {
        Instrument i = new Instrument(0, INSTRUMENT_PREFIX + uniqueSuffix(), "Uji",
                new BigDecimal(rentalPrice), status);
        i.setInstrumentId(new InstrumentDAO().insert(i));
        return i;
    }

    /** Admin uji dengan password {@code rawPassword} (seed admin tidak dipakai: password demo tidak ada di repo). */
    static String newAdmin(String rawPassword) throws SQLException {
        String username = ADMIN_PREFIX + uniqueSuffix();
        exec("INSERT INTO admin (username, password_hash, name) VALUES (?, ?, ?)",
                username, PasswordUtil.hash(rawPassword), "Admin Uji");
        return username;
    }

    // ------------------------------------------------------------------ helper

    static LocalDate futureDate(int daysAhead) {
        return LocalDate.now().plusDays(daysAhead);
    }

    static LocalTime t(int hour) {
        return LocalTime.of(hour, 0);
    }

    static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    /** Peta instrumentId -> quantity dari pasangan berurutan (id1, qty1, id2, qty2, ...). */
    static Map<Integer, Integer> qty(int... idAndQuantityPairs) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < idAndQuantityPairs.length; i += 2) {
            map.put(idAndQuantityPairs[i], idAndQuantityPairs[i + 1]);
        }
        return map;
    }

    static long count(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    static long bookingCountForRoom(int roomId) throws SQLException {
        return count("SELECT COUNT(*) FROM booking WHERE room_id = ?", roomId);
    }

    static long detailCountForBooking(int bookingId) throws SQLException {
        return count("SELECT COUNT(*) FROM booking_instrument WHERE booking_id = ?", bookingId);
    }

    static int exec(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    // ----------------------------------------------------------------- cleanup

    /**
     * Menghapus semua data uji berawalan khusus (aman diulang; dipanggil setelah tiap tes dan sekali
     * sebelum kelas tes untuk membersihkan sisa eksekusi yang terputus). Seed tidak cocok dengan pola.
     */
    static void cleanUp() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String emailPattern = EMAIL_PREFIX + "%";
            String roomPattern = ROOM_PREFIX + "%";
            String instrumentPattern = INSTRUMENT_PREFIX + "%";
            run(conn, "DELETE FROM booking WHERE customer_id IN "
                    + "(SELECT customer_id FROM customer WHERE email LIKE ?) "
                    + "OR room_id IN (SELECT room_id FROM room WHERE name LIKE ?)",
                    emailPattern, roomPattern);
            run(conn, "DELETE FROM booking_instrument WHERE instrument_id IN "
                    + "(SELECT instrument_id FROM instrument WHERE name LIKE ?)", instrumentPattern);
            run(conn, "DELETE FROM customer WHERE email LIKE ?", emailPattern);
            run(conn, "DELETE FROM room WHERE name LIKE ?", roomPattern);
            run(conn, "DELETE FROM instrument WHERE name LIKE ?", instrumentPattern);
            run(conn, "DELETE FROM admin WHERE username LIKE ?", ADMIN_PREFIX + "%");
        } catch (SQLException e) {
            throw new IllegalStateException("Pembersihan data uji gagal: " + e.getMessage(), e);
        }
    }

    private static void run(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            ps.executeUpdate();
        }
    }
}
