import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utilitas mandiri untuk membuat dan memeriksa hash password PBKDF2 sesuai keputusan D-018.
 *
 * Format keluaran:  pbkdf2_sha256$<iterasi>$<salt Base64>$<hash Base64>
 *   - algoritma JCA : PBKDF2WithHmacSHA256
 *   - salt          : 16 byte acak dari SecureRandom
 *   - panjang kunci : 256 bit
 *   - iterasi       : bawaan 600000 (dapat diubah dengan --iterations)
 *
 * Hanya memakai JDK standar (tanpa dependency Maven). Password TIDAK pernah diterima lewat
 * argumen baris perintah (agar tidak tersimpan di riwayat shell) dan TIDAK disimpan di file mana pun.
 *
 * Cara pakai (dari root proyek studio-musik/, JDK 21):
 *   java database/tools/Pbkdf2HashGenerator.java                      -> minta password (input tersembunyi), cetak 1 hash
 *   java database/tools/Pbkdf2HashGenerator.java --iterations 310000  -> ubah jumlah iterasi
 *   java database/tools/Pbkdf2HashGenerator.java --stdin              -> baca password dari stdin, satu per baris, cetak satu hash per baris
 *   java database/tools/Pbkdf2HashGenerator.java --verify "<hash>"    -> periksa password terhadap hash (exit 0 = cocok, 1 = tidak)
 *   java database/tools/Pbkdf2HashGenerator.java --help
 */
public final class Pbkdf2HashGenerator {

    private static final String LABEL = "pbkdf2_sha256";
    private static final String JCA_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private Pbkdf2HashGenerator() {
    }

    public static void main(String[] args) throws Exception {
        int iterations = DEFAULT_ITERATIONS;
        boolean fromStdin = false;
        String verifyHash = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                case "--stdin" -> fromStdin = true;
                case "--iterations" -> {
                    if (i + 1 >= args.length) {
                        fail("Nilai --iterations belum diberikan.");
                    }
                    try {
                        iterations = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        fail("Nilai --iterations harus bilangan bulat.");
                    }
                    if (iterations < 1) {
                        fail("Nilai --iterations harus >= 1.");
                    }
                }
                case "--verify" -> {
                    if (i + 1 >= args.length) {
                        fail("Hash untuk --verify belum diberikan.");
                    }
                    verifyHash = args[++i];
                }
                default -> fail("Argumen tidak dikenal: " + args[i] + " (gunakan --help).");
            }
        }

        if (verifyHash != null) {
            char[] password = readOnePassword(fromStdin);
            boolean ok = verify(password, verifyHash);
            java.util.Arrays.fill(password, '\0');
            System.out.println(ok ? "COCOK" : "TIDAK COCOK");
            System.exit(ok ? 0 : 1);
        }

        if (fromStdin) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                char[] password = line.toCharArray();
                System.out.println(hash(password, iterations));
                java.util.Arrays.fill(password, '\0');
            }
        } else {
            char[] password = readOnePassword(false);
            System.out.println(hash(password, iterations));
            java.util.Arrays.fill(password, '\0');
        }
    }

    /** Membuat hash baru dengan salt acak. */
    static String hash(char[] password, int iterations) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] derived = derive(password, salt, iterations);
        return LABEL + "$" + iterations + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    /** Memeriksa password terhadap hash tersimpan (perbandingan waktu-konstan). */
    static boolean verify(char[] password, String stored) throws GeneralSecurityException {
        String[] parts = stored.split("\\$");
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
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] actual = derive(password, salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(JCA_ALGORITHM).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static char[] readOnePassword(boolean fromStdin) throws IOException {
        if (!fromStdin) {
            Console console = System.console();
            if (console != null) {
                char[] password = console.readPassword("Password: ");
                if (password == null || password.length == 0) {
                    fail("Password kosong.");
                }
                return password;
            }
            fail("Konsol interaktif tidak tersedia. Gunakan --stdin (contoh: echo ... | java ... --stdin).");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line = reader.readLine();
        if (line == null || line.isEmpty()) {
            fail("Password kosong.");
        }
        return line.toCharArray();
    }

    private static void fail(String message) {
        System.err.println("Error: " + message);
        System.exit(2);
    }

    private static void printHelp() {
        System.out.println("""
                Pbkdf2HashGenerator - pembuat hash password PBKDF2 (D-018)

                Pemakaian:
                  java database/tools/Pbkdf2HashGenerator.java [opsi]

                Opsi:
                  (tanpa opsi)          minta password secara tersembunyi, cetak satu hash
                  --stdin               baca password dari stdin (satu per baris), cetak satu hash per baris
                  --iterations N        jumlah iterasi (bawaan %d)
                  --verify "<hash>"     periksa password terhadap hash; exit 0 = cocok, 1 = tidak
                  --help                tampilkan bantuan

                Format: pbkdf2_sha256$<iterasi>$<salt Base64>$<hash Base64>
                Password tidak pernah diterima lewat argumen dan tidak disimpan.
                """.formatted(DEFAULT_ITERATIONS));
    }
}
