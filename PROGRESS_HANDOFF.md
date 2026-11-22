# PROGRESS_HANDOFF — Sistem Pengelolaan Operasional Studio Musik (PBO TK II)

> Satu-satunya dokumen handoff lintas akun Claude. Diperbarui setiap akhir STEP. Jangan membuat file handoff lain.
> Terakhir diperbarui: 2026-11-22, akhir STEP 7. Kumulatif STEP 0–7.

## 1. Status
| STEP | Isi | Status / bukti |
|---|---|---|
| 0 | Spesifikasi & kontrak (`docs/*`) | Selesai |
| 1 | Maven + starter JavaFX | Selesai; lokal 2026-11-19: `mvn clean test` 5 tes, `mvn package` OK |
| 2 | `database/schema.sql`, `seed.sql`, `reset.sql`, `verify_stage2.sql` | Selesai; sandbox MySQL 8.0.46 57/57 |
| 3 | Model (7 class) + 3 enum | Selesai; 79 tes |
| 4A/4B | `DatabaseConnection`, `PasswordUtil`, 6 DAO (+ tes) | Selesai; **dilaporkan pengguna: `mvn clean test` = 110 tes, 0 gagal, 0 error, 2 skip, BUILD SUCCESS** |
| 5 | Service + exception + tes AC-R01…R19 | Kode selesai & terkompilasi; tes DB belum dijalankan (lihat §6) |
| **6** | **View: 5 FXML + `app.css` + `docs/GUI_CONTRACT.md`** | **File selesai; XML well-formed, `fx:id`/handler konsisten dengan kontrak. FXML belum dimuat di JavaFX (controller belum ada) dan tampilan belum dilihat/di-screenshot (lihat §9)** |
| **7** | **Controller (5) + Util (`SessionManager`, `DateTimeUtil`, `CurrencyUtil`) + `Navigator`/`UiSupport` + `MainApp`** | **Kode selesai. Belum dikompilasi penuh dan belum dijalankan dengan JavaFX (lihat §10).** |
- **Tahap berikutnya:** STEP 8 — integrasi & perbaikan hasil uji tim (jalankan aplikasi, perbaiki galat FXML/CSS/controller), lalu STEP 9 (README, screenshot, laporan, ZIP).

## 2. File proyek (root `studio-musik/`)
Kode STEP 0–4 tidak diubah pada STEP 5. Catatan: `PROGRESS.md`, `README.md`, `docs/DECISION_LOG.md` di ZIP masih berstatus STEP 3 (STEP 4 & 5 belum tercatat; dilarang diubah pada STEP 5 → **koordinator perlu menyinkronkan**, lihat §7).

**STEP 5 — main** (`src/main/java/com/studiomusik/service/`): `ServiceException`, `ValidationException`, `BusinessRuleException`, `AuthService`, `CustomerService`, `RoomService`, `InstrumentService`, `BookingService`, `ServiceSupport` (helper package-private, bukan API), `package-info.java` (diperbarui).
**STEP 5 — test** (`src/test/java/com/studiomusik/service/`, 76 tes baru): `ServiceTestSupport` (pendukung), `ServiceValidationTest` (11, tanpa DB), `AuthServiceTest` (8), `CustomerServiceTest` (7), `RoomServiceTest` (7), `InstrumentServiceTest` (6), `BookingServiceTest` (22), `BookingInstrumentServiceTest` (10), `BookingTransactionTest` (5).
Tidak ada FXML/Controller baru pada STEP 5.

**STEP 6 — view/CSS/docs** (hanya resource & dokumen; tidak ada kelas Java baru, `model/ util/ dao/ service/ database/ pom.xml` tidak diubah):
- Baru: `src/main/resources/com/studiomusik/view/{landing,login,register,customer-dashboard,admin-dashboard}-view.fxml` (200 `fx:id`, 44 handler unik), `docs/GUI_CONTRACT.md`.
- Diubah: `src/main/resources/com/studiomusik/css/app.css` (tema SONORA; kelas starter lama dipertahankan agar `main-view.fxml` tetap berfungsi, tetapi warnanya kini mengikuti palet SONORA), `PROGRESS.md`, `PROGRESS_HANDOFF.md`.
- Dipertahankan: `view/main-view.fxml` + `MainController` (referensi starter). `MainApp` masih memuat `main-view.fxml`.
- Peringatan: FXML baru merujuk `fx:controller` yang belum ada → memuatnya sebelum STEP 7 akan gagal (`ClassNotFoundException`); itu perilaku yang diharapkan.

