package com.studiomusik.service;

import com.studiomusik.dao.InstrumentDAO;
import com.studiomusik.model.Instrument;
import com.studiomusik.model.enums.InstrumentStatus;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * CRUD Instrument (API_CONTRACTS §4.4). Validasi nama/tipe tidak kosong dan {@code rentalPrice >= 0}
 * (skala 2, HALF_UP). Instrument {@code MAINTENANCE} tidak dapat dipilih
 * ({@link #getSelectableInstruments()}; penegakan akhir di {@code BookingService}, BR-06).
 * Instrument yang sudah dipakai pada detail Booking tidak dapat dihapus (BR-22). Mengubah harga
 * tidak memengaruhi booking lama karena harga di-snapshot pada {@code booking_instrument} (BR-12).
 */
public class InstrumentService {

    private final InstrumentDAO instrumentDAO;

    public InstrumentService() {
        this(new InstrumentDAO());
    }

    public InstrumentService(InstrumentDAO instrumentDAO) {
        this.instrumentDAO = instrumentDAO;
    }

    public List<Instrument> getAllInstruments() {
        try {
            return instrumentDAO.findAll();
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca daftar alat", e);
        }
    }

    /** Hanya alat berstatus AVAILABLE (BR-06). */
    public List<Instrument> getSelectableInstruments() {
        try {
            return instrumentDAO.findByStatus(InstrumentStatus.AVAILABLE);
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca daftar alat tersedia", e);
        }
    }

    /** @throws ValidationException bila alat tidak ditemukan */
    public Instrument getInstrumentById(int instrumentId) {
        try {
            return instrumentDAO.findById(instrumentId).orElseThrow(InstrumentService::notFound);
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca alat", e);
        }
    }

    /**
     * @return alat tersimpan (objek baru dengan id; argumen tidak diubah)
     * @throws ValidationException bila nama/tipe kosong atau harga tidak valid
     */
    public Instrument addInstrument(Instrument instrument) {
        Instrument candidate = validated(instrument, 0);
        try {
            int id = instrumentDAO.insert(candidate);
            candidate.setInstrumentId(id);
            return candidate;
        } catch (SQLException e) {
            throw ServiceSupport.dbError("menyimpan alat", e);
        }
    }

    /**
     * @return alat hasil pembaruan (objek baru)
     * @throws ValidationException bila input tidak valid atau alat tidak ditemukan
     */
    public Instrument updateInstrument(Instrument instrument) {
        if (instrument == null) {
            throw new ValidationException("Data alat wajib diisi.");
        }
        Instrument candidate = validated(instrument, instrument.getInstrumentId());
        try {
            instrumentDAO.findById(candidate.getInstrumentId()).orElseThrow(InstrumentService::notFound);
            if (!instrumentDAO.update(candidate)) {
                throw notFound();
            }
            return candidate;
        } catch (SQLException e) {
            throw ServiceSupport.dbError("mengubah alat", e);
        }
    }

    /**
     * @throws BusinessRuleException bila alat sudah dipakai pada Booking (BR-22; FK RESTRICT)
     * @throws ValidationException   bila alat tidak ditemukan
     */
    public void deleteInstrument(int instrumentId) {
        try {
            if (!instrumentDAO.delete(instrumentId)) {
                throw notFound();
            }
        } catch (SQLException e) {
            if (ServiceSupport.isReferencedByChild(e)) {
                throw new BusinessRuleException("Alat tidak dapat dihapus karena sudah dipakai pada booking. "
                        + "Ubah statusnya menjadi MAINTENANCE bila tidak ingin dipilih lagi.", e);
            }
            throw ServiceSupport.dbError("menghapus alat", e);
        }
    }

    public long countInstruments() {
        try {
            return instrumentDAO.count();
        } catch (SQLException e) {
            throw ServiceSupport.dbError("menghitung alat", e);
        }
    }

    private static Instrument validated(Instrument instrument, int instrumentId) {
        if (instrument == null) {
            throw new ValidationException("Data alat wajib diisi.");
        }
        String name = ServiceSupport.requireText(instrument.getName(), "Nama alat", ServiceSupport.MAX_ASSET_NAME);
        String type = ServiceSupport.requireText(instrument.getType(), "Tipe alat", ServiceSupport.MAX_ASSET_TYPE);
        BigDecimal price = ServiceSupport.requireMoney(instrument.getRentalPrice(), "Harga sewa");
        return new Instrument(instrumentId, name, type, price, instrument.getStatus());
    }

    private static ValidationException notFound() {
        return new ValidationException("Alat tidak ditemukan.");
    }
}
