package com.studiomusik.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Tes {@link PasswordUtil}. Password di sini hanya string uji sementara, bukan password demo
 * (D-024). Vektor dari utilitas {@code Pbkdf2HashGenerator} dibuat sekali dengan password uji ini.
 */
class PasswordUtilTest {

    private static final String PASSWORD = "kata-sandi-uji-sementara";

    /** Dibuat oleh database/tools/Pbkdf2HashGenerator.java (iterasi bawaan 600000) untuk PASSWORD. */
    private static final String GENERATOR_VECTOR_DEFAULT =
            "pbkdf2_sha256$600000$n9f+RVBopUJQnW38Vqnn/A==$gPG91KJUzrIlPZv/VmIKop5hWZW0VQmUtz3i3DLnxLg=";

    /** Dibuat oleh Pbkdf2HashGenerator --iterations 1000 untuk password "Üji-Sandi-ñ-日本" (non-ASCII, UTF-8). */
    private static final String GENERATOR_VECTOR_UNICODE =
            "pbkdf2_sha256$1000$EgM21VC4A/qXyVvDFLGggA==$D3wuti+jMG4gAVzzkf0oDFG1VStFMFXGSQCteBnzC20=";
    private static final String UNICODE_PASSWORD = "Üji-Sandi-ñ-日本";

    // ------------------------------------------------------------------ hash: format

    @Test
    void hashHasContractFormat() {
        String hash = PasswordUtil.hash(PASSWORD);
        String[] parts = hash.split("\\$", -1);

        assertEquals(4, parts.length);
        assertEquals("pbkdf2_sha256", parts[0]);
        assertEquals("600000", parts[1], "iterasi bawaan D-023");
        assertEquals(16, Base64.getDecoder().decode(parts[2]).length, "salt 16 byte");
        assertEquals(32, Base64.getDecoder().decode(parts[3]).length, "kunci turunan 256 bit");
    }

    @Test
    void hashNeverContainsRawPassword() {
        assertFalse(PasswordUtil.hash(PASSWORD).contains(PASSWORD));
    }

    @Test
    void samePasswordProducesDifferentHashesBecauseOfRandomSalt() {
        String first = PasswordUtil.hash(PASSWORD, 1000);
        String second = PasswordUtil.hash(PASSWORD, 1000);

        assertNotEquals(first, second);
        assertNotEquals(first.split("\\$")[2], second.split("\\$")[2], "salt harus berbeda");
        assertTrue(PasswordUtil.verify(PASSWORD, first));
        assertTrue(PasswordUtil.verify(PASSWORD, second));
    }

