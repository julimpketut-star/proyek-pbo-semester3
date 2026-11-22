package com.studiomusik.service;

import com.studiomusik.dao.AdminDAO;
import com.studiomusik.dao.CustomerDAO;
import com.studiomusik.model.Admin;
import com.studiomusik.model.Customer;
import com.studiomusik.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/**
 * Autentikasi dan registrasi (API_CONTRACTS §4.1, BR-20).
 *
 * <ul>
 *   <li>Admin login memakai {@code username}; Customer login memakai {@code email}.</li>
 *   <li>Verifikasi password lewat {@link PasswordUtil#verify}. Kegagalan login (akun tidak ada atau
 *       password salah) memakai <b>satu pesan generik</b> agar akun tidak dapat ditebak; untuk akun yang
 *       tidak ada tetap dilakukan satu verifikasi dummy agar waktu respons tidak membocorkan keberadaan akun.</li>
 *   <li>Objek {@link Admin}/{@link Customer} yang dikembalikan <b>tidak membawa</b> {@code passwordHash}.</li>
 * </ul>
 */
public class AuthService {

    static final String ADMIN_LOGIN_FAILED = "Username atau password salah.";
    static final String CUSTOMER_LOGIN_FAILED = "Email atau password salah.";

    /** Hash dummy (dibuat sekali, malas) untuk menyamakan waktu verifikasi saat akun tidak ditemukan. */
    private static final class DummyHash {
        static final String VALUE = PasswordUtil.hash("dummy-password-untuk-menyamakan-waktu");
    }

    private final AdminDAO adminDAO;
    private final CustomerDAO customerDAO;

    public AuthService() {
        this(new AdminDAO(), new CustomerDAO());
    }

    public AuthService(AdminDAO adminDAO, CustomerDAO customerDAO) {
        this.adminDAO = adminDAO;
        this.customerDAO = customerDAO;
    }

    /**
     * @throws ValidationException    bila username/password kosong
     * @throws BusinessRuleException  bila kredensial salah (pesan generik)
     * @throws ServiceException       bila database gagal
     */
    public Admin loginAdmin(String username, String rawPassword) {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new ValidationException("Username dan password wajib diisi.");
        }
        try {
            Optional<Admin> found = adminDAO.findByUsername(cleanUsername);
            boolean ok = PasswordUtil.verify(rawPassword,
                    found.map(Admin::getPasswordHash).orElse(DummyHash.VALUE));
            if (found.isEmpty() || !ok) {
                throw new BusinessRuleException(ADMIN_LOGIN_FAILED);
            }
            Admin admin = found.get();
            return new Admin(admin.getId(), admin.getName(), admin.getUsername(), null);
        } catch (SQLException e) {
            throw ServiceSupport.dbError("login admin", e);
        }
    }

    /**
     * @throws ValidationException    bila email/password kosong
     * @throws BusinessRuleException  bila kredensial salah (pesan generik)
     * @throws ServiceException       bila database gagal
     */
    public Customer loginCustomer(String email, String rawPassword) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (cleanEmail.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new ValidationException("Email dan password wajib diisi.");
        }
        try {
            Optional<Customer> found = customerDAO.findByEmail(cleanEmail);
            boolean ok = PasswordUtil.verify(rawPassword,
                    found.map(Customer::getPasswordHash).orElse(DummyHash.VALUE));
            if (found.isEmpty() || !ok) {
                throw new BusinessRuleException(CUSTOMER_LOGIN_FAILED);
            }
            return ServiceSupport.withoutHash(found.get());
        } catch (SQLException e) {
            throw ServiceSupport.dbError("login customer", e);
        }
    }

    /**
     * Registrasi Customer baru: nama/telepon/email wajib dan berformat benar, password minimal
     * {@value ServiceSupport#MIN_PASSWORD_LENGTH} karakter, email belum terpakai (BR-20), password
     * disimpan sebagai hash PBKDF2.
     *
     * @return Customer tersimpan (dengan id) tanpa passwordHash
     * @throws ValidationException    bila input tidak valid
     * @throws BusinessRuleException  bila email sudah terdaftar
     */
    public Customer registerCustomer(String name, String phone, String email, String rawPassword) {
        return ServiceSupport.registerNewCustomer(customerDAO, name, phone, email, rawPassword);
    }
}
