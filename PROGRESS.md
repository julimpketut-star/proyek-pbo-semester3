# PROGRESS — Sistem Pengelolaan Operasional Studio Musik

Ringkasan log tahap. Handoff lengkap lintas akun: **`PROGRESS_HANDOFF.md`** (satu-satunya dokumen handoff hidup).

| Tahap | Tanggal | Status | Bukti |
|---|---|---|---|
| STEP 0 — Final Specification & Contract | 2026-11-18 | **Selesai** | `docs/*.md`; pemeriksaan konsistensi |
| STEP 1 — Maven Project Setup | 2026-11-18 (file), 2026-11-19 (verifikasi lokal) | **Selesai** | Screenshot lokal: `mvn clean test` (5 tes) dan `mvn package` BUILD SUCCESS. `javafx:run` dilaporkan berhasil; tombol belum dikonfirmasi |
| STEP 2 — Database Schema & Seed | 2026-11-19 | **Selesai** (sandbox; verifikasi lokal dianjurkan) | MySQL 8.0.46: 57/57 uji lulus (`docs/STAGE2_VERIFICATION.md`) |
| STEP 3 — Model & Enum | 2026-11-19 | **Selesai di sandbox; `mvn clean test` lokal menunggu pengguna** | javac `-Xlint:all` bersih; 79/79 tes lulus via JUnit console (74 baru); 12 dari 13 mutasi terdeteksi, 1 mutan setara (`docs/STAGE3_VERIFICATION.md`) |
| STEP 4 — Util JDBC & DAO | — | Belum dimulai | — |

## Ringkasan STEP 3 (aktual)
- **Dibuat (main):** `model/enums/{BookingStatus,RoomStatus,InstrumentStatus}.java`; `model/{Person,Customer,Admin,Room,Instrument,BookingInstrument,Booking}.java`.
- **Dibuat (test):** `model/{Fixtures,PersonHierarchyTest,RoomTest,InstrumentTest,BookingInstrumentTest,BookingTest,OopConceptsTest}.java`, `model/enums/BookingStatusTest.java` (74 tes baru).
- **Dibuat (docs):** `docs/STAGE3_VERIFICATION.md`.
- **Diubah:** `model/package-info.java`, `model/enums/package-info.java` (teks status), `docs/DECISION_LOG.md` (D-029…D-035), `docs/API_CONTRACTS.md` (§1.4, aditif), `docs/ACCEPTANCE_CRITERIA.md` (status AC-M + tabel 7 konsep PBO), `README.md`, `PROGRESS.md`, `PROGRESS_HANDOFF.md`.
- **Tidak diubah:** `pom.xml`, `database/*.sql`, kode STEP 1, `AppInfoTest`.
- AC-M01…AC-M05: lulus di sandbox; menunggu konfirmasi `mvn clean test` lokal.

## Belum selesai / terbuka
1. **`mvn clean test` lokal** harus menunjukkan 79 tes, 0 gagal (`docs/STAGE3_VERIFICATION.md` §6).
2. **Verifikasi MySQL lokal** (`docs/STAGE2_VERIFICATION.md` §7) — wajib sebelum STEP 4.
3. Screenshot jendela starter dan konfirmasi tombol "Uji Interaksi".
4. Konfirmasi tim: A-01 (tarif alat per unit per sesi), A-03 (pembatalan oleh Customer), A-04 (tabel transisi status), A-05 (durasi jam bulat).
5. Koneksi JDBC dari Java dan DAO belum ada (STEP 4).
