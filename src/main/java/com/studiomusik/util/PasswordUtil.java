package com.studiomusik.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash dan verifikasi password (D-018, D-023). Hanya memakai JDK.
 *
 * <p>Format: {@code pbkdf2_sha256$<iterasi>$<saltBase64>$<hashBase64>}; PBKDF2WithHmacSHA256, salt
 * 16 byte dari {@link SecureRandom}, kunci 256 bit, iterasi bawaan {@value #DEFAULT_ITERATIONS}.
 * Jumlah iterasi dibaca dari string hash saat {@link #verify}, sehingga hash lama tetap valid bila
 * bawaan dinaikkan. Format dua arah kompatibel dengan {@code database/tools/Pbkdf2HashGenerator.java}
 * (Base64 standar, password dikodekan UTF-8 oleh JCA).</p>
 *
 * <p>Password mentah tidak pernah dicatat, dimasukkan ke pesan exception, atau disimpan; array
 * karakter sementara dihapus setelah dipakai.</p>
 */
public final class PasswordUtil {

    private static final String LABEL = "pbkdf2_sha256";
    private static final String JCA_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int KEY_BYTES = KEY_BITS / 8;

    /**
     * Batas atas iterasi yang mau dihitung saat verifikasi. Melindungi aplikasi dari nilai rusak di
     * database (mis. 2 miliar) yang membuat login menggantung. Jauh di atas bawaan 600000.
     */
    private static final int MAX_ITERATIONS = 10_000_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * Membuat hash baru dengan salt acak (hash dua panggilan untuk password sama selalu berbeda).
     *
     * @throws IllegalArgumentException bila {@code rawPassword} null atau kosong
     */
    public static String hash(String rawPassword) {
        return hash(rawPassword, DEFAULT_ITERATIONS);
    }

    /** Varian dengan iterasi tertentu (package-private; dipakai tes agar cepat dan untuk uji migrasi iterasi). */
    static String hash(String rawPassword, int iterations) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password tidak boleh kosong.");
        }
        if (iterations < 1 || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException("Jumlah iterasi harus antara 1 dan " + MAX_ITERATIONS + ".");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(rawPassword, salt, iterations);
        return LABEL + "$" + iterations + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    /**
     * Memeriksa password terhadap hash tersimpan dengan perbandingan waktu-konstan.
     *
     * @return {@code true} hanya bila cocok; {@code false} untuk password salah, argumen null/kosong,
     *         atau hash berformat rusak (tidak pernah melempar exception karena input)
     */
    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || rawPassword.isEmpty() || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 4 || !LABEL.equals(parts[0])) {
            return false;
        }
        int iterations;
        byte[] salt;
        byte[] expected;
        try {
            iterations = Integer.parseInt(parts[1]);
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) { // NumberFormatException termasuk subclass
            return false;
        }
        if (iterations < 1 || iterations > MAX_ITERATIONS || salt.length == 0 || expected.length != KEY_BYTES) {
            return false;
        }
        byte[] actual = derive(rawPassword, salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(String rawPassword, byte[] salt, int iterations) {
        char[] chars = rawPassword.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(JCA_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            // Tidak terjadi pada JDK standar; pesan sengaja tidak memuat password.
            throw new IllegalStateException("Algoritma " + JCA_ALGORITHM + " tidak tersedia di JVM ini.", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(chars, '\0');
        }
    }
}
