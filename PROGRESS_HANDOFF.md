# PROGRESS_HANDOFF — Sistem Pengelolaan Operasional Studio Musik (PBO TK II)

> Satu-satunya dokumen handoff lintas akun Claude. Diperbarui setiap akhir STEP. Jangan membuat file handoff lain.
> Terakhir diperbarui: 2026-11-18, akhir STEP 1.

## 1. Status
- **Tahap terakhir selesai dan terbukti:** STEP 0 (dokumen kontrak dibuat dan diperiksa konsistensinya).
- **STEP 1:** semua file scaffold dibuat dan lolos verifikasi *parsial* (kompilasi javac 21, 5 unit test via JUnit console, starter GUI di Xvfb dengan JavaFX 11). **Perintah Maven (`mvn clean/test/package/javafx:run`) belum berhasil dijalankan** karena sandbox tidak punya akses ke Maven Central. Status STEP 1 = *menunggu verifikasi lokal oleh pengguna*.
- **Jangan mulai STEP 2 sebelum pengguna melaporkan hasil `mvn test` dan `mvn clean javafx:run` di lokal** (atau menyatakan lanjut dengan risiko).

## 2. File proyek yang benar-benar dibuat (root `studio-musik/`)
```
pom.xml
README.md
PROGRESS.md
PROGRESS_HANDOFF.md
.gitignore
.vscode/extensions.json
config/db.properties.example
docs/FINAL_SPECIFICATION.md
docs/DECISION_LOG.md
docs/DATABASE_CONTRACT.md
docs/ARCHITECTURE_CONTRACT.md
docs/API_CONTRACTS.md
docs/ACCEPTANCE_CRITERIA.md
docs/PROJECT_HANDOFF_PBO.md
docs/STAGE1_VERIFICATION.md
src/main/java/com/studiomusik/MainApp.java
src/main/java/com/studiomusik/AppInfo.java
src/main/java/com/studiomusik/controller/MainController.java
src/main/java/com/studiomusik/{model,model/enums,dao,service,util}/package-info.java   (5 file; hanya Javadoc layer)
src/main/resources/com/studiomusik/view/main-view.fxml
src/main/resources/com/studiomusik/css/app.css
src/test/java/com/studiomusik/AppInfoTest.java
```
**Belum ada** (jangan dianggap tersedia): `database/*.sql`, model, enum, DAO, service, util, controller selain `MainController`, FXML selain `main-view.fxml`, `config/db.properties` asli, repository Git, screenshot, laporan akhir.

## 3. Kontrak teknis yang wajib dipertahankan
(Rincian lengkap: `docs/*`. Perubahan hanya lewat entri baru di `docs/DECISION_LOG.md`.)

**Proyek:** Java 21 · Maven `com.studiomusik:studio-musik-app:1.0.0-SNAPSHOT` · main class `com.studiomusik.MainApp` · non-modular (tanpa `module-info.java`) · UTF-8 · database `studio_musik` (MySQL 8.0.16+, InnoDB, utf8mb4) · config `config/db.properties` (di-ignore Git).

**Dependency (belum terverifikasi unduhannya):** javafx-controls & javafx-fxml **21.0.5** · mysql-connector-j **8.4.0** (runtime) · junit-jupiter **5.10.2** (test) · maven-compiler-plugin 3.13.0 · maven-surefire-plugin 3.2.5 · javafx-maven-plugin 0.0.8.

**Package:** `com.studiomusik` → `model`, `model.enums`, `dao`, `service`, `controller`, `util`. Resource: `src/main/resources/com/studiomusik/{view,css}`. Test: `src/test/java/com/studiomusik`.

**Class:** `Person` (abstract, `id,name,phone,email`, `abstract getRole()`), `Customer` (+`passwordHash`, role "Customer"), `Admin` (+`username`,`passwordHash`, phone/email null, role "Admin"), `Room(roomId,roomName,roomType,pricePerHour,status)`, `Instrument(instrumentId,name,type,rentalPrice,status)`, `BookingInstrument(bookingInstrumentId,bookingId,instrument,quantity,rentalPrice)`, `Booking(bookingId,customer,room,bookingDate,startTime,endTime,status,totalCost,createdAt,instruments)`. Uang = `BigDecimal`. Enum: `BookingStatus{PENDING,CONFIRMED,COMPLETED,CANCELLED}`, `RoomStatus{AVAILABLE,MAINTENANCE}`, `InstrumentStatus{AVAILABLE,MAINTENANCE}`.

**Tabel (6):** `admin(admin_id, username UNIQUE, password_hash, name)`; `customer(customer_id, name, phone, email UNIQUE NOT NULL, password_hash)`; `room(room_id, name, type, price_per_hour, status)`; `instrument(instrument_id, name, type, rental_price, status)`; `booking(booking_id, customer_id FK, room_id FK, booking_date, start_time, end_time, status, total_cost, created_at)`; `booking_instrument(booking_instrument_id, booking_id FK CASCADE, instrument_id FK RESTRICT, quantity, rental_price, UNIQUE(booking_id,instrument_id))`.

