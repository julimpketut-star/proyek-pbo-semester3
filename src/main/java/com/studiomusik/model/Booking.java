package com.studiomusik.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.studiomusik.model.enums.BookingStatus;

/**
 * Pemesanan studio. Berasosiasi dengan satu {@link Customer} dan satu {@link Room}, dan
 * memiliki (komposisi) daftar {@link BookingInstrument}.
 *
 * <p>Model ini hanya berisi perhitungan murni dan pengelolaan detail di memori. Pengecekan
 * ketersediaan jadwal, validasi aturan bisnis, dan penyimpanan berada di Service/DAO (D-013).
 * Pelanggaran invariant dasar dilaporkan sebagai {@link IllegalArgumentException} atau
 * {@link IllegalStateException}; Service menerjemahkannya ke pesan untuk pengguna.
 */
public class Booking {

    private int bookingId;
    private Customer customer;
    private Room room;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private BigDecimal totalCost;
    private LocalDateTime createdAt;
    private final List<BookingInstrument> instruments = new ArrayList<>();

    /** Konstruktor lengkap (dipakai DAO saat memetakan baris database). */
    public Booking(int bookingId, Customer customer, Room room, LocalDate bookingDate,
                   LocalTime startTime, LocalTime endTime, BookingStatus status,
                   BigDecimal totalCost, LocalDateTime createdAt) {
        this.bookingId = bookingId;
        this.customer = customer;
        this.room = room;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Objects.requireNonNull(status, "status tidak boleh null");
        this.totalCost = Objects.requireNonNull(totalCost, "totalCost tidak boleh null");
        this.createdAt = createdAt;
    }

    /** Konstruktor untuk booking baru: belum disimpan, status {@code PENDING} (BR-13), total 0.00. */
    public Booking(Customer customer, Room room, LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        this(0, customer, room, bookingDate, startTime, endTime, BookingStatus.PENDING,
                BigDecimal.ZERO.setScale(2), null);
    }

    public int getBookingId() {
        return bookingId;
    }

    /** Menetapkan id booking dan menyelaraskan {@code bookingId} seluruh detail (komposisi). */
    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
        for (BookingInstrument detail : instruments) {
            detail.setBookingId(bookingId);
        }
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    /**
     * Setter biasa (dipakai DAO). Validasi transisi status (BR-14) dilakukan Service dengan
     * {@link BookingStatus#canTransitionTo(BookingStatus)}.
     */
    public void setStatus(BookingStatus status) {
        this.status = Objects.requireNonNull(status, "status tidak boleh null");
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = Objects.requireNonNull(totalCost, "totalCost tidak boleh null");
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** @return tampilan baca-saja daftar detail alat; perubahan harus lewat add/removeInstrument. */
    public List<BookingInstrument> getInstruments() {
        return Collections.unmodifiableList(instruments);
    }

    /** Mengganti seluruh detail dengan salinan {@code details} (dipakai DAO setelah JOIN). */
    public void setInstruments(List<BookingInstrument> details) {
        Objects.requireNonNull(details, "details tidak boleh null");
        instruments.clear();
        instruments.addAll(details);
    }

    /**
     * Durasi dalam jam bulat (A-05, BR-04).
     *
     * @throws IllegalStateException bila waktu belum diisi, {@code endTime <= startTime} (BR-03),
     *         atau waktu mulai/selesai tidak berada tepat pada jam (menit 00)
     */
    public int calculateDuration() {
        if (startTime == null || endTime == null) {
            throw new IllegalStateException("Waktu mulai dan waktu selesai harus diisi");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalStateException("Waktu selesai harus lebih besar dari waktu mulai");
        }
        if (!isWholeHour(startTime) || !isWholeHour(endTime)) {
            throw new IllegalStateException("Waktu mulai dan selesai harus tepat pada jam (menit 00)");
        }
        return (int) Duration.between(startTime, endTime).toHours();
    }

    /**
     * Total biaya = {@code room.calculateCost(durasi) + Σ subtotal detail} (BR-09 s.d. BR-11),
     * skala 2, HALF_UP. Method ini tidak mengubah field {@code totalCost}; pemanggil (Service)
     * yang menyimpannya lewat {@link #setTotalCost(BigDecimal)}.
     *
     * @throws IllegalStateException bila room belum diisi atau durasi tidak valid
     */
    public BigDecimal calculateTotalCost() {
        if (room == null) {
            throw new IllegalStateException("Room harus diisi untuk menghitung biaya");
        }
        BigDecimal total = room.calculateCost(calculateDuration());
        for (BookingInstrument detail : instruments) {
            total = total.add(detail.calculateSubtotal());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Menambahkan alat ke booking di memori dengan <b>snapshot</b> harga saat ini (BR-12).
     *
     * @throws IllegalArgumentException bila alat null, {@code quantity < 1} (BR-18), alat berstatus
     *         MAINTENANCE (BR-06), atau alat yang sama (berdasarkan instrumentId) sudah ada (BR-17)
     */
    public void addInstrument(Instrument instrument, int quantity) {
        if (instrument == null) {
            throw new IllegalArgumentException("Instrument tidak boleh null");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity minimal 1: " + quantity);
        }
        if (!instrument.isAvailable()) {
            throw new IllegalArgumentException("Instrument sedang MAINTENANCE: " + instrument.getName());
        }
        if (findDetail(instrument.getInstrumentId()) != null) {
            throw new IllegalArgumentException("Instrument sudah ada pada booking; ubah quantity-nya: "
                    + instrument.getName());
        }
        instruments.add(new BookingInstrument(0, bookingId, instrument, quantity, instrument.getRentalPrice()));
    }

    /**
     * Menghapus detail alat dari booking di memori.
     *
     * @throws IllegalArgumentException bila alat tersebut tidak ada pada booking
     */
    public void removeInstrument(int instrumentId) {
        BookingInstrument detail = findDetail(instrumentId);
        if (detail == null) {
            throw new IllegalArgumentException("Instrument dengan id " + instrumentId + " tidak ada pada booking");
        }
        instruments.remove(detail);
    }

    private BookingInstrument findDetail(int instrumentId) {
        for (BookingInstrument detail : instruments) {
            if (detail.getInstrument().getInstrumentId() == instrumentId) {
                return detail;
            }
        }
        return null;
    }

    private static boolean isWholeHour(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }
}
