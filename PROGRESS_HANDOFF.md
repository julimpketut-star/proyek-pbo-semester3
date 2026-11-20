# PROGRESS_HANDOFF — Sistem Pengelolaan Operasional Studio Musik (PBO TK II)

> Satu-satunya dokumen handoff lintas akun Claude. Diperbarui setiap akhir STEP. Jangan membuat file handoff lain.
> Terakhir diperbarui: 2026-11-19, akhir STEP 2.

## 1. Status
- **STEP 0** — selesai (dokumen kontrak, pemeriksaan konsistensi).
- **STEP 1** — **selesai**. Bukti lokal pengguna 2026-11-19: `mvn clean test` (5 tes, 0 gagal, BUILD SUCCESS) dan `mvn package` (BUILD SUCCESS, JAR terbentuk). `mvn clean javafx:run` dilaporkan berhasil oleh pengguna (tanpa screenshot); tombol "Uji Interaksi" belum dikonfirmasi. Branch Git pengguna: `feature/database`.
- **STEP 2** — **selesai, diverifikasi di sandbox** (MySQL 8.0.46): 57/57 uji lulus, siklus reset → schema → seed lulus. **Belum diverifikasi di MySQL lokal pengguna** (Windows); wajib dijalankan sebelum STEP 4 (DAO). Koneksi JDBC dari Java belum diuji.
- **Tahap berikutnya:** STEP 3 — Model & Enum (tidak memerlukan database).

## 2. File proyek yang benar-benar ada (root `studio-musik/`)
```
pom.xml
README.md  PROGRESS.md  PROGRESS_HANDOFF.md  .gitignore
.vscode/extensions.json
config/db.properties.example
database/schema.sql
database/seed.sql
database/reset.sql
database/tools/Pbkdf2HashGenerator.java
database/tests/verify_stage2.sql
docs/FINAL_SPECIFICATION.md      docs/DECISION_LOG.md (D-001…D-028)
docs/DATABASE_CONTRACT.md        docs/ARCHITECTURE_CONTRACT.md
docs/API_CONTRACTS.md            docs/ACCEPTANCE_CRITERIA.md
docs/PROJECT_HANDOFF_PBO.md      docs/STAGE1_VERIFICATION.md   docs/STAGE2_VERIFICATION.md
src/main/java/com/studiomusik/MainApp.java
src/main/java/com/studiomusik/AppInfo.java
src/main/java/com/studiomusik/controller/MainController.java
src/main/java/com/studiomusik/{model,model/enums,dao,service,util}/package-info.java   (hanya Javadoc layer)
src/main/resources/com/studiomusik/view/main-view.fxml
src/main/resources/com/studiomusik/css/app.css
src/test/java/com/studiomusik/AppInfoTest.java
```
**Belum ada** (jangan dianggap tersedia): class Model/Enum, `DatabaseConnection` dan util lain, DAO, Service, controller selain `MainController`, FXML selain `main-view.fxml`, `config/db.properties` asli (lokal, di-ignore Git), screenshot, laporan akhir.

**File STEP 2 — dibuat:** `database/*` (lima file di atas), `docs/STAGE2_VERIFICATION.md`. **Diubah:** `config/db.properties.example`, `README.md`, `PROGRESS.md`, `PROGRESS_HANDOFF.md`, `docs/DECISION_LOG.md`, `docs/DATABASE_CONTRACT.md`, `docs/ARCHITECTURE_CONTRACT.md`, `docs/STAGE1_VERIFICATION.md`. **Tidak diubah:** semua kode STEP 1 dan `pom.xml`.

## 3. Kontrak teknis yang wajib dipertahankan
(Rincian lengkap: `docs/*`. Perubahan hanya lewat entri baru di `docs/DECISION_LOG.md`.)