**STEP 7 — main** (hanya Java; FXML/CSS/model/dao/service/`pom.xml`/DB tidak diubah):
- Baru `util/`: `SessionManager`, `DateTimeUtil`, `CurrencyUtil`.
- Baru `controller/`: `LandingController`, `LoginController`, `RegisterController`, `CustomerDashboardController`, `AdminDashboardController`, `Navigator` (public, helper navigasi minimal), `UiSupport` (package-private, helper tampilan).
- Diubah: `MainApp.java` (membuka Landing lewat `Navigator`; satu Scene dipakai bersama).
- Dipertahankan: `MainController` + `view/main-view.fxml` (referensi starter; tidak dimuat lagi oleh `MainApp`).
- **Alasan `Navigator`/`UiSupport`:** navigasi 5 halaman + data sementara (jadwal pilihan Landing, pesan Login) dan kode tampilan berulang (pesan, kolom/lencana, parser harga) dibutuhkan semua controller; satu kelas statis kecil menghindari duplikasi tanpa membuat framework navigasi. Keduanya tanpa SQL/aturan bisnis.

## 3. Kontrak teknis yang wajib dipertahankan
Java 21 · Maven `com.studiomusik:studio-musik-app:1.0.0-SNAPSHOT` · non-modular · UTF-8 · MySQL 8.0.16+ `studio_musik` · config `config/db.properties` (di-ignore Git). JavaFX/Connector-J/JUnit 21.0.5 / 8.4.0 / 5.10.2.
Layer: `View → Controller → Service → DAO → MySQL`. DAO melempar `SQLException`; Service → `ServiceException` (unchecked; `ValidationException`, `BusinessRuleException` subclass). Service tanpa `javafx.*` dan tanpa SQL. Uang `BigDecimal` skala 2 `HALF_UP`. Password PBKDF2 `pbkdf2_sha256$600000$salt$hash` via `PasswordUtil`. Signature: `docs/API_CONTRACTS.md` §1–4 (tidak berubah).
Tabel (6) dan seed: customer 1–6 (email `*@example.com`), room 1–5 (5 = MAINTENANCE), instrument 1–7 (7 = MAINTENANCE), booking 1–7, booking_instrument 1–12 (booking 1 & 6 milik customer 1). Password demo tidak ada di repo (D-024).
Aturan bisnis BR-01…BR-22 dan asumsi A-01…A-11: `docs/FINAL_SPECIFICATION.md`.

## 4. Perilaku Service yang dikunci pada STEP 5 (usulan D-040…; belum di Decision Log)
- **Exception:** input kosong/format/rentang, id tidak ditemukan → `ValidationException`; pelanggaran aturan, login gagal, email duplikat, FK RESTRICT → `BusinessRuleException`; SQL/koneksi/tak terduga → `ServiceException` berpesan umum (detail hanya `java.util.logging`, cause tetap terpasang).
- **Login:** pesan generik sama untuk akun tidak ada/password salah (`Username atau password salah.` / `Email atau password salah.`); verifikasi dummy menyamakan waktu. Input kosong → `ValidationException`. Objek `Admin`/`Customer` hasil Service **tanpa `passwordHash`** (semua method Auth/Customer).
- **Validasi:** email trim+huruf kecil, regex dasar, ≤100; telepon: spasi/`-` dibuang, `+?` + 8–15 digit, ≤20; nama ≤100; password **8–128 karakter**; Room/Instrument nama & tipe ≤50, harga ≥0, ≤99.999.999,99, dibulatkan skala 2 HALF_UP. `addRoom/addInstrument` mengembalikan objek baru (argumen tidak diubah).
- **Waktu:** urutan cek: null → `end > start` (BR-03) → jam bulat termasuk detik (BR-04) → tanggal ≥ hari ini (BR-21; hari ini diterima, jam lewat tidak dicek). Dilakukan **sebelum** akses DB. `DateTimeUtil` **tidak dipakai** (tidak ada di ZIP dan `util/` dilarang diubah); logika waktu privat di `BookingService`. Akibat: tidak ada slot sampai 24:00 (sesi terakhir berakhir 23:00).
- **`isRoomAvailable`:** `false` hanya untuk overlap; Room MAINTENANCE → `BusinessRuleException`; room tidak ada → `ValidationException`.
- **`createBooking`:** 1 transaksi; urutan: validasi → customer (koneksi terpisah) → `RoomDAO.findByIdForUpdate` → MAINTENANCE → `existsOverlap` → muat alat (tidak ada → Validation; MAINTENANCE → BusinessRule) → hitung total via `Booking.calculateTotalCost` → insert booking → `insertAll` (dilewati bila tanpa alat) → baca ulang → commit; exception apa pun → rollback.
- **Status/detail:** `updateBookingStatus` dan CRUD detail mengunci room booking dulu (baca awal lewat koneksi terpisah agar snapshot InnoDB tidak basi), lalu membaca ulang booking. BR-19: hanya PENDING/CONFIRMED. Total dihitung ulang dengan `Booking.calculateTotalCost()` dari detail terbaru (snapshot harga alat tetap; `updateQuantity` tidak menyentuh snapshot).
- **BR-16:** `getBookingForCustomer` memberi pesan sama untuk "tidak ada" dan "bukan milik" (`BusinessRuleException`). `getAllBookings/getBookingsByCustomer` **tanpa** detail alat; `getBookingDetail/getBookingForCustomer/getBookingInstruments` memuat detail. `getBookingStatusSummary` selalu memuat 4 status (EnumMap).
- **BR-22:** hapus ditolak lewat error MySQL 1451/1217 (FK RESTRICT) → `BusinessRuleException`; pesan Room/Instrument menyarankan MAINTENANCE. Email duplikat (1062) pada balapan → `BusinessRuleException`.
- `BookingService` punya konstruktor injeksi 5 DAO (dipakai tes rollback); Service lain juga punya konstruktor injeksi DAO.

