package com.studiomusik.service;

/**
 * Input pengguna tidak valid: kosong, format salah, di luar rentang, {@code end <= start},
 * durasi bukan jam bulat, tanggal lampau, {@code quantity < 1}, atau id yang dirujuk tidak ada
 * (lihat {@code API_CONTRACTS.md} §6).
 */
public class ValidationException extends ServiceException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
