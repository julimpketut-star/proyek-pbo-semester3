# PROGRESS_HANDOFF — Sistem Pengelolaan Operasional Studio Musik (PBO TK II)

> Satu-satunya dokumen handoff lintas akun Claude. Diperbarui setiap akhir STEP. Jangan membuat file handoff lain.
> Terakhir diperbarui: 2026-11-19, akhir STEP 3.


## 1. Status
- **STEP 0** — selesai (dokumen kontrak, pemeriksaan konsistensi).
- **STEP 1** — **selesai**. Bukti lokal pengguna 2026-11-19: `mvn clean test` (5 tes, 0 gagal, BUILD SUCCESS) dan `mvn package` (BUILD SUCCESS, JAR terbentuk). `javafx:run` dilaporkan berhasil (tanpa screenshot); tombol "Uji Interaksi" belum dikonfirmasi. Branch Git pengguna: `feature/database`.
- **STEP 2** — **selesai, diverifikasi di sandbox** (MySQL 8.0.46): 57/57 uji lulus, siklus reset → schema → seed lulus. **Belum diverifikasi di MySQL lokal pengguna** (Windows); wajib sebelum STEP 4.
- **STEP 3** — **selesai di sandbox**: 7 class model + 3 enum; `javac -Xlint:all` bersih; **79/79 tes lulus** lewat JUnit console (74 baru + 5 lama); uji mutasi 12 dari 13 terdeteksi (1 mutan setara). **`mvn clean test` belum berhasil dijalankan di sandbox** (Maven tidak dapat mengunduh dependency) dan **belum dijalankan lokal**; placeholder hasil lokal pada prompt STEP 3 tidak diisi pengguna.
- **Gerbang sebelum STEP 4:** (1) `mvn clean test` lokal = 79 tes, 0 gagal; (2) verifikasi MySQL lokal = `LULUS | 57`.
- **Tahap berikutnya:** STEP 4 — Util JDBC & DAO.

## 2. File proyek yang benar-benar ada (root `studio-musik/`)
```
pom.xml
README.md  PROGRESS.md  PROGRESS_HANDOFF.md  .gitignore
.vscode/extensions.json
config/db.properties.example
database/schema.sql  seed.sql  reset.sql
database/tools/Pbkdf2HashGenerator.java
database/tests/verify_stage2.sql
docs/FINAL_SPECIFICATION.md      docs/DECISION_LOG.md (D-001…D-035)
docs/DATABASE_CONTRACT.md        docs/ARCHITECTURE_CONTRACT.md
docs/API_CONTRACTS.md (+§1.4)    docs/ACCEPTANCE_CRITERIA.md (+status AC-M, tabel 7 konsep PBO)
docs/PROJECT_HANDOFF_PBO.md      docs/STAGE1_VERIFICATION.md   docs/STAGE2_VERIFICATION.md   docs/STAGE3_VERIFICATION.md
src/main/java/com/studiomusik/MainApp.java
src/main/java/com/studiomusik/AppInfo.java
src/main/java/com/studiomusik/controller/MainController.java
src/main/java/com/studiomusik/model/Person.java  Customer.java  Admin.java  Room.java  Instrument.java  BookingInstrument.java  Booking.java  package-info.java
src/main/java/com/studiomusik/model/enums/BookingStatus.java  RoomStatus.java  InstrumentStatus.java  package-info.java
src/main/java/com/studiomusik/{dao,service,util}/package-info.java   (hanya Javadoc layer)
src/main/resources/com/studiomusik/view/main-view.fxml
src/main/resources/com/studiomusik/css/app.css
src/test/java/com/studiomusik/AppInfoTest.java
src/test/java/com/studiomusik/model/Fixtures.java  PersonHierarchyTest.java  RoomTest.java  InstrumentTest.java
                                    BookingInstrumentTest.java  BookingTest.java  OopConceptsTest.java
src/test/java/com/studiomusik/model/enums/BookingStatusTest.java
```
**Belum ada** (jangan dianggap tersedia): `DatabaseConnection`, `PasswordUtil`, `DateTimeUtil`, `CurrencyUtil`, `SessionManager`, DAO, Service (+ exception), controller selain `MainController`, FXML selain `main-view.fxml`, `config/db.properties` asli (lokal, di-ignore Git), screenshot, laporan akhir.

