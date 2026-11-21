# STAGE 4A VERIFICATION — Util, Security & Database Connection

Tanggal: 2026-11-21 · Workstream paralel STEP 4A (DAO dikerjakan worker 4B) · Branch pengguna: `feature/model`

## 1. Kesimpulan

| Item | Status |
|---|---|
| Gerbang sebelum STEP 4 (dilaporkan pengguna): `mvn clean test` lokal = 79 tes, 0 gagal, BUILD SUCCESS; verifikasi MySQL lokal = `LULUS \| 57` (MySQL 8.0.46) | Terpenuhi (angka dari pengguna; keluaran mentah tidak dilampirkan) |
| Lima util dibuat: `DatabaseConnection`, `PasswordUtil`, `SessionManager`, `DateTimeUtil`, `CurrencyUtil` | **Selesai** |
| `javac --release 21 -Xlint:all` (main + semua tes) | **Bersih** (exit 0, tanpa warning) |
| Tes (JUnit console di sandbox): 79 lama + 70 baru = **149 ditemukan, 147 berhasil, 0 gagal, 2 dilewati** (tes koneksi nyata tanpa `config/db.properties`) | **Lulus** |
| Tes yang sama dengan `config/db.properties` sementara → MySQL 8.0.46 sandbox lewat **driver MariaDB** | `DatabaseConnectionTest` **16/16 lulus, 0 dilewati** (bukan Connector/J; lihat bagian 5) |
| Uji mutasi pada util | **20 dari 20 mutan terdeteksi** |
| `mvn clean test` | **Tidak dapat dijalankan di sandbox** (Maven Central tidak terjangkau; `maven-clean-plugin:3.2.0` tidak terunduh). Wajib dijalankan lokal. **Tidak ada klaim build penuh lulus.** |
| Penggabungan dengan 4B (DAO) | **Belum**; 4A tidak menyentuh file DAO |

## 2. File (semua nyata; hanya cakupan 4A)

**Dibuat (main):** `src/main/java/com/studiomusik/util/` → `DatabaseConnection.java`, `PasswordUtil.java`, `SessionManager.java`, `DateTimeUtil.java`, `CurrencyUtil.java`.
**Diubah:** `src/main/java/com/studiomusik/util/package-info.java` (Javadoc layer).
**Dibuat (test):** `src/test/java/com/studiomusik/util/` → `DatabaseConnectionTest.java` (16), `PasswordUtilTest.java` (15), `SessionManagerTest.java` (12), `DateTimeUtilTest.java` (15), `CurrencyUtilTest.java` (12) = **70 tes**.
**Dibuat (dokumen):** `docs/STAGE4A_UTIL_VERIFICATION.md` (ini).
**Sengaja tidak disentuh:** `pom.xml`, `database/*`, model/enum, DAO, Service, FXML, Controller, `README.md`, `PROGRESS.md`, `PROGRESS_HANDOFF.md`, `docs/DECISION_LOG.md`, `docs/ACCEPTANCE_CRITERIA.md`, `docs/API_CONTRACTS.md` (file bersama; koordinator yang menggabungkan, lihat bagian 8).

## 3. API yang dibuat (signature sesuai `API_CONTRACTS.md` §2, tidak ada yang diubah)

```java
DatabaseConnection { public static Connection getConnection() throws SQLException; }
PasswordUtil       { public static String hash(String raw); public static boolean verify(String raw, String stored); }
SessionManager     { getInstance(); login(Person); logout(); getCurrentUser(); isLoggedIn(); isAdmin(); isCustomer(); getCurrentCustomer(); }
DateTimeUtil       { hoursBetween(LocalTime,LocalTime); isWholeHour(LocalTime); overlaps(s1,e1,s2,e2); formatDate(LocalDate); formatTime(LocalTime); }
CurrencyUtil       { public static String formatRupiah(BigDecimal); }
```
Hanya tambahan **package-private** untuk pengujian (bukan API aplikasi): `DatabaseConnection.getConnection(Path)`, `loadConfig(Path)`, `record Config`; `PasswordUtil.hash(String, int iterasi)`.