**Method signature:** seluruh DAO dan Service ada di `docs/API_CONTRACTS.md` §3–4 (termasuk CRUD `BookingInstrumentDAO` insert/insertAll/findByBookingId/updateQuantity/delete dan `BookingService.addInstrumentToBooking/getBookingInstruments/updateBookingInstrumentQuantity/removeInstrumentFromBooking`). Error: DAO `SQLException` → Service `ServiceException` (+`ValidationException`, `BusinessRuleException`).

**Aturan bisnis (ringkas):** PENDING & CONFIRMED memblokir jadwal; CANCELLED & COMPLETED tidak · overlap `existingStart < requestedEnd AND existingEnd > requestedStart` (berurutan boleh) · `end > start`, durasi jam bulat · Room/Instrument MAINTENANCE tidak bisa dipakai · biaya ruangan = harga/jam × durasi; alat = quantity × harga per unit per sesi; total `BigDecimal` HALF_UP skala 2 · harga alat di-snapshot di `booking_instrument.rental_price` · booking baru PENDING; transisi PENDING→CONFIRMED/CANCELLED, CONFIRMED→COMPLETED/CANCELLED · satu transaksi untuk booking + detail alat; kunci baris room sebelum cek overlap · Customer hanya akses booking sendiri · password PBKDF2 (`pbkdf2_sha256$iter$salt$hash`) · tidak ada hapus booking permanen.

**Penomoran tahap:** 0 Spesifikasi · 1 Maven · 2 Database Schema & Seed · 3 Model & enum · 4 Util JDBC & DAO · 5 Service · 6 FXML & CSS · 7 Controller & navigasi · 8 Integrasi & pengujian · 9 README, screenshot, laporan, ZIP.

## 4. Keputusan penting yang diambil di STEP 0 (ringkas; lengkap di Decision Log D-001…D-022)
Dua aktor (Customer + Admin); `password_hash`; `customer.email` unik sebagai login; `BigDecimal` menggantikan `double`; method Class Diagram yang butuh DB (`checkAvailability`, `saveBooking`, dst.) direalisasikan di Service, bukan model (D-013); resource dan config mengikuti master prompt (D-015); exception di package `service` (D-017); PBKDF2 tanpa dependency baru (D-018).

## 5. Terbuka / perlu konfirmasi tim
- **A-01** tarif alat per unit per sesi (ditetapkan master prompt, belum dikonfirmasi tim).
- **A-02** tidak ada stok alat (keterbatasan diterima; tulis di laporan).
- **A-03** pembatalan oleh Customer: tidak ada di v1.
- **A-04** tabel transisi status (diusulkan). **A-05** durasi jam bulat (diusulkan).
- Versi dependency belum diverifikasi terhadap Maven Central (D-021); jika unduhan gagal di lokal, ganti properti versi di `pom.xml` dan catat sebagai D-023.

## 6. Hasil verifikasi aktual
| Item | Hasil |
|---|---|
| JDK | OpenJDK 21.0.13 + javac 21.0.13 (sandbox; JDK dipasang via apt) |
| Maven | 3.8.7 (sandbox). `mvn clean`, `mvn test`, `mvn package`, `mvn clean javafx:run` → **GAGAL**, penyebab: *Unknown host repo.maven.apache.org* (jaringan sandbox) |
| javac 21 pada `src/main` dan `AppInfoTest` | Sukses (dengan OpenJFX 11 dan JUnit 5.10.1 dari apt) |
| Unit test | 5/5 lulus via JUnit console (bukan Maven) |
| Starter GUI | Jalan di Xvfb dengan JavaFX 11: judul, 1 stylesheet, label dari `AppInfo`, tombol mengubah status "Jumlah klik: 2". Tidak ada screenshot. Belum diuji dengan JavaFX 21 |
| Pemeriksaan konsistensi dokumen | Lulus (versi pom = ARCHITECTURE_CONTRACT = DECISION_LOG; 22 BR terdefinisi; tidak ada rujukan BR/A menggantung; 6 tabel) |
| Perintah yang harus Anda jalankan lokal | `docs/STAGE1_VERIFICATION.md` §4 |

## 7. Error / pekerjaan belum selesai
1. Verifikasi Maven penuh (butuh lokal). 2. Screenshot jendela starter. 3. Konfirmasi A-01/A-03/A-04/A-05. 4. Repository Git belum dibuat. 5. Seluruh STEP 2–9.

## 8. Prompt lengkap untuk STEP 2 (Database Schema & Seed)

Salin ke percakapan baru (lampirkan: `PROGRESS_HANDOFF.md`, ZIP/isi proyek `studio-musik/` terbaru, dan laporan hasil verifikasi Maven lokal).

