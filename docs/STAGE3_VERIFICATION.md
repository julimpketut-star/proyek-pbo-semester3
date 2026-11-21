# STAGE 3 VERIFICATION — Model & Enum

Tanggal: 2026-11-19 · Lingkungan: sandbox Linux milik Claude (JDK 21.0.13; Maven 3.8.7 tanpa akses Maven Central) · Branch kerja pengguna: `feature/database`

## 1. Kesimpulan jujur

| Item | Status |
|---|---|
| `mvn clean test` | **Dijalankan di sandbox dan GAGAL karena jaringan** (`maven-clean-plugin:2.5` tidak dapat diunduh dari Maven Central). Bukan kesalahan proyek. **Belum ada hasil Maven.** |
| Kompilasi `javac --release 21 -Xlint:all` (model + enum + `AppInfo` + semua tes) | **Sukses, tanpa warning** |
| Tes via JUnit Platform console (pengganti sementara Maven) | **79 ditemukan, 79 berhasil, 0 gagal, 0 dilewati, 0 dibatalkan** (74 tes baru + 5 `AppInfoTest`) |
| Uji mutasi (13 kerusakan sengaja pada kode) | **12 terdeteksi**, 1 mutan setara (bukan celah tes) |
| `mvn clean test` di komputer pengguna | **Menunggu pengguna** (bagian 6) |

"Lulus" di AC-M di bawah berarti lulus pada jalur javac + JUnit console di sandbox. Perbedaan dengan Maven: versi JUnit di sandbox 5.10.1 (platform console 1.9.1, dari apt), sedangkan proyek mengunci 5.10.2; package model tidak memakai JavaFX sehingga tidak bergantung pada JavaFX 21.

Catatan gerbang awal: placeholder hasil lokal pada prompt STEP 3 (`mvn clean test`, verifikasi MySQL lokal) tidak terisi. Dipakai bukti sebelumnya: `mvn clean test` lokal STEP 1 (5 tes lulus, 2026-11-19). Verifikasi MySQL lokal masih **belum dilaporkan** dan tetap wajib sebelum STEP 4.

## 2. Perintah yang benar-benar dijalankan dan hasilnya

| Perintah / pemeriksaan | Hasil |
|---|---|
| `javac --release 21 -Xlint:all` untuk 12 file di `model/` dan `model/enums/` | exit 0, tanpa warning |
| `javac` untuk seluruh `src/test/java` (8 kelas tes) | exit 0 |
| JUnit console `--scan-classpath` | 79 tes: 79 berhasil, 0 gagal |
| Pencarian teks `java.sql`, `javafx`, `jdbc`, `java.io`, `double`, `float` di `src/main/java/com/studiomusik/model` | **Tidak ada temuan** (model murni: tanpa SQL/JDBC/JavaFX/`double`/`float`) |
| `mvn -B clean test` | **GAGAL** (`BUILD FAILURE`, plugin tidak dapat diunduh karena jaringan sandbox) |
| `pom.xml`, `schema.sql`, `seed.sql`, `reset.sql` dibanding ZIP STEP 2 | **Identik** (tidak diubah) |

### Jumlah tes per kelas
| Kelas tes | Tes |
|---|---|
| `AppInfoTest` (STEP 1) | 5 |
| `model/PersonHierarchyTest` | 8 |
| `model/enums/BookingStatusTest` | 6 |
| `model/RoomTest` | 8 |
| `model/InstrumentTest` | 7 |
| `model/BookingInstrumentTest` | 8 |
| `model/BookingTest` | 28 |
| `model/OopConceptsTest` | 9 |
| **Total** | **79** |

### Cakupan sesuai permintaan
Person abstract; `getRole()` polimorfik lewat referensi `Person`; contoh biaya Rp100.000/jam × 2 jam + 2 instrumen × Rp25.000 = **Rp250.000,00**; BigDecimal dan HALF_UP skala 2 (termasuk kasus `0.005→0.01`, `1.005→1.01`, `33.333×3→100.00`); 16 kombinasi transisi status; `blocksSchedule()`; snapshot harga (harga alat naik setelah booking, total tidak berubah); validasi quantity dan instrumen duplikat; Room dan Instrument MAINTENANCE; total yang cocok dengan angka seed booking 1, 4, 5 (400000, 496000, 470000).

