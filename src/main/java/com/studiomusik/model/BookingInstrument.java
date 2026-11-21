package com.studiomusik.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Detail alat pada sebuah {@link Booking} (relasi many-to-many Booking–Instrument dan bagian
 * komposisi dari Booking). {@code rentalPrice} adalah <b>snapshot</b> harga saat alat ditambahkan
 * (BR-12): tidak mengikuti perubahan harga {@link Instrument}, dan sengaja tanpa setter.
 * Yang boleh diubah setelah dibuat hanya {@code quantity} (≥ 1, BR-18).
 */
public class BookingInstrument {

    private int bookingInstrumentId;
    private int bookingId;
    private final Instrument instrument;
    private int quantity;
    private final BigDecimal rentalPrice;

    public BookingInstrument(int bookingInstrumentId, int bookingId, Instrument instrument,
                             int quantity, BigDecimal rentalPrice) {
        this.bookingInstrumentId = bookingInstrumentId;
        this.bookingId = bookingId;
        this.instrument = Objects.requireNonNull(instrument, "instrument tidak boleh null");
        this.quantity = requireValidQuantity(quantity);
        Objects.requireNonNull(rentalPrice, "rentalPrice tidak boleh null");
        if (rentalPrice.signum() < 0) {
            throw new IllegalArgumentException("rentalPrice tidak boleh negatif: " + rentalPrice);
        }
        this.rentalPrice = rentalPrice;
    }

    public int getBookingInstrumentId() {
        return bookingInstrumentId;
    }

    public void setBookingInstrumentId(int bookingInstrumentId) {
        this.bookingInstrumentId = bookingInstrumentId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public int getQuantity() {
        return quantity;
    }

    /** @throws IllegalArgumentException bila {@code quantity} kurang dari 1 (BR-18) */
    public void setQuantity(int quantity) {
        this.quantity = requireValidQuantity(quantity);
    }

    /** @return harga snapshot per unit per sesi saat alat ditambahkan ke booking. */
    public BigDecimal getRentalPrice() {
        return rentalPrice;
    }

    /** Subtotal = snapshot {@code rentalPrice × quantity}, skala 2, HALF_UP (BR-10, BR-12). */
    public BigDecimal calculateSubtotal() {
        return rentalPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private static int requireValidQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity minimal 1: " + quantity);
        }
        return quantity;
    }
}
