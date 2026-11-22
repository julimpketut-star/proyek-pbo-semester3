# Sistem Pengelolaan Operasional Studio Musik

Aplikasi **desktop JavaFX** (bukan website, bukan aplikasi web/mobile) untuk mengelola operasional satu studio musik: pelanggan, ruangan, alat musik, dan pemesanan (booking). Dibuat untuk **Tugas Kelompok II — Pemrograman Berorientasi Objek**, Program Studi Informatika, Universitas Udayana (Kelompok 1, Kelas D).

**Teknologi:** Java 21 · JavaFX 21.0.5 (FXML + CSS) · MySQL 8.0.16+ · JDBC (MySQL Connector/J 8.4.0) · Maven · JUnit 5.10.2
**Arsitektur:** View (FXML/CSS) → Controller → Service → DAO → MySQL

## Fitur

**Customer**
- Register dan login (identitas login: email).
- Menjelajahi landing, katalog ruangan (harga per jam) dan alat (harga sewa per unit per sesi), serta mencari jadwal kosong **tanpa login**.
- Membuat booking (wajib login/registrasi Customer): pilih tanggal, jam bulat, ruangan, dan alat + jumlah (opsional); melihat estimasi total biaya sebelum menyimpan; status awal `PENDING`.
- Melihat riwayat dan detail booking **miliknya sendiri**.

**Admin** (akun dari seed; tidak ada self-register)
- Login dan dashboard ringkasan (jumlah Customer, Ruangan, Instrumen, Booking per status).
- CRUD Customer, Ruangan (Room), dan Instrumen (Instrument).
- Melihat seluruh Booking, memfilter berdasarkan status, dan mengubah status (`PENDING→CONFIRMED/CANCELLED`, `CONFIRMED→COMPLETED/CANCELLED`).
- CRUD detail alat pada Booking (tabel penghubung many-to-many `booking_instrument`: tambah, lihat, ubah jumlah, hapus; total biaya dihitung ulang dalam transaksi yang sama).

**Aturan penting:** jadwal `PENDING`/`CONFIRMED` memblokir slot dan booking bertumpang tindih ditolak; Room/Instrument `MAINTENANCE` tidak dapat dipakai; harga alat disimpan sebagai snapshot saat ditambahkan; password disimpan sebagai hash PBKDF2; simpan booking + detail alat dalam satu transaksi. Daftar lengkap BR-01…BR-22: `docs/FINAL_SPECIFICATION.md`.

**Batasan yang diketahui:** tidak ada stok fisik alat (A-02: dua booking di jam sama dapat memesan alat yang sama); Customer tidak dapat membatalkan booking sendiri (A-03); tidak ada reset password, pembayaran, atau laporan/ekspor; satu studio saja; aplikasi hanya berjalan sebagai aplikasi desktop dengan akses langsung ke MySQL lokal.

## Status (kumulatif STEP 0–9)
| Tahap | Isi | Status |
|---|---|---|
| 0 | Spesifikasi & kontrak (`docs/`) | Selesai |
| 1 | Proyek Maven + starter JavaFX | Selesai (`mvn clean test` 5 tes dan `mvn package` lulus di lokal, `docs/STAGE1_VERIFICATION.md`) |
| 2 | Skema, seed, reset, utilitas hash | Selesai (57/57 uji di MySQL 8.0.46 sandbox, `docs/STAGE2_VERIFICATION.md`) |
| 3 | Model & Enum | Selesai (79 tes di sandbox, `docs/STAGE3_VERIFICATION.md`) |
| 4A/4B | Util JDBC + 6 DAO | Selesai (dilaporkan pengguna: `mvn clean test` 110 tes, 0 gagal, 0 error, 2 skip) |
| 5 | Service + exception | Kode selesai; **tes DB (65 tes) belum terbukti dijalankan** — lihat PROGRESS.md |
| 6 | View: 5 FXML + CSS | File selesai |
| 7 | Controller, Session, Navigasi | Kode selesai |
| 8 | Integrasi & perbaikan hasil uji tim | **Tidak ada catatan/bukti pada berkas dokumentasi**; kelompok melengkapi (lihat PROGRESS.md) |
| 9 | Dokumentasi & pengiriman | Dokumen selesai; screenshot, hasil uji, peer review, nama/NIM final diisi kelompok |

Klaim "berfungsi" hanya berlaku untuk hal yang punya bukti (hasil `mvn`, query, screenshot). Rincian bukti ada di `PROGRESS.md` dan `PROGRESS_HANDOFF.md`.

## Prasyarat
1. **JDK 21** (mis. Temurin 21). `java -version` dan `javac -version` harus menunjukkan 21.
2. **Maven 3.9+** yang memakai JDK 21 (`mvn -version` menampilkan `Java version: 21.x`).
3. **MySQL Community Server 8.0.16+** (versi lebih lama tidak menegakkan `CHECK`).
4. Koneksi internet untuk mengunduh dependency pertama kali.
5. (Opsional) Scene Builder untuk mengedit FXML; Visual Studio Code dengan *Extension Pack for Java*, *Maven for Java*, *XML* (rekomendasi di `.vscode/extensions.json`).