### Perilaku yang dipilih (kontrak diam; dicatat agar dapat dikoreksi koordinator)

| Util | Perilaku |
|---|---|
| `DatabaseConnection` | Baca `config/db.properties` relatif ke working directory (`java.util.Properties`, UTF-8). Wajib: `db.url` (diawali `jdbc:`), `db.username`, `db.password`; kunci hilang/kosong/`GANTI_DENGAN_...` → `SQLException` berpesan Indonesia yang menyebut nama kunci dan menunjuk ke `db.properties.example`. Setiap panggilan = koneksi baru (auto-commit aktif) yang ditutup pemanggil. Pesan error **tidak memuat password maupun URL**; `Config.toString()` tersamarkan; kegagalan koneksi dibungkus dengan pesan umum + `SQLState`/kode (cause asli dipertahankan untuk developer); "No suitable driver" (pesan aslinya memuat URL) dibungkus tanpa cause. **Password kosong ditolak** (sesuai instruksi "properti kosong"); MySQL lokal tanpa password harus diberi nilai atau aturan ini dilonggarkan lewat keputusan baru. |
| `PasswordUtil` | `pbkdf2_sha256$iter$saltB64$hashB64`, PBKDF2WithHmacSHA256, salt 16 byte `SecureRandom`, kunci 256 bit, iterasi bawaan 600000 (D-023); `verify` membaca iterasi dari string hash dan membandingkan dengan `MessageDigest.isEqual`. `hash(null/"")` → `IllegalArgumentException` (pesan tanpa password). `verify` → `false` (tidak pernah exception) untuk null/kosong, label salah, bagian ≠ 4 (termasuk bagian kosong di akhir), iterasi bukan angka/≤ 0, Base64 rusak, salt kosong, panjang hash ≠ 32 byte. **Tambahan pengaman:** iterasi > 10.000.000 ditolak (`false`) agar nilai rusak di database tidak membuat login menggantung; jauh di atas 600000, tidak memengaruhi hash dari `Pbkdf2HashGenerator` dengan opsi `--iterations` yang wajar. |
| `SessionManager` | Singleton eager, field `volatile`. `login(null)` → `NullPointerException`; subtipe `Person` selain `Admin`/`Customer` → `IllegalArgumentException`; `login` kedua menggantikan pengguna pertama; `logout` aman saat belum login; sesi sebelumnya tidak hilang bila `login` gagal. Hanya menyimpan referensi `Person` (tidak ada field/parameter password mentah; diuji lewat refleksi). |
| `DateTimeUtil` | `hoursBetween` = jam penuh `Duration.toHours()` (dibulatkan ke nol; **tidak memvalidasi urutan**: ≤ 0 bila `end ≤ start`) — Service wajib memeriksa `end > start` dan `isWholeHour` dulu. `overlaps` = `s1 < e2 && e1 > s2` persis (berurutan **bukan** overlap). `formatDate` = `dd/MM/yyyy`, `formatTime` = `HH:mm` (`Locale.ROOT`, tidak bergantung locale OS). Semua parameter null → `NullPointerException` (konsisten dengan D-029). |
| `CurrencyUtil` | `Rp 150.000`; nilai bulat tanpa desimal (termasuk skala 2 dari DB: `150000.00` → `Rp 150.000`); berpecahan → dua desimal dengan koma (`Rp 150.000,50`); pembulatan HALF_UP **hanya untuk tampilan** pada salinan; negatif `-Rp 1.000`; nilai yang membulat ke nol tidak pernah bertanda minus; spasi ASCII biasa; separator tidak bergantung locale. Input `BigDecimal` tidak diubah. null → `NullPointerException`. |

## 4. Hasil tes aktual