**Proyek:** Java 21 · Maven `com.studiomusik:studio-musik-app:1.0.0-SNAPSHOT` · main class `com.studiomusik.MainApp` · non-modular (tanpa `module-info.java`) · UTF-8 · database `studio_musik` (MySQL 8.0.16+, InnoDB, utf8mb4) · config `config/db.properties` (di-ignore Git; templat `config/db.properties.example`, URL `jdbc:mysql://localhost:3306/studio_musik?...`).

**Dependency (terbukti terunduh di build lokal 2026-11-19):** javafx-controls & javafx-fxml **21.0.5** · mysql-connector-j **8.4.0** (runtime) · junit-jupiter **5.10.2** (test) · maven-compiler-plugin 3.13.0 · maven-surefire-plugin 3.2.5 · javafx-maven-plugin 0.0.8.

**Package:** `com.studiomusik` → `model`, `model.enums`, `dao`, `service`, `controller`, `util`. Resource: `src/main/resources/com/studiomusik/{view,css}`. Test: `src/test/java/com/studiomusik`.

**Class (dikunci, belum diimplementasi):** `Person` (abstract, `id,name,phone,email`, `abstract getRole()`), `Customer` (+`passwordHash`, role "Customer"), `Admin` (+`username`,`passwordHash`, phone/email null, role "Admin"), `Room(roomId,roomName,roomType,pricePerHour,status)`, `Instrument(instrumentId,name,type,rentalPrice,status)`, `BookingInstrument(bookingInstrumentId,bookingId,instrument,quantity,rentalPrice)`, `Booking(bookingId,customer,room,bookingDate,startTime,endTime,status,totalCost,createdAt,instruments)`. Uang = `BigDecimal`. Enum: `BookingStatus{PENDING,CONFIRMED,COMPLETED,CANCELLED}` (+`blocksSchedule()`, `canTransitionTo()`), `RoomStatus{AVAILABLE,MAINTENANCE}`, `InstrumentStatus{AVAILABLE,MAINTENANCE}`. Signature lengkap: `docs/API_CONTRACTS.md` §1.

**Tabel (6, SUDAH ada di database/schema.sql, sesuai kontrak tanpa perubahan):**
`admin(admin_id, username UNIQUE, password_hash, name)` · `customer(customer_id, name, phone, email UNIQUE NOT NULL, password_hash)` · `room(room_id, name, type, price_per_hour, status)` · `instrument(instrument_id, name, type, rental_price, status)` · `booking(booking_id, customer_id FK RESTRICT, room_id FK RESTRICT, booking_date, start_time, end_time, status, total_cost, created_at)` · `booking_instrument(booking_instrument_id, booking_id FK CASCADE, instrument_id FK RESTRICT, quantity, rental_price, UNIQUE(booking_id,instrument_id))`.
Constraint bernama: `pk_*`, `uq_admin_username`, `uq_customer_email`, `uq_booking_instrument_pair`, `fk_booking_customer`, `fk_booking_room`, `fk_bi_booking`, `fk_bi_instrument`, `chk_room_price`, `chk_instrument_price`, `chk_booking_time`, `chk_booking_total`, `chk_bi_quantity`, `chk_bi_price`, indeks `idx_booking_room_date_status(room_id, booking_date, status)`. Kolom uang `DECIMAL(10,2)`.

**Method signature DAO/Service/Util:** `docs/API_CONTRACTS.md` §2–4 (termasuk CRUD `BookingInstrumentDAO` insert/insertAll/findByBookingId/updateQuantity/delete dan `BookingService.addInstrumentToBooking/getBookingInstruments/updateBookingInstrumentQuantity/removeInstrumentFromBooking`). Error: DAO `SQLException` → Service `ServiceException` (+`ValidationException`, `BusinessRuleException`, di package `service`).

**Password:** `pbkdf2_sha256$<iterasi>$<saltBase64>$<hashBase64>`; PBKDF2WithHmacSHA256; salt 16 byte `SecureRandom`; kunci 256 bit; iterasi bawaan **600000** (D-023); `PasswordUtil` (STEP 4) wajib kompatibel dengan `database/tools/Pbkdf2HashGenerator.java` dan membaca iterasi dari string hash. Verifikasi memakai perbandingan waktu-konstan.

