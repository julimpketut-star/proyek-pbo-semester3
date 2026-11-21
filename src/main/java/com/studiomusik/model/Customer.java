package com.studiomusik.model;

/**
 * Pelanggan studio. Identitas login adalah email (D-003). Hanya menyimpan hash password
 * (format D-018), tidak pernah password mentah.
 */
public class Customer extends Person {

    private static final String ROLE = "Customer";

    private String passwordHash;

    public Customer(int id, String name, String phone, String email, String passwordHash) {
        super(id, name, phone, email);
        this.passwordHash = passwordHash;
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