Konfigurasi JDK: Windows — set `JAVA_HOME` ke folder JDK 21 dan tambahkan `%JAVA_HOME%\bin` ke `PATH`; macOS/Linux — `export JAVA_HOME=/path/ke/jdk-21` dan `export PATH="$JAVA_HOME/bin:$PATH"`.

## Langkah menjalankan (ringkas)
Semua perintah dari folder `studio-musik/` (yang berisi `pom.xml`):
1. Buat database dan data demo (bagian *Database lokal* di bawah).
2. Salin konfigurasi: `cp config/db.properties.example config/db.properties` (PowerShell: `Copy-Item config\db.properties.example config\db.properties`), lalu isi `db.username` dan `db.password` dengan akun MySQL **lokal** Anda.
3. Jalankan aplikasi: `mvn clean javafx:run`.

Aplikasi membaca `config/db.properties` relatif terhadap working directory, jadi jalankan dari root proyek. Tanpa file ini aplikasi tidak dapat terhubung ke database.

## Database lokal (MySQL)

### 1. Instal MySQL
Pasang MySQL Community Server 8.0.16+ (Windows: MySQL Installer; macOS: `brew install mysql`; Ubuntu: `sudo apt install mysql-server`). Pastikan `mysql --version` berjalan di terminal.

### 2. Buat skema dan isi data awal
Dari root `studio-musik/`. PowerShell tidak mendukung operator `<`, jadi gunakan `source` (berlaku di semua shell):
```powershell
mysql -u root -p -e "source database/schema.sql"   # membuat database studio_musik + 6 tabel
mysql -u root -p -e "source database/seed.sql"     # data demo (tabel harus masih kosong)
```
macOS/Linux/CMD juga bisa: `mysql -u root -p < database/schema.sql`.
- `schema.sql` aman dijalankan ulang (`IF NOT EXISTS`, tidak menghapus data).
- `seed.sql` hanya untuk tabel kosong; menjalankannya dua kali tanpa reset sengaja gagal.

### 3. Reset database
```powershell
mysql -u root -p -e "source database/reset.sql"    # DESTRUKTIF: menghapus semua tabel dan data
mysql -u root -p -e "source database/schema.sql"
mysql -u root -p -e "source database/seed.sql"
```
> **Peringatan:** `reset.sql` menghapus seluruh data di database `studio_musik` dan tidak dapat dibatalkan.

### 4. Konfigurasi koneksi
`config/db.properties.example` adalah templat tanpa kredensial. `config/db.properties` (berisi kredensial lokal Anda) **diabaikan Git dan tidak boleh disertakan di ZIP/repository**. Bila muncul galat *Public Key Retrieval is not allowed* pada MySQL 8 lokal, tambahkan `&allowPublicKeyRetrieval=true` di akhir `db.url` (hanya untuk pengembangan lokal).

### 5. Data demo
| Peran | Identitas login | Tabel |
|---|---|---|
| Admin | username `admin` | `admin` |
| Customer | email `ayu.lestari@example.com`, `budi.santoso@example.com`, `eka.wijaya@example.com`, `sari.dewi@example.com`, `dharma.putra@example.com`, `tirta.yasa@example.com` | `customer` |

**Password demo tidak disimpan di repository** (database hanya menyimpan hash PBKDF2); password demo disampaikan terpisah oleh tim. Untuk membuat hash baru: `java database/tools/Pbkdf2HashGenerator.java` (opsi: `--help`, `--stdin`, `--iterations`, `--verify`), lalu ganti `password_hash` di `database/seed.sql`. Format: `pbkdf2_sha256$<iterasi>$<salt>$<hash>` (PBKDF2WithHmacSHA256, 600000 iterasi, salt 16 byte).

### 6. Verifikasi database
```powershell
mysql -u root -p --table -e "source database/tests/verify_stage2.sql"
```
Hasil yang diharapkan: ringkasan `LULUS | 57` tanpa baris `GAGAL`. Setelah itu jalankan lagi reset → schema → seed sebelum memakai aplikasi.

## Build dan tes
```bash
mvn clean            # bersihkan target/
mvn test             # unit test JUnit 5
mvn package          # JAR tipis di target/ (tidak memuat runtime JavaFX)
mvn clean javafx:run # jalankan aplikasi JavaFX
```
Tes yang membutuhkan database (DAO dan Service) memakai `config/db.properties` dan otomatis di-*skip* bila database/seed belum siap. Jalankan `reset → schema → seed` sebelum `mvn clean test`. Tes Service memakai data berawalan `svc-test-`/`SvcTest `/`svctest_` dan membersihkannya sendiri.