```
Anda adalah Software Architect dan Java Developer untuk proyek kuliah PBO "Sistem Pengelolaan Operasional Studio Musik" (Java 21, JavaFX, Maven, MySQL 8.0+).

LANGKAH AWAL (wajib, sebelum membuat file apa pun):
1. Baca PROGRESS_HANDOFF.md, lalu semua file di docs/ (terutama DATABASE_CONTRACT.md, DECISION_LOG.md, FINAL_SPECIFICATION.md, API_CONTRACTS.md).
2. Periksa file proyek yang benar-benar saya lampirkan. Jangan mengasumsikan file ada hanya karena disebut di catatan.
3. Laporkan hasil verifikasi STEP 1 yang saya berikan: [TEMPEL HASIL mvn test / mvn package / javafx:run LOKAL SAYA DI SINI]. Jika STEP 1 belum lulus, berhenti dan bantu perbaiki STEP 1 terlebih dahulu.
4. Jika ada konflik antara dokumen dan file aktual, laporkan file dan masalahnya sebelum mengubah apa pun. Catat keputusan baru sebagai entri baru di docs/DECISION_LOG.md.

JAWABAN TIM atas pertanyaan terbuka (isi sebelum dikirim; jika kosong, pakai default dokumen):
- A-01 tarif alat per unit per sesi: [YA / TIDAK, ...]
- A-03 Customer boleh membatalkan booking sendiri: [TIDAK / HANYA PENDING]
- A-04 tabel transisi status: [SETUJU / REVISI ...]
- A-05 durasi jam bulat: [SETUJU / PECAHAN ...]

TUGAS STEP 2 — Database Schema & Seed (hanya ini):
Buat file nyata (bukan hanya ditampilkan di chat):
1. database/schema.sql — membuat database studio_musik (utf8mb4) dan 6 tabel TEPAT sesuai docs/DATABASE_CONTRACT.md: kolom, tipe, NOT NULL, DEFAULT, PK, FK (RESTRICT/CASCADE sesuai kontrak), UNIQUE (admin.username, customer.email, booking_instrument(booking_id, instrument_id)), CHECK, indeks booking(room_id, booking_date, status), created_at.
2. database/seed.sql — 1 Admin (password_hash HASIL GENERATE format pbkdf2_sha256$iterasi$salt$hash sesuai D-018; kredensial demo ditulis di README, bukan di SQL), beberapa Room (termasuk 1 MAINTENANCE), beberapa Instrument (termasuk 1 MAINTENANCE), beberapa Customer demo (password_hash hasil generate), dan beberapa Booking + booking_instrument dengan status berbeda untuk menguji overlap (termasuk kasus berurutan 10:00-12:00 lalu 12:00-14:00, dan booking CANCELLED di jadwal yang sama dengan booking aktif).
3. database/reset.sql — mengembalikan database ke kondisi awal (drop/ulang dengan urutan yang menghormati FK).
4. Skrip kecil untuk meng-generate hash PBKDF2 sesuai format D-018 (mis. satu file Java single-file di database/tools/ atau jshell) dan petunjuk memakainya; jangan menambah dependency Maven.
5. docs/STAGE2_VERIFICATION.md — hasil verifikasi aktual.
6. Perbarui README.md (bagian database + kredensial demo), PROGRESS.md, dan PROGRESS_HANDOFF.md (satu-satunya handoff; jangan membuat file handoff baru).

VERIFIKASI (jalankan sungguhan; jangan mengklaim berhasil jika tidak dijalankan):
- Coba pasang dan jalankan MySQL/MariaDB di sandbox (apt) lalu eksekusi schema.sql, seed.sql, reset.sql. Jika tidak bisa (mis. akses jaringan/versi), nyatakan eksplisit dan berikan perintah agar saya memverifikasi di MySQL lokal.
- Buktikan: 6 tabel terbentuk (SHOW TABLES / SHOW CREATE TABLE), FK dan UNIQUE bekerja (INSERT duplikat booking_instrument harus ditolak), CHECK bekerja (end_time <= start_time ditolak), query overlap dari DATABASE_CONTRACT §4 mengembalikan hasil yang benar untuk kasus seed (beririsan sebagian, mengandung, identik, berurutan, status CANCELLED).
- Verifikasi acceptance criteria AC-D01…AC-D05 di docs/ACCEPTANCE_CRITERIA.md dengan bukti.

BATASAN:
- Pertahankan nama tabel, kolom, package, class, dan kontrak yang sudah dikunci. Jangan mengubah kontrak tanpa alasan tercatat.
- JANGAN membuat Model, DAO, Service, Controller, FXML, atau login/booking. Jangan mengerjakan STEP 3.
- Jangan menaruh password plaintext atau credential asli di file mana pun.
- Berhenti setelah STEP 2 selesai. Akhiri dengan: ringkasan keputusan, daftar file yang benar-benar dibuat, hasil verifikasi aktual, masalah tersisa, status proyek, dan prompt lengkap untuk STEP 3 (Model & Enum).
```