Lingkungan: sandbox Linux, OpenJDK 21.0.13, JUnit Jupiter 5.10.1 / platform console 1.9.1 dari paket apt `junit5` (**bukan** versi `pom.xml` 5.10.2 lewat Maven). Perintah: `javac --release 21 -Xlint:all` untuk `AppInfo`, model, util, lalu semua tes, kemudian `junit-platform-console-standalone` dengan `--scan-class-path`.

| Kelas tes | Ditemukan | Berhasil | Dilewati |
|---|---|---|---|
| `AppInfoTest` + 7 kelas tes model (STEP 1–3) | 79 | 79 | 0 |
| `PasswordUtilTest` | 15 | 15 | 0 |
| `SessionManagerTest` | 12 | 12 | 0 |
| `DateTimeUtilTest` | 15 | 15 | 0 |
| `CurrencyUtilTest` | 12 | 12 | 0 |
| `DatabaseConnectionTest` | 16 | 14 | **2** |
| **Total** | **149** | **147** | **2** (0 gagal) |

**Tes yang dilewati (by design, `Assumptions`):** `connectsToLocalDatabaseWhenConfigured` dan `everyCallReturnsAnIndependentConnectionWhenConfigured`, karena `config/db.properties` tidak ada di repository/sandbox. Keduanya juga dilewati (dengan alasan tercetak) bila file masih placeholder atau database tidak terjangkau, agar `mvn test` hijau di mesin tanpa database. Akibatnya: **bila Anda memiliki `config/db.properties` valid tetapi MySQL mati, tes ini terlewati, bukan gagal**; baca alasan skip pada keluaran Surefire.

**Cakupan perilaku:** hash/verify benar–salah; hash berbeda untuk password sama (salt acak); 22 pola hash rusak ditolak tanpa exception; iterasi dibaca dari hash (mengubah angka iterasi → gagal); 3 vektor PBKDF2-HMAC-SHA256 independen (dihitung dengan `hashlib` Python, bukan dari kode proyek); hash dari `Pbkdf2HashGenerator` (bawaan 600000 dan non-ASCII) diterima `PasswordUtil.verify`; hash dari `PasswordUtil` diterima `Pbkdf2HashGenerator --verify` (proses `java` terpisah, password via stdin, kedua arah COCOK/TIDAK COCOK sesuai exit code 0/1); password uji hanya string sementara, bukan password demo (D-024). Overlap: sebagian, mengandung, di dalam, identik, berurutan (kedua sisi), terpisah, sensitivitas 1 menit, simetri. Rupiah: bulat, skala 2, nol, pecahan, HALF_UP, negatif, nilai besar, nilai input tidak berubah, tidak bergantung `Locale.setDefault`.

## 5. Koneksi nyata ke MySQL (sandbox) — batasan penting

- MySQL **8.0.46** dijalankan di sandbox; `reset.sql` → `schema.sql` → `seed.sql` berhasil (booking = 7).
- **MySQL Connector/J 8.4.0 tidak tersedia** (Maven Central tidak terjangkau). Satu-satunya driver adalah `libmariadb-java 2.7.6` (URL `jdbc:mariadb://`) dan hanya mendukung akun `mysql_native_password`. Dengan driver itu dan file `config/db.properties` **sementara** (akun sandbox sekali pakai, file sudah dihapus dan tidak ada di ZIP), `DatabaseConnectionTest` menghasilkan **16/16 lulus, 0 dilewati**: koneksi terbuka, `isValid`, `SELECT 1`, auto-commit aktif, dua koneksi saling independen.
- Uji sandi salah (akun sama): pesan = *"Gagal terhubung ke database. Pastikan MySQL aktif, ... (SQLInvalidAuthorizationSpecException, SQLState=28000, kode=1045)"*; tidak memuat password maupun URL.
- Ini **hanya uji kewajaran jalur kode** `DatabaseConnection`. Ini **bukan** bukti bahwa Connector/J 8.4.0 bekerja dengan `db.url` templat (`serverTimezone=Asia/Makassar`, dst.) dan **bukan pengganti** `mvn clean test` lokal. Tidak ada akun MySQL pengguna yang dipakai.