**Aturan bisnis (ringkas):** PENDING & CONFIRMED memblokir jadwal; CANCELLED & COMPLETED tidak · overlap `existingStart < requestedEnd AND existingEnd > requestedStart` (berurutan boleh) · `end > start`, durasi jam bulat (A-05) · Room/Instrument MAINTENANCE tidak bisa dipakai · biaya ruangan = harga/jam × durasi; alat = quantity × harga per unit per sesi; total `BigDecimal` HALF_UP skala 2 · harga alat di-snapshot di `booking_instrument.rental_price` · booking baru PENDING; transisi PENDING→CONFIRMED/CANCELLED, CONFIRMED→COMPLETED/CANCELLED · satu transaksi untuk booking + detail alat; kunci baris room (`SELECT ... FOR UPDATE`) sebelum cek overlap · Customer hanya akses booking sendiri · tanggal lampau ditolak (BR-21) · tidak ada hapus booking permanen · hapus Room/Instrument/Customer yang dipakai booking ditolak (BR-22).

**Data seed (kontrak data uji; ID eksplisit):** admin 1 (`admin`); customer 1–6 (email `*@example.com`, customer 6 tanpa booking); room 1–5 (Studio A 150000, Studio B 120000, Studio C 80000, Studio Rekaman 250000, Studio D 100000 MAINTENANCE tanpa booking); instrument 1–7 (instrument 7 MAINTENANCE tanpa booking; instrument 5 harga sekarang 10000); booking 1–7 dan booking_instrument 1–12 dengan skenario: booking 1 (A, 2026-12-01 10–12 CONFIRMED), 2 (12–14 PENDING, berurutan), 3 (10–12 CANCELLED), 4 (A, 2026-11-10 10–13 COMPLETED, snapshot alat 8000), 5 (Studio B 2026-12-02 15–18 PENDING, 4 alat), 6 (Studio C 2026-12-03 09–10 CONFIRMED), 7 (Studio Rekaman 2026-12-05 13–17 CANCELLED). Semua `total_cost` konsisten dengan rumus (diuji B06). **Password demo tidak ada di repository** (D-024); Admin login `admin`, Customer login memakai email; password disampaikan terpisah oleh tim.

**Penomoran tahap:** 0 Spesifikasi · 1 Maven · 2 Database Schema & Seed · 3 Model & enum · 4 Util JDBC & DAO · 5 Service · 6 FXML & CSS · 7 Controller & navigasi · 8 Integrasi & pengujian · 9 README, screenshot, laporan, ZIP.

## 4. Keputusan penting (ringkas; lengkap di Decision Log D-001…D-028)
Dua aktor (Customer + Admin); `password_hash`; `customer.email` unik sebagai login; `BigDecimal` menggantikan `double`; method Class Diagram yang butuh DB direalisasikan di Service, bukan model (D-013); resource/config mengikuti master prompt (D-015); exception di package `service` (D-017); PBKDF2 tanpa dependency baru, 600000 iterasi (D-018, D-023); kredensial demo tidak di repository (D-024); `schema.sql` idempotent, `seed.sql` hanya untuk tabel kosong, `reset.sql` hanya drop tabel (D-025); skrip verifikasi tambahan (D-026); tanggal seed tetap (D-027).

## 5. Terbuka / perlu konfirmasi tim
- **A-01** tarif alat per unit per sesi (ditetapkan master prompt, belum dikonfirmasi tim). **A-02** tidak ada stok alat (keterbatasan diterima; tulis di laporan). **A-03** pembatalan oleh Customer: tidak ada di v1. **A-04** tabel transisi status (diusulkan). **A-05** durasi jam bulat (diusulkan).
- Verifikasi database di MySQL lokal Windows (wajib sebelum STEP 4).
- Screenshot starter dan konfirmasi tombol "Uji Interaksi" (lampiran laporan).

