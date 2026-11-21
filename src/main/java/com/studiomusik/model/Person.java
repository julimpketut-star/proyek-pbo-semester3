package com.studiomusik.model;

/**
 * Abstraksi pengguna sistem (Class Diagram TK I). Menyimpan atribut umum Customer dan Admin.
 * Class ini abstract sehingga tidak dapat diinstansiasi; perilaku yang berbeda antar peran
 * diwujudkan lewat method abstract {@link #getRole()} yang di-override oleh subclass.
 */
public abstract class Person {

    private int id;
    private String name;
    private String phone;
    private String email;

    protected Person(int id, String name, String phone, String email) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** @return nama peran pengguna ("Customer" atau "Admin"). */
    public abstract String getRole();
}
