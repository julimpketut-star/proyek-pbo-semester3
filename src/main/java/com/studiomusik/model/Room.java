package com.studiomusik.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.studiomusik.model.enums.RoomStatus;

/**
 * Ruangan studio. {@code roomId = 0} berarti belum disimpan ke database.
 * Semua nilai uang memakai {@link BigDecimal}; hasil perhitungan berskala 2 dengan
 * {@link RoundingMode#HALF_UP}.
 */
public class Room {

    private int roomId;
    private String roomName;
    private String roomType;
    private BigDecimal pricePerHour;
    private RoomStatus status;

    public Room(int roomId, String roomName, String roomType, BigDecimal pricePerHour, RoomStatus status) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomType = roomType;
        this.pricePerHour = requireNonNegative(pricePerHour, "pricePerHour");
        this.status = Objects.requireNonNull(status, "status tidak boleh null");
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = requireNonNegative(pricePerHour, "pricePerHour");
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = Objects.requireNonNull(status, "status tidak boleh null");
    }

    /** @return true bila status AVAILABLE; ruangan MAINTENANCE tidak dapat dipesan (BR-05). */
    public boolean isAvailable() {
        return status == RoomStatus.AVAILABLE;
    }

    /**
     * Biaya ruangan = {@code pricePerHour × hours} (BR-09), skala 2, HALF_UP.
     *
     * @throws IllegalArgumentException bila {@code hours} negatif
     */
    public BigDecimal calculateCost(int hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Jumlah jam tidak boleh negatif: " + hours);
        }
        return pricePerHour.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " tidak boleh null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " tidak boleh negatif: " + value);
        }
        return value;
    }
}