## 5. Terbuka / perlu konfirmasi tim
- A-01 (tarif alat per unit per sesi), A-03 (pembatalan oleh Customer; tidak ada di v1), A-04 (tabel transisi), A-05 (durasi jam bulat) — default dokumen berlaku.
- **Harga Room tidak di-snapshot** (skema tidak punya kolomnya): total yang dihitung ulang setelah ubah/hapus detail memakai harga Room *saat ini*, sehingga mengubah harga Room lalu mengedit detail booking lama ikut mengubah komponen ruangan. Sesuai rumus kontrak; alternatif (pertahankan komponen ruangan lama) bisa dipilih tim.
- A-02 (tidak ada stok alat) tetap keterbatasan yang diterima.
- **STEP 6 — keputusan GUI D-GUI-01…D-GUI-11 (usulan, lihat `docs/GUI_CONTRACT.md` §1; belum di Decision Log).** Terpenting: **D-GUI-01** — landing/katalog/cari jadwal tanpa login, tetapi submit booking wajib login/registrasi Customer (backend butuh `customerId`; tidak ada guest booking). Butuh konfirmasi tim/dosen karena menyimpang dari Spesifikasi GUI §8.1.
- GUI: jam operasional, rentang jam "Cari Jadwal", data kontak studio, ukuran minimum jendela, varian "RENT"/"RENTAL" belum diputuskan (default di kontrak GUI).
- Konflik D-035 vs rencana 4A: `DateTimeUtil/CurrencyUtil/SessionManager` kini dibuat di STEP 7 (hanya dipakai Controller; Service tetap memakai logika waktu privat).

## 6. Hasil verifikasi aktual STEP 5
| Item | Hasil |
|---|---|
| Kompilasi main (model+util+dao+service) | `javac -source 21 -Xlint:all`: bersih, 0 warning (JRE sandbox tanpa `ct.sym`, jadi `--release` tak tersedia) |
| Kompilasi 8 kelas tes | Bersih terhadap **stub** JUnit (JUnit asli tidak tersedia di sandbox) |
| `ServiceValidationTest` (11 tes, tanpa DB) | **11/11 lulus** lewat runner refleksi + stub assertion (bukan JUnit/Maven asli) |
| Tes DB (65 tes: Auth, Customer, Room, Instrument, Booking, BookingInstrument, BookingTransaction) | **Belum dijalankan** (tidak ada MySQL/Maven di sandbox). Kebenarannya belum terbukti sampai `mvn clean test` lokal |
| `mvn clean test` | **Belum dijalankan oleh Claude.** Harapan lokal: 110 + 76 = **186 tes**, 0 gagal; tes DB otomatis di-skip bila DB/seed tidak siap |
| Kemurnian | Tidak ada `javafx`/SQL mentah di `service/`; semua DB lewat DAO + `DatabaseConnection` untuk transaksi |