## 6. Uji mutasi (20 mutan, semua terdeteksi)

`overlaps` memakai `<=`; `overlaps` tanpa syarat kedua; `isWholeHour` mengabaikan detik; `hoursBetween` memakai menit; format tanggal `MM/dd`; `verify` memakai iterasi tetap 600000; `verify` selalu `true`; salt tetap; salt 8 byte; iterasi bawaan 100000; `verify` tanpa cek label; Rupiah tanpa pemangkasan `,00`; Rupiah HALF_EVEN; negatif tanpa tanda; `isAdmin` benar bila login apa pun; `logout` tidak menghapus; `getCurrentCustomer` tanpa cek tipe; tanpa cek placeholder; tanpa cek kosong; URL bocor di pesan driver. Setiap mutan membuat ≥ 1 tes gagal.

## 7. Bukti kebersihan (AC-A02/A03 untuk layer util)

- Tidak ada `javafx.*` di `util/` (pencarian teks; satu-satunya kemunculan kata itu ada di Javadoc `package-info`).
- Satu-satunya impor `java.sql` ada di `DatabaseConnection` (`Connection`, `DriverManager`, `SQLException`); tidak ada SQL maupun `Statement` di util.
- Tidak ada credential di source/test/dokumen: pemindaian teks atas `src docs config database` tidak menemukan kata sandi akun sandbox; password di tes hanyalah string palsu (`sandi-palsu-untuk-uji-12345`, `kata-sandi-uji-sementara`). `config/db.properties` tidak dibuat di ZIP (sudah di-ignore Git).
- Tidak ada `System.out`/logger di util: tidak ada jalur yang mencetak password/URL.

## 8. Konfigurasi `config/db.properties` dan menjalankan tes (lokal)

```powershell
# dari root proyek studio-musik\
Copy-Item config\db.properties.example config\db.properties
# edit config\db.properties: isi db.username dan db.password dengan akun MySQL LOKAL Anda
```
```
db.url=jdbc:mysql://localhost:3306/studio_musik?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Makassar
db.username=<akun MySQL lokal>
db.password=<password akun itu>
```
- Database harus ada dan terisi: `reset.sql` → `schema.sql` → `seed.sql` (`docs/STAGE2_VERIFICATION.md`). `config/db.properties` di-ignore Git; jangan di-commit.
- Bila muncul "Public Key Retrieval is not allowed", tambahkan `&allowPublicKeyRetrieval=true` di akhir `db.url` (hanya lokal).
- Jalankan dari **root proyek** (working directory harus root; jalur file relatif): `mvn clean test`.
- **Hasil yang diharapkan setelah 4A saja:** 149 tes, 0 gagal, 0 error; skipped = 2 bila `db.properties` tidak ada/ DB mati, **0 bila konfigurasi valid dan MySQL aktif**. Angka ini belum dijalankan oleh Maven; mohon dilaporkan ke koordinator.

## 9. Keterbatasan dan hal yang belum dilakukan

1. `mvn clean test` belum dijalankan (sandbox tanpa Maven Central); semua angka tes berasal dari JUnit console 5.10.1 / platform 1.9.1, bukan Surefire 3.2.5 + JUnit 5.10.2.
2. Connector/J 8.4.0 belum pernah dijalankan pada `DatabaseConnection`.
3. Tes `generatorVerifyAcceptsHashesCreatedByPasswordUtil` menjalankan proses `java` memakai launcher JVM yang sama dan butuh working directory = root proyek; dilewati otomatis bila file `database/tools/Pbkdf2HashGenerator.java` tidak ditemukan. Pada Windows belum dicoba.
4. Tes mengubah `Locale.getDefault()` sementara (dipulihkan di `finally`); aman untuk Surefire bawaan (berurutan), tidak aman bila kelak tes dijalankan paralel.
5. Penggabungan dengan DAO (4B), `README.md`, `PROGRESS.md`, `PROGRESS_HANDOFF.md`, dan status AC belum dilakukan (bukan cakupan 4A).