## 3. Bukti AC-M01 — `new Person(...)` tidak dapat dikompilasi
```
Coba.java:2: error: Person is abstract; cannot be instantiated
... Person p = new Person(1, "a", "b", "c");
1 error
```

## 4. Uji mutasi (membuktikan tes mendeteksi cacat)
Kode dirusak di salinan sementara, lalu seluruh suite dijalankan. Setiap mutasi harus membuat ≥ 1 tes gagal.

| Mutasi | Tes gagal |
|---|---|
| `blocksSchedule()` ikut memblokir COMPLETED | 1 |
| PENDING boleh langsung ke COMPLETED | 1 |
| `Room.calculateCost` memakai `RoundingMode.DOWN` | 2 |
| `Room.isAvailable()` selalu `true` | 3 |
| Subtotal memakai harga alat terkini, bukan snapshot | 4 |
| `Customer.getRole()` mengembalikan "Admin" | 2 |
| `addInstrument` tidak menolak duplikat | 1 |
| `addInstrument` tidak menolak `quantity < 1` | **0** (mutan setara: konstruktor `BookingInstrument` menegakkan aturan yang sama dan melempar exception yang sama; lihat D-030) |
| `calculateDuration` menerima menit tidak bulat | 1 |
| `getInstruments()` mengembalikan list yang dapat diubah | 3 |
| `setBookingId` tidak menyebar ke detail | 2 |
| Booking baru berstatus CONFIRMED, bukan PENDING | 1 |
| Field `Person.name` dibuat `public` | 1 |

## 5. Status AC-M01 sampai AC-M05

| ID | Status | Catatan |
|---|---|---|
| AC-M01 | **Lulus (sandbox)** | Bukti kompilasi + tes (bagian 3) |
| AC-M02 | **Lulus (sandbox)** | Tes polimorfisme dan override |
| AC-M03 | **Lulus (sandbox)** | Contoh Rp250.000 dan tiga angka seed |
| AC-M04 | **Lulus (sandbox)** | HALF_UP skala 2 pada Room, Instrument, BookingInstrument, Booking |
| AC-M05 | **Lulus (sandbox)** | Tabel tujuh konsep dengan tes perilaku di `docs/ACCEPTANCE_CRITERIA.md` |

Semua berstatus "menunggu konfirmasi `mvn clean test` lokal".

## 6. Menjalankan pengujian lokal (VS Code, PowerShell)
Dari root `studio-musik\`:
```powershell
mvn clean test
```
Ekspektasi: `Tests run: 79, Failures: 0, Errors: 0, Skipped: 0` dan `BUILD SUCCESS` (8 kelas tes; ringkasan per kelas tampil di output Surefire). Tempel hasilnya (atau screenshot) pada percakapan berikutnya. Jika ada tes gagal, salin nama tes dan pesan kesalahannya.

Centang saat selesai:
- [ ] `mvn clean test` → 79 tes, 0 gagal
- [ ] Verifikasi MySQL lokal (`docs/STAGE2_VERIFICATION.md` §7) — wajib sebelum STEP 4

## 7. Keterbatasan dan hal yang belum diuji
- Hasil Maven belum ada (bagian 1). Perbedaan JUnit 5.10.1 vs 5.10.2 tidak diharapkan berpengaruh, tetapi belum dibuktikan.
- Tes model tidak menyentuh database; integrasi DAO ↔ model baru diuji pada STEP 4.
- `calculateDuration()` sengaja melempar exception untuk waktu tidak bulat (D-032); Service harus menyaring input sebelum memanggilnya.
- Aturan yang bergantung pada data lintas objek (overlap jadwal, BR-05, BR-16, BR-21, BR-22) bukan tanggung jawab model dan baru diuji pada STEP 5.
