package com.studiomusik.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Satu-satunya pintu pembuatan koneksi JDBC ke database {@code studio_musik}.
 *
 * <p>Konfigurasi dibaca dari {@code config/db.properties} (relatif terhadap working directory,
 * yaitu root proyek saat {@code mvn test} atau {@code javafx:run}). Kunci yang wajib diisi:
 * {@code db.url}, {@code db.username}, {@code db.password}. File itu di-ignore Git; templatnya adalah
 * {@code config/db.properties.example}.</p>
 *
 * <p>Aturan keamanan: tidak ada credential di source; password dan URL tidak pernah dicetak atau
 * dimasukkan ke pesan error; setiap pemanggil mendapat koneksi baru dan bertanggung jawab menutupnya
 * (try-with-resources). Driver MySQL Connector/J terdaftar otomatis dari classpath runtime
 * ({@code DriverManager}).</p>
 *
 * <p>Semua kegagalan dilaporkan sebagai {@link SQLException} berpesan Indonesia yang jelas, sehingga
 * kontrak DAO ({@code throws SQLException}) tidak berubah.</p>
 */
public final class DatabaseConnection {

    /** Lokasi file konfigurasi, relatif terhadap working directory. */
    static final Path DEFAULT_CONFIG_PATH = Path.of("config", "db.properties");

    static final String KEY_URL = "db.url";
    static final String KEY_USERNAME = "db.username";
    static final String KEY_PASSWORD = "db.password";

    /** Awalan nilai templat pada {@code db.properties.example}. */
    static final String PLACEHOLDER_PREFIX = "GANTI_DENGAN_";

    private static final String HINT_COPY_TEMPLATE =
            "Salin config/db.properties.example menjadi config/db.properties lalu isi dengan akun MySQL lokal Anda.";

    private DatabaseConnection() {
    }

    /**
     * Membuka koneksi baru memakai {@code config/db.properties}.
     *
     * @return koneksi baru (auto-commit aktif); wajib ditutup oleh pemanggil
     * @throws SQLException bila file/properti tidak valid, driver tidak ada, atau database tidak terjangkau
     */
    public static Connection getConnection() throws SQLException {
        return getConnection(DEFAULT_CONFIG_PATH);
    }

    /**
     * Varian untuk pengujian (package-private): membaca konfigurasi dari {@code configFile}.
     * Bukan jalur koneksi alternatif untuk aplikasi.
     */
    static Connection getConnection(Path configFile) throws SQLException {
        Config config = loadConfig(configFile);
        try {
            return DriverManager.getConnection(config.url(), config.username(), config.password());
        } catch (SQLException e) {
            // Pesan asli driver ("No suitable driver found for <URL>") dapat memuat URL, jadi tidak diteruskan.
            if (e.getMessage() != null && e.getMessage().startsWith("No suitable driver")) {
                throw new SQLException("Driver JDBC untuk db.url tidak ditemukan di classpath. "
                        + "Pastikan db.url diawali \"jdbc:mysql:\" dan dependency mysql-connector-j tersedia "
                        + "(jalankan lewat Maven).", e.getSQLState(), e.getErrorCode());
            }
            throw new SQLException("Gagal terhubung ke database. Pastikan MySQL aktif, database studio_musik "
                    + "sudah dibuat, serta db.url, db.username, dan db.password di config/db.properties benar. "
                    + "(" + e.getClass().getSimpleName() + ", SQLState=" + e.getSQLState()
                    + ", kode=" + e.getErrorCode() + ")",
                    e.getSQLState(), e.getErrorCode(), e);
        }
    }

    /** Membaca dan memvalidasi file konfigurasi. Package-private agar dapat diuji tanpa database. */
    static Config loadConfig(Path configFile) throws SQLException {
        if (!Files.isRegularFile(configFile)) {
            throw new SQLException("File konfigurasi database tidak ditemukan: " + displayPath(configFile)
                    + ". " + HINT_COPY_TEMPLATE);
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            // Tidak melampirkan cause: pesan IOException dapat memuat potongan isi file.
            throw new SQLException("File konfigurasi database tidak dapat dibaca: " + displayPath(configFile)
                    + " (" + e.getClass().getSimpleName() + ").");
        }

        String url = required(props, KEY_URL, configFile).trim();
        String username = required(props, KEY_USERNAME, configFile).trim();
        String password = required(props, KEY_PASSWORD, configFile);

        if (!url.startsWith("jdbc:")) {
            throw new SQLException("Properti " + KEY_URL + " harus diawali \"jdbc:\" (contoh: "
                    + "jdbc:mysql://localhost:3306/studio_musik). Periksa " + displayPath(configFile) + ".");
        }
        return new Config(url, username, password);
    }

    private static String required(Properties props, String key, Path configFile) throws SQLException {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new SQLException("Properti " + key + " belum diisi di " + displayPath(configFile) + ". "
                    + HINT_COPY_TEMPLATE);
        }
        if (value.trim().startsWith(PLACEHOLDER_PREFIX)) {
            throw new SQLException("Properti " + key + " di " + displayPath(configFile)
                    + " masih berisi nilai placeholder (" + PLACEHOLDER_PREFIX + "...). "
                    + "Ganti dengan nilai MySQL lokal Anda.");
        }
        return value;
    }

    private static String displayPath(Path path) {
        return path.toString().replace('\\', '/');
    }

    /**
     * Konfigurasi hasil baca. {@code toString()} sengaja tidak memuat nilai apa pun agar credential
     * tidak bocor lewat log/debugger.
     */
    record Config(String url, String username, String password) {
        @Override
        public String toString() {
            return "Config[url=***, username=***, password=***]";
        }
    }
}
