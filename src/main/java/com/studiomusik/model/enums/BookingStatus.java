package com.studiomusik.model.enums;

/**
 * Status booking. Aturan perilaku berada di enum ini agar satu-satunya sumber kebenaran:
 * <ul>
 *   <li>BR-07: hanya {@link #PENDING} dan {@link #CONFIRMED} yang memblokir jadwal.</li>
 *   <li>BR-14: transisi yang diizinkan adalah PENDING→CONFIRMED, PENDING→CANCELLED,
 *       CONFIRMED→COMPLETED, CONFIRMED→CANCELLED. COMPLETED dan CANCELLED bersifat final.</li>
 * </ul>
 * Nama konstanta identik dengan nilai ENUM kolom {@code booking.status} di database.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED;

    /** @return true bila booking berstatus ini memblokir jadwal ruangan (BR-07). */
    public boolean blocksSchedule() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Memeriksa apakah perpindahan dari status ini ke {@code next} diizinkan (BR-14).
     * Perpindahan ke status yang sama dan ke {@code null} dianggap tidak diizinkan.
     */
    public boolean canTransitionTo(BookingStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