**File STEP 3 — dibuat:** 10 file main di `model/` dan `model/enums/`, 8 file test (74 tes baru), `docs/STAGE3_VERIFICATION.md`. **Diubah:** dua `package-info.java` model, `docs/DECISION_LOG.md`, `docs/API_CONTRACTS.md`, `docs/ACCEPTANCE_CRITERIA.md`, `README.md`, `PROGRESS.md`, `PROGRESS_HANDOFF.md`. **Tidak diubah:** `pom.xml`, `database/*.sql`, kode STEP 1, `AppInfoTest`.

## 3. Kontrak teknis yang wajib dipertahankan
(Rincian lengkap: `docs/*`. Perubahan hanya lewat entri baru di `docs/DECISION_LOG.md`.)

**Proyek:** Java 21 · Maven `com.studiomusik:studio-musik-app:1.0.0-SNAPSHOT` · main class `com.studiomusik.MainApp` · non-modular (tanpa `module-info.java`) · UTF-8 · database `studio_musik` (MySQL 8.0.16+, InnoDB, utf8mb4) · config `config/db.properties` (di-ignore Git; templat `config/db.properties.example`, URL `jdbc:mysql://localhost:3306/studio_musik?...`).

**Dependency (terbukti terunduh di build lokal 2026-11-19):** javafx-controls & javafx-fxml **21.0.5** · mysql-connector-j **8.4.0** (runtime) · junit-jupiter **5.10.2** (test) · maven-compiler-plugin 3.13.0 · maven-surefire-plugin 3.2.5 · javafx-maven-plugin 0.0.8.

**Package:** `com.studiomusik` → `model`, `model.enums`, `dao`, `service`, `controller`, `util`. Resource: `src/main/resources/com/studiomusik/{view,css}`. Test: `src/test/java/com/studiomusik`.

**Class (SUDAH diimplementasi di STEP 3; kontrak dikunci):** `Person` (abstract, `id,name,phone,email`, `abstract getRole()`), `Customer` (+`passwordHash`, role "Customer"), `Admin` (+`username`,`passwordHash`, phone/email null, role "Admin"), `Room(roomId,roomName,roomType,pricePerHour,status)`, `Instrument(instrumentId,name,type,rentalPrice,status)`, `BookingInstrument(bookingInstrumentId,bookingId,instrument,quantity,rentalPrice)`, `Booking(bookingId,customer,room,bookingDate,startTime,endTime,status,totalCost,createdAt,instruments)`. Uang = `BigDecimal`. Enum: `BookingStatus{PENDING,CONFIRMED,COMPLETED,CANCELLED}` (+`blocksSchedule()`, `canTransitionTo()`), `RoomStatus{AVAILABLE,MAINTENANCE}`, `InstrumentStatus{AVAILABLE,MAINTENANCE}`. Signature lengkap: `docs/API_CONTRACTS.md` §1 dan §1.4 (perilaku/konstruktor, D-029…D-034).

**Perilaku model yang harus dipertahankan (STEP 3):** konstruktor lengkap untuk DAO pada `Room`, `Instrument`, `BookingInstrument`, `Booking`; `Booking(Customer, Room, date, start, end)` untuk booking baru (id 0, PENDING, total `0.00`); `id = 0` = belum disimpan · `BookingInstrument.rentalPrice` dan `.instrument` adalah `final` tanpa setter (snapshot BR-12), hanya `quantity` (≥ 1), `bookingInstrumentId`, `bookingId` yang bisa diubah · `Booking.getInstruments()` baca-saja; `setInstruments(List)` menyalin; `setBookingId` menyebar ke detail · `calculateDuration()` jam bulat, `IllegalStateException` bila waktu kosong/`end <= start`/menit ≠ 00 · `calculateTotalCost()` murni (tidak mengubah `totalCost`), pembulatan per komponen HALF_UP skala 2 · `addInstrument` menolak null, `quantity < 1`, alat MAINTENANCE, dan `instrumentId` duplikat (`IllegalArgumentException`); `removeInstrument` menolak id yang tidak ada · `BookingStatus.canTransitionTo` (status sama/`null` → `false`) · pelanggaran invariant: `IllegalArgumentException` / `IllegalStateException` / `NullPointerException` (Service yang menerjemahkan ke `ValidationException`/`BusinessRuleException`) · tidak ada `equals/hashCode/toString` (agar hash tidak bocor lewat `toString`).