    @Test
    void hashRejectsNullAndEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
    }

    @Test
    void hashErrorMessageDoesNotLeakPassword() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.hash(PASSWORD, 0));
        assertFalse(e.getMessage().contains(PASSWORD));
    }

    // ------------------------------------------------------------------ verify: benar / salah

    @Test
    void verifyAcceptsCorrectPasswordWithDefaultIterations() {
        String hash = PasswordUtil.hash(PASSWORD);
        assertTrue(PasswordUtil.verify(PASSWORD, hash));
    }

    @Test
    void verifyRejectsWrongPassword() {
        String hash = PasswordUtil.hash(PASSWORD, 1000);

        assertFalse(PasswordUtil.verify("kata-sandi-salah", hash));
        assertFalse(PasswordUtil.verify(PASSWORD + " ", hash), "spasi di akhir termasuk password");
        assertFalse(PasswordUtil.verify(PASSWORD.toUpperCase(), hash), "case-sensitive");
    }

    @Test
    void verifyReturnsFalseForNullOrEmptyArguments() {
        String hash = PasswordUtil.hash(PASSWORD, 1000);

        assertFalse(PasswordUtil.verify(null, hash));
        assertFalse(PasswordUtil.verify("", hash));
        assertFalse(PasswordUtil.verify(PASSWORD, null));
        assertFalse(PasswordUtil.verify(PASSWORD, ""));
    }

    @Test
    void verifyReadsIterationCountFromStoredHash() {
        String hash1000 = PasswordUtil.hash(PASSWORD, 1000);
        String hash2500 = PasswordUtil.hash(PASSWORD, 2500);

        assertEquals("1000", hash1000.split("\\$")[1]);
        assertEquals("2500", hash2500.split("\\$")[1]);
        assertTrue(PasswordUtil.verify(PASSWORD, hash1000), "hash lama dengan iterasi berbeda tetap valid");
        assertTrue(PasswordUtil.verify(PASSWORD, hash2500));

        // Mengganti angka iterasi pada hash yang sama harus membuat verifikasi gagal (bukti iterasi benar-benar dipakai).
        String tampered = hash1000.replaceFirst("\\$1000\\$", "\\$1001\\$");
        assertFalse(PasswordUtil.verify(PASSWORD, tampered));
    }

    @Test
    void verifyIsCaseSensitiveAndHandlesUnicodePasswords() {
        String hash = PasswordUtil.hash(UNICODE_PASSWORD, 1000);

        assertTrue(PasswordUtil.verify(UNICODE_PASSWORD, hash));
        assertFalse(PasswordUtil.verify("uji-sandi-n-日本", hash));
    }

    // ------------------------------------------------------------------ verify: format rusak

    @Test
    void verifyRejectsMalformedHashesWithoutThrowing() {
        String valid = PasswordUtil.hash(PASSWORD, 1000);
        String[] p = valid.split("\\$");

        String[] malformed = {
                "teks-biasa-bukan-hash",
                "$",
                "$$$",
                "pbkdf2_sha256",
                "pbkdf2_sha256$1000",
                "pbkdf2_sha256$1000$" + p[2],
                valid + "$tambahan",                        // 5 bagian
                valid + "$",                                // 5 bagian (bagian kosong di akhir)
                "pbkdf2_sha512$1000$" + p[2] + "$" + p[3],  // label salah
                "PBKDF2_SHA256$1000$" + p[2] + "$" + p[3],  // label beda huruf besar
                "pbkdf2_sha256$abc$" + p[2] + "$" + p[3],   // iterasi bukan angka
                "pbkdf2_sha256$$" + p[2] + "$" + p[3],      // iterasi kosong
                "pbkdf2_sha256$0$" + p[2] + "$" + p[3],     // iterasi 0
                "pbkdf2_sha256$-5$" + p[2] + "$" + p[3],    // iterasi negatif
                "pbkdf2_sha256$2147483647$" + p[2] + "$" + p[3], // iterasi tak wajar (melebihi batas)
                "pbkdf2_sha256$99999999999$" + p[2] + "$" + p[3], // di luar rentang int
                "pbkdf2_sha256$1000$%%%bukan-base64%%%$" + p[3],  // salt bukan Base64
                "pbkdf2_sha256$1000$" + p[2] + "$%%%bukan-base64%%%",
                "pbkdf2_sha256$1000$$" + p[3],              // salt kosong
                "pbkdf2_sha256$1000$" + p[2] + "$",         // hash kosong
                "pbkdf2_sha256$1000$" + p[2] + "$" + Base64.getEncoder().encodeToString(new byte[8]), // hash terlalu pendek
                " " + valid,                                // spasi di depan
        };

        for (String bad : malformed) {
            assertDoesNotThrow(() -> PasswordUtil.verify(PASSWORD, bad), "tidak boleh melempar untuk: " + bad);
            assertFalse(PasswordUtil.verify(PASSWORD, bad), "harus false untuk: " + bad);
        }
    }

    @Test
    void verifyRejectsTamperedHashValue() {
        String valid = PasswordUtil.hash(PASSWORD, 1000);
        String[] p = valid.split("\\$");
        byte[] derived = Base64.getDecoder().decode(p[3]);
        derived[0] ^= 0x01; // balik satu bit
        String tampered = p[0] + "$" + p[1] + "$" + p[2] + "$" + Base64.getEncoder().encodeToString(derived);

        assertFalse(PasswordUtil.verify(PASSWORD, tampered));
    }

    // ------------------------------------------------------------------ kompatibilitas

    @Test
    void verifyMatchesIndependentPbkdf2Vectors() {
        // Vektor PBKDF2-HMAC-SHA256 (password="password", salt="salt", dkLen=32) dihitung terpisah
        // dengan hashlib Python (bukan dari PasswordUtil maupun Pbkdf2HashGenerator).
        assertTrue(PasswordUtil.verify("password", "pbkdf2_sha256$1$c2FsdA==$Eg+2z/z4syxD5yJSVsT4N6hlSMkszDVICAWYfLcL4Xs="));
        assertTrue(PasswordUtil.verify("password", "pbkdf2_sha256$2$c2FsdA==$rk0Mla9rRtMtCt/5KPBt0CowP47zwlHf1uLYWpVHTEM="));
        assertTrue(PasswordUtil.verify("password", "pbkdf2_sha256$4096$c2FsdA==$xeR41ZKIyEGqUw22hFxMjZYok6ABzk4RpJY4c6qYE0o="));
        assertFalse(PasswordUtil.verify("Password", "pbkdf2_sha256$4096$c2FsdA==$xeR41ZKIyEGqUw22hFxMjZYok6ABzk4RpJY4c6qYE0o="));
    }

    @Test
    void verifyAcceptsHashesCreatedByPbkdf2HashGenerator() {
        assertTrue(PasswordUtil.verify(PASSWORD, GENERATOR_VECTOR_DEFAULT));
        assertFalse(PasswordUtil.verify("kata-sandi-salah", GENERATOR_VECTOR_DEFAULT));
        assertTrue(PasswordUtil.verify(UNICODE_PASSWORD, GENERATOR_VECTOR_UNICODE));
    }

    /**
     * Arah sebaliknya: hash dari PasswordUtil diperiksa oleh {@code Pbkdf2HashGenerator --verify}
     * (proses java terpisah). Dilewati bila file/launcher java tidak tersedia.
     */
    @Test
    void generatorVerifyAcceptsHashesCreatedByPasswordUtil() throws Exception {
        Path tool = Path.of("database", "tools", "Pbkdf2HashGenerator.java");
        assumeTrue(Files.isRegularFile(tool), "database/tools/Pbkdf2HashGenerator.java tidak ada di working directory");
        String javaBin = ProcessHandle.current().info().command().orElse(null);
        assumeTrue(javaBin != null, "launcher java tidak dapat ditentukan");

        String hash = PasswordUtil.hash(PASSWORD, 1000);
        assertEquals(0, runGeneratorVerify(javaBin, tool, hash, PASSWORD), "password benar harus COCOK");
        assertEquals(1, runGeneratorVerify(javaBin, tool, hash, "kata-sandi-salah"), "password salah harus TIDAK COCOK");

        String unicodeHash = PasswordUtil.hash(UNICODE_PASSWORD, 1000);
        assertEquals(0, runGeneratorVerify(javaBin, tool, unicodeHash, UNICODE_PASSWORD));
    }

    private static int runGeneratorVerify(String javaBin, Path tool, String hash, String password)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(javaBin, tool.toString(), "--stdin", "--verify", hash)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        try (var out = process.getOutputStream()) {
            out.write((password + "\n").getBytes(StandardCharsets.UTF_8)); // password lewat stdin, bukan argumen
        }
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "utilitas generator tidak selesai tepat waktu");
        return process.exitValue();
    }
}
