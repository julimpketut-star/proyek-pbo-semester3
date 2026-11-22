package com.studiomusik.service;

/**
 * Exception dasar lapisan Service (D-017). Bersifat unchecked dan pesannya berbahasa Indonesia serta
 * aman ditampilkan ke pengguna (tanpa detail SQL, path, atau credential). Detail teknis kegagalan
 * database hanya dicatat ke log developer dan, bila perlu, tersedia lewat {@link #getCause()}.
 *
 * <p>Subclass: {@link ValidationException} (input salah) dan {@link BusinessRuleException}
 * (pelanggaran aturan bisnis BR-xx). Instans {@code ServiceException} biasa dipakai untuk kegagalan
 * teknis (mis. {@code SQLException} yang dibungkus).
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