## 6. Hasil verifikasi aktual
| Item | Hasil |
|---|---|
| STEP 1 lokal (screenshot pengguna) | `mvn clean test`: 5 tes, 0 gagal, BUILD SUCCESS (26.342 s); `mvn package`: BUILD SUCCESS, `studio-musik-app-1.0.0-SNAPSHOT.jar` terbentuk (21.224 s); `javafx:run` dilaporkan berhasil (tanpa bukti); tombol belum dikonfirmasi |
| STEP 2 sandbox | MySQL `8.0.46-0ubuntu0.24.04.5`, mode strict. `verify_stage2.sql`: **57 LULUS, 0 GAGAL** (struktur 14, seed 9, overlap 11, constraint 20, FK 3) |
| Siklus ulang | reset → 0 tabel; schema → 6 tabel; seed → 1/6/5/7/7/12; schema ulang saat data ada → data utuh; seed ulang tanpa reset → gagal sesuai desain (error 1062); reset → schema → seed → verify → 57 LULUS |
| AC-D01…AC-D05 | Lulus (sandbox) |
| Utilitas hash | hash seed cocok dengan password demo yang benar, tidak cocok dengan yang salah; salt berbeda tiap akun; mode `--stdin`, `--verify`, dan interaktif (pseudo-TTY) berjalan; penyembunyian echo tidak diverifikasi mandiri |
| Plaintext password di repo | Tidak ada (pencarian teks) |
| Belum diuji | MySQL lokal Windows; koneksi JDBC dari Java; `PowerShell` langsung (gaya `-e "source ..."` diuji di Linux) |

## 7. Error / pekerjaan belum selesai
1. Verifikasi MySQL lokal. 2. Screenshot starter + konfirmasi tombol. 3. Konfirmasi A-01/A-03/A-04/A-05. 4. Seluruh STEP 3–9. 5. Pembaruan Use Case/Flow di laporan akhir sesuai D-001 dan D-013.
Masalah yang ditemukan dan sudah diperbaiki di STEP 2: tiga bug pada skrip uji (urutan ENUM di `GROUP_CONCAT`, nama tabel pada SQL dinamis, handler untuk error 1265); tidak ada bug pada skema atau seed.

## 8. Prompt lengkap untuk STEP 3 (Model & Enum)

Salin ke percakapan baru. Lampirkan: `PROGRESS_HANDOFF.md`, isi proyek `studio-musik/` terbaru (ZIP), dan hasil `mvn clean test` lokal terbaru bila ada.

