package com.studiomusik.service;

import com.studiomusik.dao.CustomerDAO;
import com.studiomusik.model.Customer;
import com.studiomusik.util.PasswordUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Helper internal layer Service (package-private; bukan bagian API). Memusatkan validasi input yang
 * dipakai bersama, pemetaan kegagalan SQL ke {@link ServiceException}, dan pencatatan log developer.
 * Tidak pernah mencatat password maupun hash.
 */
final class ServiceSupport {

    static final Logger LOG = Logger.getLogger("com.studiomusik.service");

    static final String DB_ERROR_MESSAGE =
            "Terjadi kesalahan pada penyimpanan data. Silakan coba lagi atau hubungi Admin.";

    // Batas panjang mengikuti database/schema.sql
    static final int MAX_PERSON_NAME = 100;
    static final int MAX_PHONE = 20;
    static final int MAX_EMAIL = 100;
    static final int MAX_ASSET_NAME = 50;
    static final int MAX_ASSET_TYPE = 50;

    /** Batas password (keputusan implementasi STEP 5; dicatat di PROGRESS_HANDOFF.md). */
    static final int MIN_PASSWORD_LENGTH = 8;
    static final int MAX_PASSWORD_LENGTH = 128;

    /** Batas DECIMAL(10,2). */
    private static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");

    /** MySQL ER_DUP_ENTRY. */
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    /** MySQL ER_ROW_IS_REFERENCED_2 dan ER_ROW_IS_REFERENCED (FK RESTRICT saat hapus induk). */
    private static final int MYSQL_ROW_IS_REFERENCED_2 = 1451;
    private static final int MYSQL_ROW_IS_REFERENCED = 1217;

    private ServiceSupport() {
    }

    // ------------------------------------------------------------ validasi input

    /** @return teks ter-trim; {@link ValidationException} bila null/kosong atau melebihi {@code maxLength}. */
    static String requireText(String value, String label, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(label + " wajib diisi.");
        }
        if (trimmed.length() > maxLength) {
            throw new ValidationException(label + " maksimal " + maxLength + " karakter.");
        }
        return trimmed;
    }

    /** Email ter-trim dan huruf kecil (login/unik tidak peka huruf besar-kecil); format dasar divalidasi. */
    static String normalizeEmail(String value) {
        String email = requireText(value, "Email", MAX_EMAIL).toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Format email tidak valid.");
        }
        return email;
    }

    /** Nomor telepon: spasi dan tanda hubung dibuang; hasil {@code +?} diikuti 8–15 digit. */
    static String normalizePhone(String value) {
        String raw = requireText(value, "Nomor telepon", MAX_PHONE * 2);
        String phone = raw.replace(" ", "").replace("-", "");
        if (!PHONE_PATTERN.matcher(phone).matches() || phone.length() > MAX_PHONE) {
            throw new ValidationException("Nomor telepon tidak valid (8–15 digit, boleh diawali +).");
        }
        return phone;
    }

    static void requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new ValidationException("Password wajib diisi.");
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password minimal " + MIN_PASSWORD_LENGTH + " karakter.");
        }
        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Password maksimal " + MAX_PASSWORD_LENGTH + " karakter.");
        }
    }

    /** Nilai uang: tidak null, tidak negatif, muat DECIMAL(10,2); hasil berskala 2 (HALF_UP). */
    static BigDecimal requireMoney(BigDecimal value, String label) {
        if (value == null) {
            throw new ValidationException(label + " wajib diisi.");
        }
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        if (scaled.signum() < 0) {
            throw new ValidationException(label + " tidak boleh negatif.");
        }
        if (scaled.compareTo(MAX_MONEY) > 0) {
            throw new ValidationException(label + " terlalu besar (maksimal 99.999.999,99).");
        }
        return scaled;
    }

    // ---------------------------------------------------------- kegagalan SQL

    static boolean isDuplicateKey(SQLException e) {
        return e != null && e.getErrorCode() == MYSQL_DUPLICATE_ENTRY;
    }

    static boolean isReferencedByChild(SQLException e) {
        return e != null && (e.getErrorCode() == MYSQL_ROW_IS_REFERENCED_2
                || e.getErrorCode() == MYSQL_ROW_IS_REFERENCED);
    }

    /** Mencatat detail ke log developer dan mengembalikan exception berpesan umum (tanpa detail SQL). */
    static ServiceException dbError(String operation, SQLException e) {
        LOG.log(Level.SEVERE, "Kegagalan database saat " + operation, e);
        return new ServiceException(DB_ERROR_MESSAGE, e);
    }

    /** Kegagalan tak terduga (bukan input pengguna) pada {@code operation}. */
    static ServiceException unexpected(String operation, RuntimeException e) {
        LOG.log(Level.SEVERE, "Kegagalan tak terduga saat " + operation, e);
        return new ServiceException("Terjadi kesalahan internal saat " + operation + ".", e);
    }

    // ------------------------------------------------------------- Customer

    /** Salinan Customer tanpa passwordHash agar hash tidak menyebar ke lapisan atas. */
    static Customer withoutHash(Customer customer) {
        return new Customer(customer.getId(), customer.getName(), customer.getPhone(),
                customer.getEmail(), null);
    }

    /**
     * Pembuatan Customer baru yang dipakai bersama {@code AuthService.registerCustomer} dan
     * {@code CustomerService.addCustomer}: validasi, email unik (BR-20), hash PBKDF2, simpan.
     */
    static Customer registerNewCustomer(CustomerDAO customerDAO, String name, String phone,
                                        String email, String rawPassword) {
        String cleanName = requireText(name, "Nama", MAX_PERSON_NAME);
        String cleanPhone = normalizePhone(phone);
        String cleanEmail = normalizeEmail(email);
        requirePassword(rawPassword);
        try {
            if (customerDAO.existsByEmail(cleanEmail)) {
                throw new BusinessRuleException("Email sudah terdaftar. Gunakan email lain.");
            }
            String hash = PasswordUtil.hash(rawPassword);
            int id = customerDAO.insert(new Customer(0, cleanName, cleanPhone, cleanEmail, hash));
            return new Customer(id, cleanName, cleanPhone, cleanEmail, null);
        } catch (SQLException e) {
            if (isDuplicateKey(e)) { // balapan: email terdaftar di antara cek dan insert
                throw new BusinessRuleException("Email sudah terdaftar. Gunakan email lain.", e);
            }
            throw dbError("menyimpan customer", e);
        }
    }
}