## 10. Konflik dokumen dan usulan entri Decision Log (untuk koordinator)

**Konflik:** D-035 menempatkan `DateTimeUtil` di STEP 5 serta `CurrencyUtil` dan `SessionManager` di STEP 6–7, sedangkan instruksi workstream 4A (lebih baru) menugaskan kelimanya pada STEP 4A. Instruksi terbaru diikuti; `DECISION_LOG.md` **tidak diubah** oleh 4A agar tidak bentrok dengan 4B.

Usulan teks (nomor disesuaikan koordinator; nomor berikutnya setelah D-035 adalah D-036 bila 4B tidak memakainya):

> **D-036 — Seluruh util dikerjakan di STEP 4A (mengubah D-035)**
> - **Konteks:** D-035 membagi util per tahap; rencana paralel STEP 4 menugaskan kelima util kepada worker 4A.
> - **Keputusan:** `DatabaseConnection`, `PasswordUtil`, `SessionManager`, `DateTimeUtil`, `CurrencyUtil` diimplementasi di STEP 4A. STEP 5–7 hanya memakainya. Signature `API_CONTRACTS.md` §2 tidak berubah. Perilaku yang ditetapkan: `hoursBetween` tidak memvalidasi urutan (Service memvalidasi `end > start` dan jam bulat); parameter null → `NullPointerException`; format tanggal `dd/MM/yyyy`, jam `HH:mm`; Rupiah tanpa desimal untuk nilai bulat dan koma desimal untuk pecahan; `DatabaseConnection` menolak properti kosong/placeholder (termasuk password kosong) dan tidak pernah memuat password/URL di pesan; `PasswordUtil.verify` menolak iterasi > 10.000.000. `DECIDED`

## 11. Catatan handoff 4A untuk koordinator

- **Untuk 4B (DAO):** pakai `DatabaseConnection.getConnection()` untuk method tanpa parameter `Connection`; jangan membuat helper koneksi lain. Kegagalan koneksi/konfigurasi muncul sebagai `SQLException` berpesan Indonesia; Service tetap membungkusnya menjadi `ServiceException` dengan pesan umum (`API_CONTRACTS.md` §6).
- **Untuk STEP 5 (Service):** `AuthService` memakai `PasswordUtil.verify(raw, hash)` (tidak pernah melempar karena input) dan `PasswordUtil.hash` untuk pendaftaran/ubah password; validasi waktu memakai `DateTimeUtil.isWholeHour`, `hoursBetween`, `overlaps` setelah memeriksa `end > start` sendiri.
- **Untuk STEP 6–7:** `CurrencyUtil.formatRupiah`, `DateTimeUtil.formatDate/formatTime`, `SessionManager` siap pakai. `SessionManager.login` menyimpan referensi objek apa adanya; `Customer`/`Admin` dari DAO masih membawa `passwordHash`. Disarankan `AuthService` mengosongkan `passwordHash` (`setPasswordHash(null)`) pada objek sebelum `login` agar hash tidak bertahan di memori sesi. Ini **belum** diterapkan di 4A karena menyangkut kontrak Service.
- **Penggabungan:** file 4A tidak tumpang-tindih dengan DAO. Satu-satunya file bersama yang diubah 4A hanyalah `util/package-info.java` (milik util). Setelah gabung: perbarui `PROGRESS_HANDOFF.md` §1/§2/§6 (daftar file, D-036, jumlah tes 149 + tes DAO), `ACCEPTANCE_CRITERIA.md` (bukti AC-A02/A03 untuk util, bagian 7), dan jalankan `mvn clean test` lokal.
- **Yang masih terbuka dari handoff:** A-01/A-03/A-04/A-05 belum dikonfirmasi tim (default dokumen berlaku); screenshot starter dan konfirmasi tombol "Uji Interaksi".
