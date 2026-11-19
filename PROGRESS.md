# PROGRESS — Sistem Pengelolaan Operasional Studio Musik

Ringkasan log tahap. Handoff lengkap lintas akun: **`PROGRESS_HANDOFF.md`** (satu-satunya dokumen handoff hidup).

| Tahap | Tanggal | Status | Bukti |
|---|---|---|---|
| STEP 0 — Final Specification & Contract | 2026-11-18 | **Selesai** (dokumen dibuat, konsistensi diperiksa) | `docs/*.md`, pemeriksaan konsistensi (22 BR terdefinisi, tidak ada rujukan BR/A yang menggantung, versi pom = dokumen) |
| STEP 1 — Maven Project Setup | 2026-11-18 | **File dibuat; verifikasi Maven BELUM berhasil di sandbox** (tidak ada akses Maven Central) | `docs/STAGE1_VERIFICATION.md` |
| STEP 2 — Database Schema & Seed | — | Belum dimulai | — |

## Verifikasi STEP 1 (ringkas, aktual)
- Java 21.0.13 dan javac 21.0.13 terdeteksi; Maven 3.8.7 terpasang di sandbox.
- `mvn clean`, `mvn test`, `mvn package`, `mvn clean javafx:run`: **gagal** karena *Unknown host repo.maven.apache.org* (bukan error proyek).
- Parsial: `javac --release 21` sukses; 5 unit test lulus lewat JUnit console (bukan Maven); starter GUI berjalan di Xvfb dengan JavaFX 11 dan tombol mengubah label status.
- **Yang harus dilakukan pengguna:** jalankan perintah Maven di lokal (daftar di `docs/STAGE1_VERIFICATION.md` §4) sebelum memulai STEP 2.

## Pertanyaan terbuka untuk tim
A-01 tarif alat per unit per sesi · A-03 pembatalan oleh Customer · A-04 tabel transisi status · A-05 durasi jam bulat.