**Pembagian util:** STEP 4 = `DatabaseConnection`, `PasswordUtil`, 6 DAO · STEP 5 = `DateTimeUtil` + Service · STEP 6–7 = `CurrencyUtil`, `SessionManager` (D-035).

**Tabel (6, SUDAH ada di database/schema.sql, sesuai kontrak tanpa perubahan):**
`admin(admin_id, username UNIQUE, password_hash, name)` · `customer(customer_id, name, phone, email UNIQUE NOT NULL, password_hash)` · `room(room_id, name, type, price_per_hour, status)` · `instrument(instrument_id, name, type, rental_price, status)` · `booking(booking_id, customer_id FK RESTRICT, room_id FK RESTRICT, booking_date, start_time, end_time, status, total_cost, created_at)` · `booking_instrument(booking_instrument_id, booking_id FK CASCADE, instrument_id FK RESTRICT, quantity, rental_price, UNIQUE(booking_id,instrument_id))`.
Constraint bernama: `pk_*`, `uq_admin_username`, `uq_customer_email`, `uq_booking_instrument_pair`, `fk_booking_customer`, `fk_booking_room`, `fk_bi_booking`, `fk_bi_instrument`, `chk_room_price`, `chk_instrument_price`, `chk_booking_time`, `chk_booking_total`, `chk_bi_quantity`, `chk_bi_price`, indeks `idx_booking_room_date_status(room_id, booking_date, status)`. Kolom uang `DECIMAL(10,2)`.

**Method signature DAO/Service/Util:** `docs/API_CONTRACTS.md` §2–4 (termasuk CRUD `BookingInstrumentDAO` insert/insertAll/findByBookingId/updateQuantity/delete dan `BookingService.addInstrumentToBooking/getBookingInstruments/updateBookingInstrumentQuantity/removeInstrumentFromBooking`). Error: DAO `SQLException` → Service `ServiceException` (+`ValidationException`, `BusinessRuleException`, di package `service`).

**Password:** `pbkdf2_sha256$<iterasi>$<saltBase64>$<hashBase64>`; PBKDF2WithHmacSHA256; salt 16 byte `SecureRandom`; kunci 256 bit; iterasi bawaan **600000** (D-023); `PasswordUtil` (STEP 4) wajib kompatibel dengan `database/tools/Pbkdf2HashGenerator.java` dan membaca iterasi dari string hash. Verifikasi memakai perbandingan waktu-konstan.

**Aturan bisnis (ringkas):** PENDING & CONFIRMED memblokir jadwal; CANCELLED & COMPLETED tidak · overlap `existingStart < requestedEnd AND existingEnd > requestedStart` (berurutan boleh) · `end > start`, durasi jam bulat (A-05) · Room/Instrument MAINTENANCE tidak bisa dipakai · biaya ruangan = harga/jam × durasi; alat = quantity × harga per unit per sesi; total `BigDecimal` HALF_UP skala 2 · harga alat di-snapshot di `booking_instrument.rental_price` · booking baru PENDING; transisi PENDING→CONFIRMED/CANCELLED, CONFIRMED→COMPLETED/CANCELLED · satu transaksi untuk booking + detail alat; kunci baris room (`SELECT ... FOR UPDATE`) sebelum cek overlap · Customer hanya akses booking sendiri · tanggal lampau ditolak (BR-21) · tidak ada hapus booking permanen · hapus Room/Instrument/Customer yang dipakai booking ditolak (BR-22).

