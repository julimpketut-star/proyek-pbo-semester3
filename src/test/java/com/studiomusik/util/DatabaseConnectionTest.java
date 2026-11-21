package com.studiomusik.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tes {@link DatabaseConnection}. Konfigurasi uji ditulis ke direktori sementara dengan nilai palsu
 * (bukan credential asli). Tes koneksi nyata memakai {@code config/db.properties} milik pengguna dan
 * dilewati otomatis bila file itu tidak ada, masih placeholder, atau database tidak terjangkau.
 */
class DatabaseConnectionTest {

    private static final String FAKE_PASSWORD = "sandi-palsu-untuk-uji-12345";
    private static final String FAKE_USER = "pengguna_uji";
    private static final String FAKE_URL_HOST = "host-uji.invalid";

    @TempDir
    Path tempDir;

    private Path writeConfig(String content) throws IOException {
        Path file = tempDir.resolve("db.properties");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private Path writeConfig(String url, String user, String password) throws IOException {
        return writeConfig("db.url=" + url + "\ndb.username=" + user + "\ndb.password=" + password + "\n");
    }

    private static String messageOf(Path config) {
        SQLException e = assertThrows(SQLException.class, () -> DatabaseConnection.loadConfig(config));
        assertNotNull(e.getMessage());
        return e.getMessage();
    }

    // ------------------------------------------------------------------ membaca konfigurasi

    @Test
    void loadConfigReadsAllThreeKeys() throws Exception {
        Path file = writeConfig("jdbc:mysql://" + FAKE_URL_HOST + ":3306/studio_musik?useUnicode=true&characterEncoding=UTF-8",
                FAKE_USER, FAKE_PASSWORD);

        DatabaseConnection.Config config = DatabaseConnection.loadConfig(file);

        assertEquals("jdbc:mysql://" + FAKE_URL_HOST + ":3306/studio_musik?useUnicode=true&characterEncoding=UTF-8", config.url());
        assertEquals(FAKE_USER, config.username());
        assertEquals(FAKE_PASSWORD, config.password());
    }

    @Test
    void loadConfigIgnoresCommentsAndKeepsPasswordWithSpecialCharacters() throws Exception {
        Path file = writeConfig("# komentar\n"
                + "db.url = jdbc:mysql://" + FAKE_URL_HOST + "/studio_musik\n"
                + "db.username = " + FAKE_USER + "\n"
                + "db.password=p@ss=w0rd#!$ %\n");

        DatabaseConnection.Config config = DatabaseConnection.loadConfig(file);

        assertEquals("jdbc:mysql://" + FAKE_URL_HOST + "/studio_musik", config.url());
        assertEquals(FAKE_USER, config.username());
        assertEquals("p@ss=w0rd#!$ %", config.password());
    }

    @Test
    void configToStringDoesNotLeakAnyValue() throws Exception {
        Path file = writeConfig("jdbc:mysql://" + FAKE_URL_HOST + "/studio_musik", FAKE_USER, FAKE_PASSWORD);

        String text = DatabaseConnection.loadConfig(file).toString();

        assertFalse(text.contains(FAKE_PASSWORD));
        assertFalse(text.contains(FAKE_USER));
        assertFalse(text.contains(FAKE_URL_HOST));
    }

    // ------------------------------------------------------------------ gagal dengan pesan jelas

    @Test
    void missingFileGivesClearMessageWithSetupHint() {
        Path missing = tempDir.resolve("tidak-ada").resolve("db.properties");

        String message = messageOf(missing);

        assertTrue(message.contains("tidak ditemukan"), message);
        assertTrue(message.contains("db.properties"), message);
        assertTrue(message.contains("db.properties.example"), "harus menunjuk ke templat: " + message);
    }

    @Test
    void directoryInsteadOfFileIsTreatedAsMissing() {
        String message = messageOf(tempDir);
        assertTrue(message.contains("tidak ditemukan"), message);
    }

    @Test
    void missingKeysAreReportedByName() throws Exception {
        assertTrue(messageOf(writeConfig("db.username=a\ndb.password=b\n")).contains("db.url"));
        assertTrue(messageOf(writeConfig("db.url=jdbc:mysql://x/y\ndb.password=b\n")).contains("db.username"));
        assertTrue(messageOf(writeConfig("db.url=jdbc:mysql://x/y\ndb.username=a\n")).contains("db.password"));
    }

    @Test
    void emptyOrBlankValuesAreReportedByName() throws Exception {
        assertTrue(messageOf(writeConfig("", FAKE_USER, FAKE_PASSWORD)).contains("db.url"));
        assertTrue(messageOf(writeConfig("jdbc:mysql://x/y", "   ", FAKE_PASSWORD)).contains("db.username"));
        assertTrue(messageOf(writeConfig("jdbc:mysql://x/y", FAKE_USER, "")).contains("db.password"));
        assertTrue(messageOf(writeConfig("")).contains("db."), "file kosong total");
    }

    @Test
    void templatePlaceholdersAreRejected() throws Exception {
        String message = messageOf(writeConfig("jdbc:mysql://localhost:3306/studio_musik",
                "GANTI_DENGAN_USERNAME_MYSQL", FAKE_PASSWORD));
        assertTrue(message.contains("db.username"), message);
        assertTrue(message.contains("placeholder"), message);

        String passwordMessage = messageOf(writeConfig("jdbc:mysql://localhost:3306/studio_musik",
                FAKE_USER, "GANTI_DENGAN_PASSWORD_MYSQL"));
        assertTrue(passwordMessage.contains("db.password"), passwordMessage);
        assertTrue(passwordMessage.contains("placeholder"), passwordMessage);
    }

    @Test
    void realTemplateFileFromRepositoryIsRejectedAsPlaceholder() {
        Path template = Path.of("config", "db.properties.example");
        assumeTrue(Files.isRegularFile(template), "config/db.properties.example tidak ada di working directory");

        String message = messageOf(template);

        assertTrue(message.contains("placeholder"), message);
    }

    @Test
    void urlWithoutJdbcPrefixIsRejectedWithoutEchoingTheValue() throws Exception {
        String message = messageOf(writeConfig("mysql://" + FAKE_URL_HOST + "/x", FAKE_USER, FAKE_PASSWORD));

        assertTrue(message.contains("jdbc:"), message);
        assertFalse(message.contains(FAKE_URL_HOST), "nilai URL tidak boleh muncul di pesan: " + message);
    }

    @Test
    void validationMessagesNeverContainPasswordOrUrl() throws Exception {
        String[] messages = {
                messageOf(writeConfig("mysql://" + FAKE_URL_HOST + "/x", FAKE_USER, FAKE_PASSWORD)),
                messageOf(writeConfig("jdbc:mysql://" + FAKE_URL_HOST + "/x", "", FAKE_PASSWORD)),
                messageOf(writeConfig("jdbc:mysql://" + FAKE_URL_HOST + "/x", "GANTI_DENGAN_X", FAKE_PASSWORD)),
        };
        for (String message : messages) {
            assertFalse(message.contains(FAKE_PASSWORD), message);
            assertFalse(message.contains(FAKE_URL_HOST), message);
        }
    }

    @Test
    void getConnectionWithMissingConfigFailsBeforeAnyNetworkAccess() {
        Path missing = tempDir.resolve("tidak-ada.properties");

        SQLException e = assertThrows(SQLException.class, () -> DatabaseConnection.getConnection(missing));

        assertTrue(e.getMessage().contains("tidak ditemukan"), e.getMessage());
        assertNull(e.getCause());
    }

    /**
     * Konfigurasi sah tetapi server tidak ada: harus menjadi SQLException berpesan Indonesia, tanpa
     * password dan tanpa URL di pesan. Port 1 di loopback ditolak seketika (tanpa menunggu timeout).
     * Dengan Connector/J di classpath yang gagal adalah koneksi; tanpa driver, "No suitable driver".
     */
    @Test
    void unreachableServerFailsWithSafeMessage() throws Exception {
        String url = "jdbc:mysql://127.0.0.1:1/studio_musik";
        Path file = writeConfig(url, FAKE_USER, FAKE_PASSWORD);

        SQLException e = assertThrows(SQLException.class, () -> DatabaseConnection.getConnection(file));

        String message = e.getMessage();
        assertFalse(message.contains(FAKE_PASSWORD), message);
        assertFalse(message.contains(url), message);
        assertFalse(message.contains("127.0.0.1:1"), message);
        assertTrue(message.contains("db.url") || message.contains("terhubung"), message);
    }

    @Test
    void unknownJdbcSubprotocolReportsMissingDriverWithoutUrlOrCause() throws Exception {
        String url = "jdbc:protokol-tidak-ada://" + FAKE_URL_HOST + "/x";
        Path file = writeConfig(url, FAKE_USER, FAKE_PASSWORD);

        SQLException e = assertThrows(SQLException.class, () -> DatabaseConnection.getConnection(file));

        assertTrue(e.getMessage().contains("Driver JDBC"), e.getMessage());
        assertFalse(e.getMessage().contains(FAKE_URL_HOST), e.getMessage());
        assertFalse(e.getMessage().contains(FAKE_PASSWORD), e.getMessage());
        assertNull(e.getCause(), "cause asli memuat URL, jadi tidak diteruskan");
    }

    // ------------------------------------------------------------------ koneksi nyata (opsional)

    /**
     * Koneksi ke MySQL lokal pengguna lewat {@code config/db.properties}. Dilewati (bukan gagal) bila
     * file tidak ada, masih placeholder, atau database tidak aktif, agar {@code mvn test} tetap hijau
     * di mesin tanpa database. Bila lolos, koneksi benar-benar dibuka dan diuji dengan query.
     */
    @Test
    void connectsToLocalDatabaseWhenConfigured() throws Exception {
        Path config = DatabaseConnection.DEFAULT_CONFIG_PATH;
        assumeTrue(Files.isRegularFile(config),
                "config/db.properties tidak ada: tes koneksi nyata dilewati (salin dari db.properties.example untuk mengaktifkan)");
        try {
            DatabaseConnection.loadConfig(config);
        } catch (SQLException e) {
            assumeTrue(false, "config/db.properties belum valid: " + e.getMessage());
        }

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            assumeTrue(false, "Database tidak terjangkau, tes koneksi nyata dilewati: " + e.getMessage());
        }

        try (Connection c = connection; Statement st = c.createStatement()) {
            assertTrue(c.isValid(5));
            assertFalse(c.isClosed());
            assertTrue(c.getAutoCommit(), "koneksi baru memakai auto-commit");
            try (ResultSet rs = st.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    void everyCallReturnsAnIndependentConnectionWhenConfigured() throws Exception {
        Path config = DatabaseConnection.DEFAULT_CONFIG_PATH;
        assumeTrue(Files.isRegularFile(config), "config/db.properties tidak ada");
        Connection first = null;
        Connection second = null;
        try {
            DatabaseConnection.loadConfig(config);
            first = DatabaseConnection.getConnection();
            second = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            if (first != null) {
                first.close();
            }
            assumeTrue(false, "Konfigurasi/database tidak siap: " + e.getMessage());
        }

        try (Connection b = second) {
            assertTrue(first != b);
            first.close();
            assertTrue(first.isClosed());
            assertFalse(b.isClosed(), "menutup satu koneksi tidak boleh memengaruhi yang lain");
        } finally {
            first.close(); // aman bila sudah tertutup
        }
    }
}
