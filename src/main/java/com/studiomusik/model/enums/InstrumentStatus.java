package com.studiomusik.model.enums;

/**
 * Status alat musik. Nama konstanta identik dengan nilai ENUM kolom {@code instrument.status}.
 * Alat {@link #MAINTENANCE} tidak dapat dipilih (BR-06).
 */
public enum InstrumentStatus {
    AVAILABLE,
    MAINTENANCE
}