**Data seed (kontrak data uji; ID eksplisit):** admin 1 (`admin`); customer 1–6 (email `*@example.com`, customer 6 tanpa booking); room 1–5 (Studio A 150000, Studio B 120000, Studio C 80000, Studio Rekaman 250000, Studio D 100000 MAINTENANCE tanpa booking); instrument 1–7 (instrument 7 MAINTENANCE tanpa booking; instrument 5 harga sekarang 10000); booking 1–7 dan booking_instrument 1–12 dengan skenario: booking 1 (A, 2026-12-01 10–12 CONFIRMED), 2 (12–14 PENDING, berurutan), 3 (10–12 CANCELLED), 4 (A, 2026-11-10 10–13 COMPLETED, snapshot alat 8000), 5 (Studio B 2026-12-02 15–18 PENDING, 4 alat), 6 (Studio C 2026-12-03 09–10 CONFIRMED), 7 (Studio Rekaman 2026-12-05 13–17 CANCELLED). Semua `total_cost` konsisten dengan rumus (diuji B06). **Password demo tidak ada di repository** (D-024); Admin login `admin`, Customer login memakai email; password disampaikan terpisah oleh tim.

**Penomoran tahap:** 0 Spesifikasi · 1 Maven · 2 Database Schema & Seed · 3 Model & enum · 4 Util JDBC & DAO · 5 Service · 6 FXML & CSS · 7 Controller & navigasi · 8 Integrasi & pengujian · 9 README, screenshot, laporan, ZIP.


## 4. Keputusan penting (ringkas; lengkap di Decision Log D-001…D-035)
Dua aktor (Customer + Admin); `password_hash`; `customer.email` unik sebagai login; `BigDecimal` menggantikan `double`; method Class Diagram yang butuh DB direalisasikan di Service, bukan model (D-013); resource/config mengikuti master prompt (D-015); exception service di package `service` (D-017); PBKDF2 600000 iterasi tanpa dependency baru (D-018, D-023); kredensial demo tidak di repository (D-024); `schema.sql` idempotent, `seed.sql` hanya untuk tabel kosong, `reset.sql` hanya drop tabel (D-025); skrip verifikasi tambahan (D-026); tanggal seed tetap (D-027); **STEP 3:** konvensi exception model (D-029), pertahanan berlapis `addInstrument` (D-030), snapshot harga final (D-031), perilaku durasi/total (D-032), komposisi di memori (D-033), konstruktor tambahan tanpa `equals/hashCode/toString` (D-034), pembagian util per tahap (D-035).

## 5. Terbuka / perlu konfirmasi tim
- **A-01** tarif alat per unit per sesi (ditetapkan master prompt, belum dikonfirmasi tim). **A-02** tidak ada stok alat (keterbatasan diterima; tulis di laporan). **A-03** pembatalan oleh Customer: tidak ada di v1. **A-04** tabel transisi status (diusulkan; sudah ditanam di `BookingStatus`). **A-05** durasi jam bulat (diusulkan; sudah ditanam di `Booking.calculateDuration`). Bila tim mengubah A-04/A-05, ubah enum/model dan tes melalui entri Decision Log baru.
- `mvn clean test` lokal (79 tes) dan verifikasi MySQL lokal Windows (wajib sebelum STEP 4).
- Screenshot starter dan konfirmasi tombol "Uji Interaksi" (lampiran laporan).