Peta AC → tes: R01/R02/R18 `ServiceValidationTest`; R03, R05–R09, R11, R12, R14 `BookingServiceTest`; R04, R10, R15, R16 `BookingInstrumentServiceTest` (+ validasi qty di `ServiceValidationTest`); R13 `BookingTransactionTest`; R17 `AuthServiceTest`; R19 `CustomerServiceTest`/`RoomServiceTest`/`InstrumentServiceTest`. AC-C01…C04 tercakup di `BookingInstrumentServiceTest`. Serialisasi pemesan bersamaan (D-019): `BookingServiceTest.concurrentRequestsForSameSlotYieldOneBooking`.

## 7. Pekerjaan belum selesai
1. **Jalankan `mvn clean test` lokal** (MySQL aktif, `config/db.properties` terisi, DB dari `reset.sql → schema.sql → seed.sql`); laporkan jumlah tes/gagal/skip. Tes Service memakai data berawalan `svc-test-`/`SvcTest `/`svctest_` dan membersihkannya sendiri; tidak memakai `reset.sql`.
2. Koordinator: sinkronkan `README.md`, `PROGRESS.md`, `docs/DECISION_LOG.md` (D-036…D-039 dari 4A/4B + usulan §4), `docs/ACCEPTANCE_CRITERIA.md` (status AC-R, AC-A01…A03) — tidak disentuh pada STEP 5 sesuai batasan.
3. STEP 8–9: integrasi/perbaikan, README/screenshot/laporan/ZIP.
4. Konfirmasi A-01/A-03/A-04/A-05; screenshot starter.

## 8. Catatan untuk STEP 7
Baca `docs/GUI_CONTRACT.md` (fx:id, handler, tipe, alur). Controller hanya memanggil Service (tanpa SQL/`Connection`); tangkap `ValidationException`/`BusinessRuleException` → tampilkan `getMessage()`; `ServiceException` biasa → pesan umum. Customer yang login tidak membawa hash. Pilihan ruangan: `RoomService.getBookableRooms()`; alat: `InstrumentService.getSelectableInstruments()`.

## 9. Hasil verifikasi aktual STEP 6
| Item | Hasil |
|---|---|
| Lima FXML | Well-formed (parser XML); tidak ada `fx:id` ganda per berkas; seluruh `fx:id`/handler terdaftar di `GUI_CONTRACT.md` (dihasilkan dari parsing FXML) |
| Pemuatan FXML di JavaFX / tampilan visual | **Belum dicoba** (JavaFX tak tersedia di sandbox; controller belum ada). Kebenaran sintaks CSS JavaFX dan tampilan belum terbukti |
| `mvn` | Tidak dijalankan (resource saja; tidak ada Java baru). Bila FXML/CSS bermasalah, galat muncul saat runtime STEP 7, bukan saat kompilasi |
| Aset | Tidak ada foto/font lokal; hero = gradient; font = `"Poppins"`/`"Roboto"` dengan fallback default sistem |