```
Anda adalah Software Architect dan Java Developer untuk proyek kuliah PBO "Sistem Pengelolaan Operasional Studio Musik" (Java 21, JavaFX, Maven, MySQL 8.0+). Lanjutkan di workspace yang sudah ada; jangan membuat proyek baru atau mengubah struktur.

1. AUDIT WAJIB (sebelum membuat file):
- Baca PROGRESS_HANDOFF.md, README.md, dan docs/FINAL_SPECIFICATION.md, DECISION_LOG.md, ARCHITECTURE_CONTRACT.md, API_CONTRACTS.md (terutama §1 Model), ACCEPTANCE_CRITERIA.md (AC-M01…AC-M05).
- Periksa file aktual di workspace; jangan menganggap sesuatu ada hanya karena disebut di handoff.
- Hasil verifikasi lokal saya: mvn clean test: [TEMPEL OUTPUT]; verifikasi MySQL lokal (docs/STAGE2_VERIFICATION.md §7): [TEMPEL: LULUS 57 / hasil lain / belum dijalankan]; branch aktif: [NAMA BRANCH].
- Jawaban tim atas pertanyaan terbuka (jika kosong, pakai default dokumen): A-01 [..], A-03 [..], A-04 [..], A-05 [..].
- Gerbang: bila mvn clean test gagal, jangan lanjut; bantu perbaiki dulu. Verifikasi MySQL lokal tidak menghalangi STEP 3 tetapi wajib selesai sebelum STEP 4.
- Jika ada konflik dokumen vs file aktual, laporkan lebih dulu dan catat entri BARU di DECISION_LOG.md (lanjutkan nomor setelah D-028).

2. KONTRAK YANG TIDAK BOLEH DIUBAH: package com.studiomusik (model, model.enums), nama class/atribut/method pada API_CONTRACTS.md §1, enam tabel dan kolom (database/schema.sql), uang = BigDecimal (skala 2, RoundingMode.HALF_UP), Java release 21, non-modular, tanpa dependency baru.

3. TUGAS STEP 3 (hanya Model & Enum), buat file nyata:
- model/enums: BookingStatus (+ blocksSchedule(), canTransitionTo(BookingStatus) sesuai BR-14), RoomStatus, InstrumentStatus.
- model: Person (abstract, getRole() abstract), Customer, Admin (getRole() di-override: "Customer" / "Admin"), Room, Instrument, BookingInstrument, Booking — sesuai API_CONTRACTS.md §1: atribut private, getter/setter, konstruktor, calculateDuration() (jam bulat), calculateTotalCost(), calculateSubtotal(), calculateCost(...), isAvailable(), addInstrument(Instrument, int) (snapshot harga), removeInstrument(int). Hanya perhitungan murni; TANPA SQL, TANPA javafx, TANPA akses database.
- Keputusan desain kecil (mis. perilaku addInstrument pada alat duplikat atau quantity < 1, validasi null) harus konsisten dengan BR-17/BR-18, diletakkan di model hanya bila murni, dan DICATAT di DECISION_LOG.md.
- Hapus package-info.java di model/ dan model/enums/ HANYA jika diganti isi yang setara; jangan menyentuh package lain.
- Unit test JUnit 5 di src/test/java/com/studiomusik/model/: cukup untuk AC-M01 (Person abstract), AC-M02 (getRole polimorfik lewat referensi Person), AC-M03 (contoh: Rp100.000/jam × 2 jam + 2 alat × Rp25.000 = Rp250.000), AC-M04 (BigDecimal, HALF_UP skala 2), tabel transisi status (semua kombinasi), BookingStatus.blocksSchedule, snapshot harga alat, dan tabel pemetaan 7 konsep PBO ke class/method nyata (AC-M05) di docs.

4. VERIFIKASI: jalankan yang benar-benar bisa dijalankan (di sandbox Maven biasanya tidak bisa mengunduh dependency: gunakan javac + JUnit console bila tersedia dan nyatakan jelas bahwa itu bukan pengganti mvn). Buat docs/STAGE3_VERIFICATION.md berisi hasil aktual dan instruksi agar saya menjalankan mvn clean test lokal. Jangan mengklaim berhasil bila tidak dijalankan.

5. BATASAN: JANGAN membuat DAO, Service, util JDBC (DatabaseConnection dst.), controller, FXML baru, login/booking, dan JANGAN mengubah pom.xml/skema SQL/seed tanpa alasan tercatat. Jangan membuat handoff tambahan. Jangan menyimpan password/credential di repository.

6. PEMBARUAN FILE: README.md (status), PROGRESS.md, PROGRESS_HANDOFF.md (satu-satunya handoff), docs/ACCEPTANCE_CRITERIA.md bila perlu menandai status AC-M.

7. LAPORAN AKHIR: daftar file dibuat/diubah, hasil verifikasi aktual, yang belum berhasil/belum dijalankan, masalah terbuka, status STEP 3, akumulasi handoff STEP 0–3, dan prompt lengkap STEP 4 (Util JDBC & DAO). Berhenti setelah STEP 3; jangan mulai STEP 4.
```
