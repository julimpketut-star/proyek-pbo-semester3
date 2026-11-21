package com.studiomusik.model.enums;

/**
 * Status ruangan. Nama konstanta identik dengan nilai ENUM kolom {@code room.status}.
 * Ruangan {@link #MAINTENANCE} tidak dapat dipesan (BR-05).
 */
public enum RoomStatus {
    AVAILABLE,
    MAINTENANCE
}