## 6. Hasil verifikasi aktual
| Item | Hasil |
|---|---|
| STEP 1 lokal (screenshot pengguna) | `mvn clean test`: 5 tes, 0 gagal (26.342 s); `mvn package`: BUILD SUCCESS, JAR terbentuk (21.224 s); `javafx:run` dilaporkan berhasil (tanpa bukti); tombol belum dikonfirmasi |
| STEP 2 sandbox | MySQL `8.0.46-0ubuntu0.24.04.5`, mode strict. `verify_stage2.sql`: **57 LULUS, 0 GAGAL**; siklus reset → schema → seed lulus; AC-D01…D05 lulus (sandbox) |
| STEP 3 `mvn clean test` di sandbox | **GAGAL karena jaringan** (`maven-clean-plugin:2.5` tidak dapat diunduh). Tidak ada hasil Maven |
| STEP 3 javac | `javac --release 21 -Xlint:all` untuk model + enum + semua tes: exit 0, tanpa warning |
| STEP 3 tes (JUnit console 5.10.1, bukan Maven) | **79 ditemukan, 79 berhasil, 0 gagal/dilewati/dibatalkan** (AppInfoTest 5, PersonHierarchyTest 8, BookingStatusTest 6, RoomTest 8, InstrumentTest 7, BookingInstrumentTest 8, BookingTest 28, OopConceptsTest 9) |
| STEP 3 uji mutasi | 12 dari 13 mutasi membuat tes gagal; 1 mutan setara (penghapusan cek `quantity < 1` di `addInstrument`; konstruktor `BookingInstrument` menegakkan aturan sama) |
| STEP 3 kemurnian model | Tidak ada `java.sql`/`javafx`/`jdbc`/`java.io`/`double`/`float` di package model; `javac` menolak `new Person(...)` |
| `pom.xml`, `schema.sql`, `seed.sql`, `reset.sql` | Identik dengan ZIP STEP 2 |
| AC-M01…AC-M05 | Lulus di sandbox; menunggu konfirmasi Maven lokal |
| Belum diuji | `mvn clean test` lokal (79 tes); MySQL lokal Windows; koneksi JDBC dari Java; DAO/Service |

## 7. Error / pekerjaan belum selesai
1. `mvn clean test` lokal (79 tes). 2. Verifikasi MySQL lokal. 3. Screenshot starter + konfirmasi tombol. 4. Konfirmasi A-01/A-03/A-04/A-05. 5. Seluruh STEP 4–9. 6. Pembaruan Use Case/Flow di laporan akhir sesuai D-001 dan D-013.
Masalah yang ditemukan dan diperbaiki selama STEP 3: nama `TestData` cocok dengan pola `Test*.java` Surefire sehingga diganti `Fixtures`; tidak ada bug pada kode produksi yang ditemukan oleh tes.

## 8. Prompt lengkap untuk STEP 4 (Util JDBC & DAO)

Salin ke percakapan baru. Lampirkan: `PROGRESS_HANDOFF.md` dan ZIP proyek `studio-musik/` terbaru. Isi placeholder hasil lokal sebelum mengirim.