## 10. STEP 7 — perilaku Controller yang dikunci (usulan D-CTL-xx; belum di Decision Log)
- **Navigasi:** `Navigator.showLanding/Login/Register/CustomerDashboard/AdminDashboard`; dashboard yang diminta tanpa peran yang sesuai dialihkan ke Login (guard ganda: `Navigator` + `initialize()` controller → `Platform.runLater(showLogin)`). `showLanding()` membuang jadwal pilihan yang belum terpakai.
- **D-GUI-01:** slot jadwal dipilih di Landing → bila Customer login: dashboard tab Booking terisi (tanggal, jam, ruangan); bila belum login: `Navigator.setPendingPrefill` + Login (pesan info); Admin: peringatan (Admin tidak membuat booking). Setelah registrasi sukses → Login dengan pesan sukses.
- **Login (D-GUI-05):** identifier berisi `@` → `loginCustomer`, selain itu `loginAdmin`. `SessionManager.login` mengosongkan `passwordHash`; password tidak disimpan di mana pun.
- **Error:** `ValidationException`/`BusinessRuleException` → `getMessage()`; `ServiceException`/`RuntimeException` lain → "Terjadi kesalahan pada sistem. Silakan coba lagi." (detail di `java.util.logging`).
- **Landing Cari Jadwal (D-GUI-04):** durasi 1–8 jam (default 2); untuk tiap ruangan AVAILABLE berjenis dipilih dan tiap jam mulai 00:00 s/d (23:00 − durasi) memanggil `BookingService.isRoomAvailable`; `BusinessRuleException` (ruangan jadi MAINTENANCE) dilewati, galat lain menghentikan pencarian. Berjalan sinkron di thread UI (≤ sekitar 5 ruangan × 22 jam kueri). Konstanta `FIRST_START_HOUR`/`LAST_END_HOUR` di `LandingController` dan `CustomerDashboardController` menunggu keputusan jam operasional.
- **Customer:** ComboBox jam bulat 00:00–22:00 (mulai) / 01:00–23:00 (selesai); ruangan dari `getBookableRooms`, instrumen dari `getSelectableInstruments` (dimuat ulang saat tab Booking dibuka); instrumen sama digabung jumlahnya (maks 99, UX); estimasi via `estimateTotalCost`; sukses booking → form direset, Riwayat dimuat ulang, pesan memuat nomor/status/total; riwayat via `getBookingsByCustomer`, detail via `getBookingForCustomer` (BR-16).
- **Admin:** ringkasan (`getBookingStatusSummary` + 3 `count*`); filter status di controller; pilihan status tujuan hanya yang lolos `BookingStatus.canTransitionTo`; tombol detail alat dinonaktifkan untuk status selain PENDING/CONFIRMED (UX, BR-19 tetap di Service); konfirmasi `Alert` untuk ubah status, hapus detail, dan hapus Customer/Ruangan/Instrumen; harga dibaca dengan pola `\d+([.,]\d{1,2})?` — "150.000" **ditolak** (agar tidak terbaca 150), pesan menyuruh menulis tanpa pemisah ribuan.
- **Format:** `CurrencyUtil.formatRupiah` → `Rp 150.000` (sen hanya bila ≠ 0: `Rp 150.000,50`; `null` → `Rp 0`); `DateTimeUtil.formatDate` → `dd MMM yyyy` locale `id-ID`; `formatTime` → `HH:mm`; `hoursBetween` membuang menit; null → `IllegalArgumentException` (formatDate/Time → `-`).
- **Hero:** `LandingController` memakai `/com/studiomusik/images/hero.jpg` bila ada; tidak ada aset → gradient. Pemuatan font belum dilakukan (tidak ada file TTF).

## 11. Hasil verifikasi aktual STEP 7
| Item | Hasil |
|---|---|
| Kecocokan FXML ↔ controller | Skrip: 200 `fx:id` (33/7/9/59/92), 44 handler; **tidak ada** field/handler hilang atau berlebih; tipe field = tipe elemen FXML (0 ketidaksesuaian). Kontrak FXML tidak diubah |
| Util baru + model | `javac -Xlint:all`: bersih, 0 warning |
| Controller/Navigator/MainApp | Pemeriksaan sintaks `javac` tanpa JavaFX: tidak ada galat selain simbol `javafx.*` yang memang tidak tersedia di sandbox. **Kompilasi penuh, pemuatan FXML, dan tampilan belum terbukti** |
| Kemurnian layer | Tidak ada `java.sql`/`Connection`/DAO/SQL di controller |
| Tes | Tidak dijalankan dan tidak ada tes baru (sesuai instruksi; tim menguji sendiri) |

## 12. Terbuka setelah STEP 7
1. Kompilasi + jalankan aplikasi lokal; kemungkinan galat runtime FXML/CSS/controller baru muncul di sini (belum pernah dimuat).
2. Sinkronkan dokumen: `GUI_CONTRACT.md` (status header, nama controller), `ARCHITECTURE_CONTRACT` §3, `DECISION_LOG.md` (D-GUI-01…11, D-CTL di §10), `README.md`.
3. Tetap menunggu konfirmasi: D-GUI-01 (dosen/tim), jam operasional, data kontak studio, ukuran minimum jendela, A-01/A-03/A-04/A-05.
4. Keterbatasan UX yang diketahui: pencarian jadwal sinkron; slot hari ini tidak menyaring jam yang sudah lewat (Service menerima hari ini); tidak ada pemuatan font; `main-view.fxml` tidak lagi dimuat.
