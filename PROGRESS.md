# PROGRESS — Sistem Pengelolaan Operasional Studio Musik

Ringkasan log tahap. Handoff lengkap lintas akun: **`PROGRESS_HANDOFF.md`** (satu-satunya dokumen handoff hidup).

| Tahap | Tanggal | Status | Bukti |
|---|---|---|---|
| STEP 0 — Final Specification & Contract | 2026-11-18 | **Selesai** | `docs/*.md`; pemeriksaan konsistensi (22 BR, rujukan A/D/BR lengkap, versi pom = dokumen) |
| STEP 1 — Maven Project Setup | 2026-11-18 (file), 2026-11-19 (verifikasi lokal) | **Selesai** | Screenshot lokal: `mvn clean test` → 5 tes lulus, `mvn package` → BUILD SUCCESS (`docs/STAGE1_VERIFICATION.md` §0). `javafx:run` dilaporkan pengguna berhasil; tombol belum dikonfirmasi |
| STEP 2 — Database Schema & Seed | 2026-11-19 | **Selesai** (diverifikasi di sandbox; verifikasi lokal dianjurkan) | MySQL 8.0.46: 57/57 uji lulus; siklus reset → schema → seed lulus (`docs/STAGE2_VERIFICATION.md`) |
| STEP 3 — Model & Enum | — | Belum dimulai | — |

## Ringkasan STEP 2 (aktual)
- Dibuat: `database/schema.sql`, `seed.sql`, `reset.sql`, `database/tools/Pbkdf2HashGenerator.java`, `database/tests/verify_stage2.sql`, `docs/STAGE2_VERIFICATION.md`.
- Diubah: `config/db.properties.example`, `README.md`, `docs/DECISION_LOG.md` (D-023…D-028), `docs/DATABASE_CONTRACT.md` (§8), `docs/ARCHITECTURE_CONTRACT.md` (struktur `database/`), `docs/STAGE1_VERIFICATION.md`, `PROGRESS.md`, `PROGRESS_HANDOFF.md`.
- Tidak diubah: seluruh kode STEP 1 dan `pom.xml`.
- AC-D01…AC-D05: lulus di MySQL 8.0.46 (sandbox).

## Belum selesai / terbuka
1. Verifikasi database di MySQL **lokal** pengguna (wajib sebelum STEP 4 / DAO): `docs/STAGE2_VERIFICATION.md` §7.
2. Screenshot jendela starter dan konfirmasi tombol "Uji Interaksi".
3. Konfirmasi tim: A-01 (tarif alat per unit per sesi), A-03 (pembatalan oleh Customer), A-04 (tabel transisi status), A-05 (durasi jam bulat).
4. Koneksi JDBC dari Java belum diuji (STEP 4).
