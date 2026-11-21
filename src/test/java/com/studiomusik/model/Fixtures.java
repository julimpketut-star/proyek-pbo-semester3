package com.studiomusik.model;

import java.math.BigDecimal;

import com.studiomusik.model.enums.InstrumentStatus;
import com.studiomusik.model.enums.RoomStatus;

/** Pembuat data uji bersama (hanya untuk test). */
final class Fixtures {

    private Fixtures() {
    }

    static Customer customer() {
        return new Customer(1, "Made Ayu Lestari", "081234567801", "ayu.lestari@example.com", "hash-uji");
    }

    static Room room(String pricePerHour) {
        return new Room(1, "Studio A", "Band Rehearsal", new BigDecimal(pricePerHour), RoomStatus.AVAILABLE);
    }

    static Instrument instrument(int id, String name, String price) {
        return new Instrument(id, name, "Uji", new BigDecimal(price), InstrumentStatus.AVAILABLE);
    }

    static Instrument maintenanceInstrument(int id) {
        return new Instrument(id, "Alat Rusak", "Uji", new BigDecimal("25000"), InstrumentStatus.MAINTENANCE);
    }
}
