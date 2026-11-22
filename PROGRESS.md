# PROGRESS — Sistem Pengelolaan Operasional Studio Musik

Ringkasan log tahap. Handoff lengkap lintas akun: **`PROGRESS_HANDOFF.md`** (satu-satunya dokumen handoff hidup). Diperbarui sampai **STEP 7** (2026-11-22).

| Tahap | Tanggal | Status | Bukti |
|---|---|---|---|
| STEP 0 — Final Specification & Contract | 2026-11-18 | **Selesai** | `docs/*.md`; pemeriksaan konsistensi |
| STEP 1 — Maven Project Setup | 2026-11-18 (file), 2026-11-19 (verifikasi lokal) | **Selesai** | Lokal: `mvn clean test` (5 tes) dan `mvn package` BUILD SUCCESS |
| STEP 2 — Database Schema & Seed | 2026-11-19 | **Selesai** | MySQL 8.0.46: 57/57 uji (`docs/STAGE2_VERIFICATION.md`) |
| STEP 3 — Model & Enum | 2026-11-19 | **Selesai** | 79 tes; 12/13 mutasi terdeteksi, 1 setara (`docs/STAGE3_VERIFICATION.md`) |
| STEP 4A/4B — Util JDBC & 6 DAO | — | **Selesai** | Dilaporkan pengguna: `mvn clean test` = 110 tes, 0 gagal, 0 error, 2 skip, BUILD SUCCESS |
| STEP 5 — Service + exception | 2026-11-22 | **Kode selesai; tes DB belum dijalankan** | Kompilasi bersih; 11/11 tes tanpa DB lulus via runner refleksi; 65 tes DB menunggu `mvn clean test` lokal (harapan 186 tes) |
| STEP 6 — GUI: FXML & CSS | 2026-11-22 | **File selesai; belum dimuat/dilihat di JavaFX** | 5 FXML well-formed, `fx:id`/handler konsisten dengan `docs/GUI_CONTRACT.md`; belum ada screenshot |
| STEP 7 — Controller, Session & Navigasi | 2026-11-22 | **Kode selesai; belum dikompilasi/dijalankan dengan JavaFX** | 5 controller + 3 Util + `Navigator`/`UiSupport` + `MainApp`; `fx:id`/tipe/handler 5 FXML cocok 100% dengan controller (dicek skrip); Util lolos `javac -Xlint:all`; controller lolos pemeriksaan sintaks (JavaFX tidak tersedia di sandbox) |

## Ringkasan STEP 6 (aktual)
- **Dibuat:** `src/main/resources/com/studiomusik/view/{landing,login,register,customer-dashboard,admin-dashboard}-view.fxml`; `docs/GUI_CONTRACT.md` (fx:controller, 200 fx:id, 44 handler, keputusan D-GUI-01…11).
- **Diubah:** `css/app.css` (palet SONORA, tipografi Poppins/Roboto dengan fallback, komponen, tabel, tab, lencana status; kelas starter dipertahankan), `PROGRESS.md`, `PROGRESS_HANDOFF.md`.
- **Dipertahankan:** `view/main-view.fxml`, `MainController`, seluruh Java/SQL/`pom.xml` (tidak diubah; tanpa dependency baru; tanpa kelas Controller baru).
- **Deviasi utama (D-GUI-01):** browsing & pencarian jadwal tanpa login, tetapi submit booking wajib login/registrasi Customer karena backend memerlukan `customerId`; tidak ada guest booking.
- **Aset:** tidak ada foto/font lokal → hero gradient, font fallback sistem.

## Ringkasan STEP 7 (aktual)
- **Dibuat (Util):** `util/SessionManager` (singleton; `login` mengosongkan `passwordHash` pada objek), `util/DateTimeUtil`, `util/CurrencyUtil` sesuai `API_CONTRACTS` §2.
- **Dibuat (controller):** `LandingController`, `LoginController`, `RegisterController`, `CustomerDashboardController`, `AdminDashboardController`; helper `Navigator` (public; navigasi + data sementara antar-halaman) dan `UiSupport` (package-private; pesan, kolom tabel, lencana, parser input).
- **Diubah:** `MainApp.java` (satu Scene 1180×760, min 960×640, `app.css`, membuka Landing via `Navigator`), `PROGRESS.md`, `PROGRESS_HANDOFF.md`.
- **Tidak diubah:** FXML, CSS, `docs/GUI_CONTRACT.md`, model/DAO/service, `pom.xml`, database, `MainController`/`main-view.fxml` (referensi starter, tidak dimuat lagi).
- **Alur D-GUI-01 dipertahankan:** browsing & cari jadwal tanpa login; memilih slot tanpa login → Login (jadwal disimpan di `Navigator`, diisikan ke form Booking setelah login Customer); submit booking memanggil `BookingService.createBooking` dengan `customerId` dari `SessionManager`.

## Belum selesai / terbuka
1. **`mvn clean test` lokal** untuk STEP 5 (harapan 186 tes, 0 gagal) — lihat handoff §7.
2. Menjalankan aplikasi (FXML/CSS/controller belum pernah dimuat di JavaFX oleh Claude); screenshot starter dan GUI.
3. Sinkronisasi `README.md`, `docs/DECISION_LOG.md` (D-036…D-040, D-GUI-01…11), `docs/ACCEPTANCE_CRITERIA.md`, `ARCHITECTURE_CONTRACT.md` §3 (nama controller).
4. Konfirmasi tim: A-01, A-03, A-04, A-05; D-GUI-01; jam operasional; data kontak studio.
5. Selesai di STEP 7: `SessionManager`, `DateTimeUtil`, `CurrencyUtil`. Sinkronkan juga `GUI_CONTRACT.md` (status header masih "Controller belum dibuat") dan `ARCHITECTURE_CONTRACT` §3 (nama controller aktual = 5 kelas + `Navigator`).
