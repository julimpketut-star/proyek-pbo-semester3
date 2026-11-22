package com.studiomusik.service;

/**
 * Pelanggaran aturan bisnis (BR-xx) pada input yang bentuknya sudah valid: Room/Instrument
 * {@code MAINTENANCE}, jadwal bertumpuk, transisi status tidak valid, alat duplikat pada booking,
 * booking final tidak boleh diubah, entitas masih dipakai, email sudah terdaftar, bukan pemilik
 * booking, atau login gagal (lihat {@code API_CONTRACTS.md} §6).
 */
public class BusinessRuleException extends ServiceException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