## Struktur proyek
```
studio-musik/
├── pom.xml
├── README.md  PROGRESS.md  PROGRESS_HANDOFF.md  .gitignore
├── config/db.properties.example      ← salin ke config/db.properties (jangan di-commit / jangan di-ZIP)
├── database/
│   ├── schema.sql  seed.sql  reset.sql
│   ├── tools/Pbkdf2HashGenerator.java    ← pembuat hash password (JDK saja)
│   └── tests/verify_stage2.sql           ← verifikasi otomatis database
├── docs/                             ← spesifikasi, kontrak, keputusan, acceptance criteria, bukti verifikasi,
│                                       FINAL_REPORT_CONTENT.md, FINAL_SCREENSHOT_CHECKLIST.md
└── src/
    ├── main/java/com/studiomusik/
    │   ├── MainApp.java, AppInfo.java
    │   ├── model/ (Person, Customer, Admin, Room, Instrument, Booking, BookingInstrument)
    │   ├── model/enums/ (BookingStatus, RoomStatus, InstrumentStatus)
    │   ├── dao/ (AdminDAO, CustomerDAO, RoomDAO, InstrumentDAO, BookingDAO, BookingInstrumentDAO)
    │   ├── service/ (AuthService, CustomerService, RoomService, InstrumentService, BookingService,
    │   │             ServiceException, ValidationException, BusinessRuleException, ServiceSupport)
    │   ├── controller/ (Landing, Login, Register, CustomerDashboard, AdminDashboard controller,
    │   │                Navigator, UiSupport; MainController = sisa starter, tidak dimuat lagi)
    │   └── util/ (DatabaseConnection, PasswordUtil, SessionManager, DateTimeUtil, CurrencyUtil)
    ├── main/resources/com/studiomusik/{view/*.fxml, css/app.css}
    └── test/java/com/studiomusik/  (model, dao, service, util)
```

## Dokumen
Mulai dari `docs/FINAL_SPECIFICATION.md`, lalu `docs/DECISION_LOG.md`. Kontrak teknis: `docs/DATABASE_CONTRACT.md`, `docs/ARCHITECTURE_CONTRACT.md`, `docs/API_CONTRACTS.md`, `docs/GUI_CONTRACT.md`. Kriteria penerimaan: `docs/ACCEPTANCE_CRITERIA.md`. Bukti verifikasi: `docs/STAGE*_VERIFICATION.md`. Bahan laporan akhir: `docs/FINAL_REPORT_CONTENT.md`; daftar tangkapan layar: `docs/FINAL_SCREENSHOT_CHECKLIST.md`.

## Membuat ZIP rilis yang bersih
**ZIP berisi:** `src/`, `database/`, `docs/`, `config/db.properties.example`, `pom.xml`, `README.md`, `PROGRESS.md`, `PROGRESS_HANDOFF.md`, `.gitignore`.
**ZIP tidak boleh berisi:** `.git/`, `target/`, `config/db.properties`, `.env*`, kredensial/password lokal, `.idea/`, `.vscode/` (kecuali `extensions.json`), `*.log`, `*.class`, `.DS_Store`/`Thumbs.db`, dan file sementara IDE.

Tugas meminta panduan menjalankan dalam **`README.txt`**: salin README sebelum mengemas (`cp README.md README.txt`; PowerShell: `Copy-Item README.md README.txt`) dan sertakan keduanya.

Dari **folder induk** `studio-musik/` (macOS/Linux/Git Bash/WSL):
```bash
cp studio-musik/README.md studio-musik/README.txt
zip -r studio-musik-final.zip studio-musik \
  -x "studio-musik/.git/*" "studio-musik/target/*" "studio-musik/config/db.properties" \
     "studio-musik/.env" "studio-musik/.env.*" "studio-musik/.idea/*" "studio-musik/*.iml" \
     "studio-musik/.vscode/settings.json" "*.log" "*.class" "*/.DS_Store" "*/Thumbs.db"
```
Windows PowerShell (tanpa `zip`): salin dulu ke folder bersih lalu kompres.
```powershell
$src = "studio-musik"; $dst = "rilis\studio-musik"
Remove-Item rilis -Recurse -Force -ErrorAction SilentlyContinue
robocopy $src $dst /E /XD .git target .idea /XF db.properties .env "*.log" "*.class" "*.iml" Thumbs.db .DS_Store
Copy-Item "$src\README.md" "$dst\README.txt"
Remove-Item "$dst\.vscode\settings.json" -ErrorAction SilentlyContinue
Compress-Archive -Path $dst -DestinationPath studio-musik-final.zip -Force
```
> `robocopy /XF db.properties` hanya mengecualikan berkas bernama `db.properties`; `db.properties.example` tetap ikut.

**Verifikasi ZIP sebelum dikumpulkan (wajib):**
```bash
unzip -l studio-musik-final.zip | grep -E "db\.properties$|\.env|/target/|/\.git/|\.log$" && echo "ADA BERKAS TERLARANG" || echo "bersih"
unzip -l studio-musik-final.zip | grep "db.properties.example"      # harus muncul
unzip -p studio-musik-final.zip studio-musik/config/db.properties.example | grep -i password   # harus berupa placeholder GANTI_DENGAN_...
```
Unggah ZIP dan laporan PDF ke OASE dan Turnitin sesuai petunjuk penugasan.
