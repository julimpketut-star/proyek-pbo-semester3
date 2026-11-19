# PROJECT HANDOFF — Sistem Pengelolaan Operasional Studio Musik (PBO, Tugas Kelompok II)

> Versi terkunci setelah STEP 0 (2026-11-18). Dokumen ini adalah konteks proyek yang statis.
> Status terkini ada di `PROGRESS_HANDOFF.md` (satu-satunya handoff hidup). Rincian kontrak ada di dokumen `docs/*`.

## 1. Tujuan
Membangun aplikasi desktop **Java 21 + JavaFX (FXML) + MySQL** yang benar-benar berjalan, memenuhi persyaratan wajib TK II PBO (lihat `FINAL_SPECIFICATION.md` §2), dan konsisten dengan rancangan TK I.

## 2. Kelanjutan dari Tugas Kelompok I
TK I menghasilkan: latar belakang, use case, class diagram, flow aplikasi, pilihan JavaFX, konsep PBO, PDM/ADM, desain CRUD M:N. TK II: implementasi, integrasi database, GUI, pengujian, dokumentasi akhir. Revisi yang sudah menjadi arah desain: **Customer Self-Service** (D-001).

## 3. Anggota Kelompok 1 (Kelas D)
I Gede Gaura Krishna (2508561064), I Dewa Gede Putra Sugiatnyana (2508561067), I Made Ari Andika Putra (2508561073), Jire Immanuel Nababan (2508561074), Ketut Juli Mahaputra (2508561078).

## 4. Ringkasan keputusan terkunci
- Dua aktor: Customer (register, login, lihat ruangan/alat, cek ketersediaan, estimasi biaya, booking, riwayat sendiri) dan Admin (login, dashboard, CRUD Customer/Room/Instrument, lihat semua booking, kelola status, kelola detail alat).
- Aplikasi desktop, bukan website; satu studio.
- Class: `Person` (abstract), `Customer`, `Admin`, `Booking`, `Room`, `Instrument`, `BookingInstrument`; enum `BookingStatus`, `RoomStatus`, `InstrumentStatus`.
- Enam tabel: `admin`, `customer`, `room`, `instrument`, `booking`, `booking_instrument` (`password_hash`, `customer.email` unik).
- Aturan bisnis BR-01…BR-22 di `FINAL_SPECIFICATION.md`; asumsi A-01…A-11; keputusan D-001…D-022 di `DECISION_LOG.md`.
- Arsitektur View → Controller → Service → DAO → MySQL; controller tanpa SQL.
- Dependency: Java 21, JavaFX 21.0.5, MySQL Connector/J 8.4.0, JUnit Jupiter 5.10.2.

## 5. Hal yang masih terbuka (perlu konfirmasi tim)
A-01 (tarif alat per unit per sesi), A-03 (pembatalan oleh Customer), A-04 (tabel transisi status), A-05 (durasi jam bulat). Jawaban dicatat sebagai entri baru di `DECISION_LOG.md`.

## 6. Aturan melanjutkan percakapan (wajib)
1. Baca `PROGRESS_HANDOFF.md` lalu dokumen ini.
2. Periksa file proyek yang benar-benar diunggah; jangan mengasumsikan sesuatu selesai berdasarkan catatan saja.
3. Identifikasi tahap terakhir yang **terverifikasi**.
4. Jangan membuat ulang file yang sudah benar; jangan mengubah nama package/class/tabel/kolom/method tanpa alasan tercatat.
5. Jika ada konflik, laporkan file dan masalahnya sebelum mengubah.
6. Kerjakan hanya tahap berikutnya; hasilkan file lengkap dengan instruksi penempatan, command, dan checklist verifikasi.
7. Perbarui `PROGRESS_HANDOFF.md` (dan `PROGRESS.md`) di akhir tahap. Jangan membuat file handoff lain.
8. Jangan mengarang hasil pengujian atau mengklaim fitur selesai tanpa bukti.

## 7. Tahap pengerjaan
0 Spesifikasi final · 1 Scaffold Maven · 2 Skema SQL & seed · 3 Model & enum · 4 JDBC util & DAO · 5 Service · 6 FXML & CSS · 7 Controller & navigasi · 8 Integrasi & pengujian · 9 README, screenshot, laporan, ZIP.
