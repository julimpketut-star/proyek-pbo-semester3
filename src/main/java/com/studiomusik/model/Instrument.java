package com.studiomusik.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.studiomusik.model.enums.InstrumentStatus;

/**
 * Alat musik yang dapat disewa. {@code rentalPrice} adalah harga per unit per sesi booking (A-01).
 * {@code instrumentId = 0} berarti belum disimpan ke database.
 */
public class Instrument {

    private int instrumentId;
    private String name;
    private String type;
    private BigDecimal rentalPrice;
    private InstrumentStatus status;

    public Instrument(int instrumentId, String name, String type, BigDecimal rentalPrice, InstrumentStatus status) {
        this.instrumentId = instrumentId;
        this.name = name;
        this.type = type;
        this.rentalPrice = requireNonNegative(rentalPrice, "rentalPrice");
        this.status = Objects.requireNonNull(status, "status tidak boleh null");
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(int instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getRentalPrice() {
        return rentalPrice;
    }

    public void setRentalPrice(BigDecimal rentalPrice) {
        this.rentalPrice = requireNonNegative(rentalPrice, "rentalPrice");
    }

    public InstrumentStatus getStatus() {
        return status;
    }

    public void setStatus(InstrumentStatus status) {
        this.status = Objects.requireNonNull(status, "status tidak boleh null");
    }

    /** @return true bila status AVAILABLE; alat MAINTENANCE tidak dapat dipilih (BR-06). */
    public boolean isAvailable() {
        return status == InstrumentStatus.AVAILABLE;
    }

    /**
     * Biaya sewa = {@code rentalPrice × quantity} (BR-10), skala 2, HALF_UP.
     *
     * @throws IllegalArgumentException bila {@code quantity} negatif
     */
    public BigDecimal calculateCost(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity tidak boleh negatif: " + quantity);
        }
        return rentalPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " tidak boleh null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " tidak boleh negatif: " + value);
        }
        return value;
    }
}
