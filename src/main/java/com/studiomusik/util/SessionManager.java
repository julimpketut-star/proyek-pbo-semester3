package com.studiomusik.util;

import com.studiomusik.model.Admin;
import com.studiomusik.model.Customer;
import com.studiomusik.model.Person;

/**
 * Sesi pengguna aktif (singleton). Hanya menyimpan objek {@link Person} hasil Service, yang sudah
 * tidak membawa {@code passwordHash} (STEP 5). Sebagai lapisan tambahan, {@link #login(Person)}
 * mengosongkan {@code passwordHash} pada objek yang diterima; password mentah tidak pernah disimpan.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private Person currentUser;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /** Menyimpan pengguna aktif (menggantikan sesi sebelumnya). Hash pada objek dihapus. */
    public synchronized void login(Person user) {
        if (user == null) {
            throw new IllegalArgumentException("Pengguna tidak boleh null.");
        }
        if (user instanceof Customer customer) {
            customer.setPasswordHash(null);
        } else if (user instanceof Admin admin) {
            admin.setPasswordHash(null);
        }
        this.currentUser = user;
    }

    public synchronized void logout() {
        this.currentUser = null;
    }

    public synchronized Person getCurrentUser() {
        return currentUser;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public synchronized boolean isAdmin() {
        return currentUser instanceof Admin;
    }

    public synchronized boolean isCustomer() {
        return currentUser instanceof Customer;
    }

    /** Customer aktif, atau {@code null} bila belum login / bukan Customer. */
    public synchronized Customer getCurrentCustomer() {
        return currentUser instanceof Customer customer ? customer : null;
    }
}
