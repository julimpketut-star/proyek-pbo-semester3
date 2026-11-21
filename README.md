# Sistem Pengelolaan Operasional Studio Musik

Aplikasi **desktop** (bukan website) untuk mengelola operasional satu studio musik: pelanggan, ruangan, alat musik, dan pemesanan (booking). Dibuat untuk **Tugas Kelompok II — Pemrograman Berorientasi Objek**, Program Studi Informatika, Universitas Udayana.

**Teknologi:** Java 21 · JavaFX 21 (FXML) · MySQL 8.0.16+ · Maven · JDBC · JUnit 5
**Arsitektur:** View → Controller → Service → DAO → MySQL

## Status
| Tahap | Isi | Status |
|---|---|---|
| STEP 0 | Spesifikasi final dan kontrak (`docs/`) | Selesai |
| STEP 1 | Scaffold Maven + starter JavaFX | **Selesai** — `mvn clean test` (5 tes) dan `mvn package` lulus di lokal (lihat `docs/STAGE1_VERIFICATION.md`) |
| STEP 2 | Skema, seed, reset, utilitas hash, konfigurasi DB | **Selesai** — 57/57 uji lulus di MySQL 8.0.46 sandbox; verifikasi di MySQL lokal masih dianjurkan (`docs/STAGE2_VERIFICATION.md` §7) |
| STEP 3 | Model dan Enum (`com.studiomusik.model`, `model.enums`) + 74 unit test baru | **Selesai di sandbox** — 79/79 tes lulus (javac + JUnit console); `mvn clean test` lokal menunggu Anda (`docs/STAGE3_VERIFICATION.md` §6) |
| STEP 4+ | Util JDBC, DAO, service, GUI | Belum dikerjakan |

Fitur bisnis (login, booking, CRUD) **belum ada**. Aplikasi saat ini hanya menampilkan jendela starter. Database dan class Model/Enum sudah siap; DAO dan Service dikerjakan pada tahap berikutnya.

## Prasyarat
1. **JDK 21** (mis. Temurin 21). Cek: `java -version` dan `javac -version` harus menunjukkan 21.
2. **Maven 3.9+** (`mvn -version`; Maven harus memakai JDK 21).
3. Koneksi internet untuk unduhan dependency pertama kali.
4. **MySQL Community Server 8.0.16+** (wajib mulai STEP 2; versi lebih lama tidak menegakkan constraint `CHECK`).
5. (Opsional) **Scene Builder** untuk mengedit FXML.

### Konfigurasi JDK 21
- Windows: set `JAVA_HOME` ke folder JDK 21 dan tambahkan `%JAVA_HOME%\bin` ke `PATH`.
- macOS/Linux: `export JAVA_HOME=/path/ke/jdk-21` dan `export PATH="$JAVA_HOME/bin:$PATH"`.
- Pastikan `mvn -version` menampilkan `Java version: 21.x`.

## Membuka di Visual Studio Code
1. Pasang ekstensi yang direkomendasikan (VS Code menawarkan dari `.vscode/extensions.json`): *Extension Pack for Java*, *Maven for Java*, *XML*.
2. `File → Open Folder…` → pilih folder `studio-musik/` (yang berisi `pom.xml`).
3. Tunggu proyek Java/Maven selesai diimpor. Pastikan JDK proyek = 21 (`Java: Configure Java Runtime`).

## Struktur proyek
```
studio-musik/
├── pom.xml
├── README.md  PROGRESS.md  PROGRESS_HANDOFF.md
├── config/db.properties.example      ← salin ke config/db.properties (jangan di-commit)
├── database/
│   ├── schema.sql  seed.sql  reset.sql
│   ├── tools/Pbkdf2HashGenerator.java    ← pembuat hash password (JDK saja)
│   └── tests/verify_stage2.sql           ← verifikasi otomatis database
├── docs/                             ← spesifikasi, kontrak, keputusan, acceptance criteria, bukti verifikasi
└── src/
    ├── main/java/com/studiomusik/
    │   ├── MainApp.java, AppInfo.java
    │   ├── model/ (Person, Customer, Admin, Room, Instrument, Booking, BookingInstrument)
    │   ├── model/enums/ (BookingStatus, RoomStatus, InstrumentStatus)
    │   ├── controller/  dao/  service/  util/   (dao, service, util masih kosong)
    ├── main/resources/com/studiomusik/{view,css}/
    └── test/java/com/studiomusik/
```

## Build, test, dan menjalankan aplikasi
Jalankan dari folder `studio-musik/`:
```bash
mvn clean            # bersihkan target/
mvn test             # jalankan unit test (JUnit 5)
mvn package          # build JAR di target/
mvn clean javafx:run # jalankan aplikasi JavaFX
```
Catatan: jalankan aplikasi dengan `mvn javafx:run` (JAR hasil `package` adalah JAR tipis dan tidak memuat runtime JavaFX).

## Database lokal (MySQL)

