package com.studiomusik.model;

/**
 * Admin/pengelola studio. Tabel {@code admin} tidak memiliki kolom phone dan email, sehingga
 * kedua atribut warisan dari {@link Person} bernilai {@code null} (D-005).
 */
public class Admin extends Person {

    private static final String ROLE = "Admin";

    private String username;
    private String passwordHash;

    public Admin(int id, String name, String username, String passwordHash) {
        super(id, name, null, null);
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String getRole() {
        return ROLE;
    }
}
