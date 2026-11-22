package com.studiomusik.service;

import com.studiomusik.dao.CustomerDAO;
import com.studiomusik.model.Admin;
import com.studiomusik.model.Customer;
import com.studiomusik.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static com.studiomusik.service.ServiceTestSupport.TEST_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tes integrasi AuthService (MySQL + seed; di-skip bila DB tidak siap). AC-R17 dan alur login. */
class AuthServiceTest {

    private final AuthService auth = new AuthService();
    private final CustomerDAO customerDAO = new CustomerDAO();

    @BeforeAll
    static void requireDb() {
        ServiceTestSupport.requireDatabaseWithSeed();
        ServiceTestSupport.cleanUp();
    }

    @AfterEach
    void cleanUp() {
        ServiceTestSupport.cleanUp();
    }

    @Test
    @DisplayName("AC-R17: registrasi menyimpan password sebagai hash PBKDF2, bukan plaintext")
    void registerStoresPasswordAsHash() throws Exception {
        String email = ServiceTestSupport.uniqueEmail();
        Customer registered = auth.registerCustomer("Ayu Uji", "0812-3456-7890", email, TEST_PASSWORD);

        assertTrue(registered.getId() > 0);
        assertNull(registered.getPasswordHash(), "objek hasil service tidak membawa hash");

        Customer stored = customerDAO.findByEmail(email).orElseThrow();
        assertNotEquals(TEST_PASSWORD, stored.getPasswordHash());
        assertFalse(stored.getPasswordHash().contains(TEST_PASSWORD));
        assertTrue(stored.getPasswordHash().startsWith("pbkdf2_sha256$"));
        assertTrue(PasswordUtil.verify(TEST_PASSWORD, stored.getPasswordHash()));
        assertEquals("081234567890", stored.getPhone(), "spasi/tanda hubung dibuang");
    }

    @Test
    @DisplayName("AC-R17: email duplikat ditolak (juga beda huruf besar/kecil dan spasi)")
    void duplicateEmailIsRejected() throws Exception {
        String email = ServiceTestSupport.uniqueEmail();
        auth.registerCustomer("Pertama", "081234567890", email, TEST_PASSWORD);

        assertThrows(BusinessRuleException.class,
                () -> auth.registerCustomer("Kedua", "081234567891", email, TEST_PASSWORD));
        assertThrows(BusinessRuleException.class,
                () -> auth.registerCustomer("Ketiga", "081234567892", "  " + email.toUpperCase(Locale.ROOT) + " ",
                        TEST_PASSWORD));
        assertEquals(1, ServiceTestSupport.count("SELECT COUNT(*) FROM customer WHERE email = ?", email));
    }

    @Test
    @DisplayName("Email seed (customer 1) tidak dapat didaftarkan ulang")
    void seedEmailCannotBeRegisteredAgain() {
        assertThrows(BusinessRuleException.class,
                () -> auth.registerCustomer("Peniru", "081234567890", "ayu.lestari@example.com", TEST_PASSWORD));
    }

    @Test
    @DisplayName("loginCustomer: sukses dengan email (tidak peka huruf besar-kecil), hash tidak ikut")
    void customerLoginSucceeds() throws Exception {
        String email = ServiceTestSupport.uniqueEmail();
        auth.registerCustomer("Login Uji", "081234567890", email, TEST_PASSWORD);

        Customer loggedIn = auth.loginCustomer(email, TEST_PASSWORD);
        assertEquals(email, loggedIn.getEmail());
        assertEquals("Customer", loggedIn.getRole());
        assertNull(loggedIn.getPasswordHash());
        assertEquals(email, auth.loginCustomer("  " + email.toUpperCase(Locale.ROOT), TEST_PASSWORD).getEmail());
    }

    @Test
    @DisplayName("loginCustomer: password salah dan email tidak terdaftar memberi pesan generik yang SAMA")
    void customerLoginFailureIsGeneric() throws Exception {
        String email = ServiceTestSupport.uniqueEmail();
        auth.registerCustomer("Login Uji", "081234567890", email, TEST_PASSWORD);

        BusinessRuleException wrongPassword = assertThrows(BusinessRuleException.class,
                () -> auth.loginCustomer(email, "password-salah-123"));
        BusinessRuleException unknownEmail = assertThrows(BusinessRuleException.class,
                () -> auth.loginCustomer(ServiceTestSupport.uniqueEmail(), TEST_PASSWORD));
        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
        assertEquals(AuthService.CUSTOMER_LOGIN_FAILED, wrongPassword.getMessage());
        assertFalse(wrongPassword.getMessage().contains(email));
    }

    @Test
    @DisplayName("loginAdmin: sukses dengan username; hash tidak ikut; peran Admin")
    void adminLoginSucceeds() throws Exception {
        String username = ServiceTestSupport.newAdmin(TEST_PASSWORD);

        Admin admin = auth.loginAdmin(" " + username + " ", TEST_PASSWORD);
        assertEquals(username, admin.getUsername());
        assertEquals("Admin", admin.getRole());
        assertNull(admin.getPasswordHash());
        assertNotNull(admin.getName());
    }

    @Test
    @DisplayName("loginAdmin: password salah dan username tidak ada memberi pesan generik yang SAMA")
    void adminLoginFailureIsGeneric() throws Exception {
        String username = ServiceTestSupport.newAdmin(TEST_PASSWORD);

        BusinessRuleException wrongPassword = assertThrows(BusinessRuleException.class,
                () -> auth.loginAdmin(username, "password-salah-123"));
        BusinessRuleException unknownUser = assertThrows(BusinessRuleException.class,
                () -> auth.loginAdmin("svctest_tidak_ada", TEST_PASSWORD));
        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
        assertEquals(AuthService.ADMIN_LOGIN_FAILED, wrongPassword.getMessage());
    }

    @Test
    @DisplayName("Akun Customer tidak dapat login sebagai Admin (dan sebaliknya)")
    void rolesAreSeparated() throws Exception {
        String email = ServiceTestSupport.uniqueEmail();
        auth.registerCustomer("Peran Uji", "081234567890", email, TEST_PASSWORD);
        String username = ServiceTestSupport.newAdmin(TEST_PASSWORD);

        assertThrows(BusinessRuleException.class, () -> auth.loginAdmin(email, TEST_PASSWORD));
        assertThrows(BusinessRuleException.class, () -> auth.loginCustomer(username, TEST_PASSWORD));
    }
}