### 1. Instal MySQL
Pasang **MySQL Community Server 8.0.16 atau lebih baru** (Windows: *MySQL Installer*; macOS: Homebrew `brew install mysql`; Ubuntu: `sudo apt install mysql-server`). Catat password akun `root` (atau buat akun khusus aplikasi). Pastikan `mysql --version` berjalan dari terminal (di Windows tambahkan folder `bin` MySQL ke `PATH`).

### 2. Buat skema dan isi data awal
Dari root `studio-musik/`. **Windows PowerShell tidak mendukung operator `<`**, jadi gunakan `source` (berlaku di semua shell):
```powershell
mysql -u root -p -e "source database/schema.sql"   # membuat database studio_musik + 6 tabel
mysql -u root -p -e "source database/seed.sql"     # data demo (tabel harus masih kosong)
```
Di macOS/Linux/CMD juga bisa: `mysql -u root -p < database/schema.sql`.

- `schema.sql` aman dijalankan ulang (memakai `IF NOT EXISTS`, tidak menghapus data).
- `seed.sql` hanya untuk tabel kosong. Menjalankannya dua kali tanpa reset sengaja gagal dengan error duplikat agar data tidak ganda.

### 3. Reset database
```powershell
mysql -u root -p -e "source database/reset.sql"    # DESTRUKTIF: menghapus semua tabel dan data
mysql -u root -p -e "source database/schema.sql"
mysql -u root -p -e "source database/seed.sql"
```
> **Peringatan:** `reset.sql` menghapus seluruh data di database `studio_musik` dan tidak dapat dibatalkan.

### 4. Konfigurasi koneksi aplikasi
```powershell
Copy-Item config\db.properties.example config\db.properties     # Windows PowerShell
# cp config/db.properties.example config/db.properties           # macOS / Linux
```
Lalu isi `db.username` dan `db.password` dengan akun MySQL **lokal** Anda. URL bawaan: `jdbc:mysql://localhost:3306/studio_musik?...`. `config/db.properties` diabaikan Git; **jangan** menaruh credential asli di source code atau repository. (Kelas Java yang membaca file ini dibuat pada STEP 4.)

### 5. Data demo
| Peran | Identitas login | Catatan |
|---|---|---|
| Admin | username `admin` | Tabel `admin` |
| Customer | email `ayu.lestari@example.com`, `budi.santoso@example.com`, `eka.wijaya@example.com`, `sari.dewi@example.com`, `dharma.putra@example.com`, `tirta.yasa@example.com` | Tabel `customer` |

**Password demo tidak disimpan di repository** (database hanya menyimpan hash PBKDF2). Password demo disampaikan terpisah oleh tim. Untuk mengganti password demo, buat hash baru dan ganti nilai `password_hash` di `database/seed.sql`:
```bash
java database/tools/Pbkdf2HashGenerator.java            # minta password (tersembunyi), cetak hash
java database/tools/Pbkdf2HashGenerator.java --help     # opsi lain: --stdin, --iterations, --verify
```
Format hash: `pbkdf2_sha256$<iterasi>$<salt>$<hash>` (PBKDF2WithHmacSHA256, 600000 iterasi, salt 16 byte). Utilitas hanya memakai JDK; tidak perlu dependency tambahan.

### 6. Verifikasi database
```powershell
mysql -u root -p --table -e "source database/tests/verify_stage2.sql"
```
Hasil yang diharapkan: ringkasan akhir `LULUS | 57` tanpa baris `GAGAL`. Setelah itu jalankan lagi reset → schema → seed. Detail: `docs/STAGE2_VERIFICATION.md`.

### Skenario uji pada data seed
Studio A, 2026-12-01: booking 10:00–12:00 (CONFIRMED), 12:00–14:00 (PENDING, berurutan), dan booking CANCELLED pada rentang yang sama dengan yang aktif; Studio Rekaman 2026-12-05 berisi satu booking CANCELLED sendirian; ada Room dan Instrument berstatus MAINTENANCE serta Customer, Room, Instrument tanpa booking untuk menguji aturan hapus. Peta lengkap ada di komentar `database/seed.sql`.

## Dokumen
Mulai dari `docs/FINAL_SPECIFICATION.md`, lalu `docs/DECISION_LOG.md`. Kontrak teknis: `docs/DATABASE_CONTRACT.md`, `docs/ARCHITECTURE_CONTRACT.md`, `docs/API_CONTRACTS.md`. Kriteria penerimaan: `docs/ACCEPTANCE_CRITERIA.md`. Bukti verifikasi: `docs/STAGE1_VERIFICATION.md`, `docs/STAGE2_VERIFICATION.md`.

## Tim — Kelompok 1, Kelas D
I Gede Gaura Krishna · I Dewa Gede Putra Sugiatnyana · I Made Ari Andika Putra · Jire Immanuel Nababan · Ketut Juli Mahaputra
Dosen pengampu: I Made Satria Bimantara, S.Kom., M.Kom.