```
Anda adalah Software Architect dan Java Developer untuk proyek kuliah PBO "Sistem Pengelolaan Operasional Studio Musik" (Java 21, JavaFX, Maven, JDBC, MySQL 8.0+). Lanjutkan di workspace yang sudah ada; jangan membuat proyek baru atau mengubah struktur.

1. AUDIT WAJIB (sebelum membuat file)
- Baca PROGRESS_HANDOFF.md, README.md, PROGRESS.md, lalu docs/FINAL_SPECIFICATION.md, DECISION_LOG.md (D-001…D-035), ARCHITECTURE_CONTRACT.md, API_CONTRACTS.md (§1, §1.4, §2, §3), DATABASE_CONTRACT.md, ACCEPTANCE_CRITERIA.md.
- Periksa file aktual: model/enums, 7 class model, tes model, database/*.sql, config/db.properties.example. Jangan menganggap file ada hanya karena disebut handoff.
- Hasil lokal saya (WAJIB diisi; bila kosong, anggap BELUM dilakukan):
  - mvn clean test (harus 79 tes, 0 gagal): [TEMPEL]
  - Verifikasi MySQL lokal (docs/STAGE2_VERIFICATION.md §7, harus LULUS | 57): [TEMPEL]
  - Versi MySQL lokal: [..]; branch aktif: [..]
- Jawaban tim: A-01 [..], A-03 [..], A-04 [..], A-05 [..] (kosong = default dokumen).
- GERBANG: bila mvn clean test gagal ATAU verifikasi MySQL lokal gagal/belum dilakukan, JANGAN lanjut membuat DAO; bantu perbaiki dulu (verifikasi MySQL lokal bersifat wajib untuk tahap ini).
- Jika ada konflik dokumen vs file aktual, laporkan lebih dulu dan catat entri BARU di DECISION_LOG.md (lanjutkan nomor setelah D-035).

2. KONTRAK YANG TIDAK BOLEH DIUBAH
Package com.studiomusik (dao, util); nama class/method/signature pada API_CONTRACTS.md §2 (hanya DatabaseConnection dan PasswordUtil pada tahap ini) dan §3 (6 DAO); enam tabel dan kolom pada database/schema.sql; uang = BigDecimal (DECIMAL(10,2)); Java 21; non-modular; tanpa dependency baru; class model dan enum dari STEP 3 (jangan diubah tanpa alasan tercatat; bila DAO membutuhkan perubahan model, laporkan dulu).

3. TUGAS STEP 4 — buat file nyata
A. util/DatabaseConnection: static Connection getConnection() throws SQLException; membaca config/db.properties relatif terhadap working directory (root proyek) — kunci db.url, db.username, db.password; pesan error jelas bila file tidak ada/kunci kosong/masih placeholder (GANTI_DENGAN_...); tidak menyimpan credential di source; tidak mencatat password ke log; memakai MySQL Connector/J dari classpath runtime.
B. util/PasswordUtil: hash(String) dan verify(String, String) — format pbkdf2_sha256$iterasi$saltBase64$hashBase64, PBKDF2WithHmacSHA256, salt 16 byte SecureRandom, kunci 256 bit, iterasi bawaan 600000 (D-023), membaca iterasi dari string hash saat verify, perbandingan waktu-konstan (MessageDigest.isEqual), verify mengembalikan false (bukan exception) untuk format rusak. WAJIB kompatibel dua arah dengan database/tools/Pbkdf2HashGenerator.java (uji dengan vektor yang dibuat utilitas itu memakai password uji sementara — bukan password demo — dan uji bahwa hash baru dapat diverifikasi oleh `--verify`).
C. Enam DAO sesuai API_CONTRACTS.md §3 (AdminDAO, CustomerDAO, RoomDAO, InstrumentDAO, BookingDAO, BookingInstrumentDAO), semua method persis seperti kontrak:
   - JDBC dengan PreparedStatement saja (tanpa penyambungan string SQL dari input) dan try-with-resources untuk Connection/Statement/ResultSet.
   - Method tanpa parameter Connection membuka/menutup koneksinya sendiri. Method dengan Connection dipakai dalam transaksi Service: tidak boleh commit/rollback/close koneksi itu dan tidak mengubah autoCommit.
   - Pemetaan: ENUM database ↔ enum Java via name()/valueOf; DECIMAL ↔ BigDecimal (getBigDecimal, jangan double); DATE/TIME/DATETIME ↔ LocalDate/LocalTime/LocalDateTime; Admin.phone/email = null (D-005); kolom password_hash ↔ passwordHash.
   - Booking hasil query: Customer dan Room terisi lewat JOIN (Customer tanpa passwordHash — isi null — agar hash tidak menyebar; catat di Decision Log); BookingDAO.findById/findAll/findByCustomerId tidak memuat detail alat (detail dimuat BookingInstrumentDAO.findByBookingId lalu Booking.setInstruments); urutan findAll: booking_date DESC, start_time DESC.
   - BookingDAO.existsOverlap memakai query kontrak DATABASE_CONTRACT §4 (status IN ('PENDING','CONFIRMED'), start_time < :end AND end_time > :start); RoomDAO.findByIdForUpdate memakai SELECT ... FOR UPDATE.
   - BookingInstrumentDAO.updateQuantity hanya mengubah quantity (rental_price snapshot tidak berubah, BR-12); findByBookingId JOIN booking_instrument ↔ instrument.
   - DAO TIDAK berisi aturan bisnis (validasi, transisi status, otorisasi) dan tidak mengimpor javafx.*; SQLException dibiarkan naik (Service yang membungkusnya).
   - Hapus entitas yang masih direferensikan harus berujung SQLException (FK RESTRICT); DAO tidak menelannya.
D. Package-info dao/ dan util/ diperbarui (jangan menghapus fungsinya).

4. UJI
- Unit test PasswordUtil (murni, selalu berjalan; termasuk kompatibilitas dengan format utilitas dan penolakan hash rusak).
- Tes integrasi DAO terhadap MySQL nyata memakai config/db.properties: HARUS di-skip otomatis (JUnit Assumptions) bila config/db.properties tidak ada atau database tidak terjangkau, agar mvn test tetap hijau di mesin tanpa database. Tes tulis harus aman terhadap data seed: bungkus dalam transaksi + rollback (gunakan overload dengan Connection) atau bersihkan baris uji pada blok finally dengan identitas unik (mis. email UUID). Cakup: pemetaan semua kolom dari seed (mis. booking 1, 4, 5; booking_instrument 5 dengan snapshot 8000.00), CRUD tiap DAO, CRUD many-to-many (insert/insertAll/findByBookingId JOIN/updateQuantity/delete), existsOverlap untuk kasus: sebagian, mengandung, identik, berurutan, CANCELLED, COMPLETED (seed STEP 2: Studio A 2026-12-01 dan lainnya), FK RESTRICT (hapus customer/room/instrument yang dipakai → SQLException), CASCADE (hapus booking menghapus detail — di dalam transaksi lalu rollback), UNIQUE (booking_id, instrument_id) duplikat → SQLException, dan pembacaan findByIdForUpdate.
- Verifikasi di sandbox: MySQL 8.0.46 dapat dipasang lewat apt dan dijalankan (service mysql start); reset → schema → seed memakai database/*.sql. Maven Central TIDAK dapat diakses di sandbox, jadi Maven/Connector/J 8.4.0 tidak bisa diunduh; apt hanya menyediakan libmariadb-java 2.7.6 (driver MariaDB, URL jdbc:mariadb://). Bila dipakai untuk uji sanity SQL terhadap MySQL, nyatakan jelas bahwa itu BUKAN MySQL Connector/J dan BUKAN pengganti mvn test lokal. Jangan mengklaim "lulus" untuk sesuatu yang tidak dijalankan.
- Buat docs/STAGE4_VERIFICATION.md: apa yang benar-benar dijalankan, jumlah tes/gagal/error, tes yang di-skip dan alasannya, instruksi mvn clean test lokal beserta cara menyiapkan config/db.properties dan database (reset → schema → seed), serta checklist.
- Tandai bukti untuk AC-A01 (tidak ada SQL/java.sql di controller), AC-A02 (tidak ada javafx.* di model/dao/service), AC-A03 (semua SQL PreparedStatement + try-with-resources; sertakan hasil pencarian teks sebagai bukti) di docs/ACCEPTANCE_CRITERIA.md.

5. BATASAN
JANGAN membuat Service/exception service, DateTimeUtil, CurrencyUtil, SessionManager (D-035), controller, FXML baru, login/booking end-to-end, dan JANGAN mengubah pom.xml, database/schema.sql, seed.sql, reset.sql, atau class model/enum tanpa alasan teknis yang dicatat. Jangan membuat handoff tambahan. Jangan menyimpan password/credential asli di repository (config/db.properties tetap di-ignore Git). Pertahankan semua file STEP 0–3.

6. PEMBARUAN
README.md (status + cara menjalankan tes integrasi), PROGRESS.md, PROGRESS_HANDOFF.md (satu-satunya handoff hidup), docs/DECISION_LOG.md, docs/ACCEPTANCE_CRITERIA.md.

7. LAPORAN AKHIR
Daftar file dibuat/diubah; ringkasan DatabaseConnection, PasswordUtil, dan 6 DAO; hasil tes aktual (termasuk yang di-skip); apa yang belum berhasil/belum dijalankan; masalah terbuka; status STEP 4; akumulasi handoff STEP 0–4; prompt lengkap STEP 5 (Service). Berhenti setelah STEP 4; jangan mulai STEP 5.
```

