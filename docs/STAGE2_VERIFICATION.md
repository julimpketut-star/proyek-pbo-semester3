# STAGE 2 VERIFICATION — Database Schema, Seed & Konfigurasi

Tanggal: 2026-11-19 · Branch kerja pengguna: `feature/database`

## 1. Kesimpulan

| Item | Status |
|---|---|
| Uji database di MySQL **8.0.46** (sandbox Claude, Ubuntu 24.04) | **Lulus: 57 dari 57 uji** |
| Siklus ulang `reset` → `schema` → `seed` | **Lulus** (dijalankan beberapa kali) |
| Verifikasi pada MySQL **lokal pengguna (Windows)** | **Belum dijalankan** — dianjurkan sekali sebelum STEP 3 (bagian 6) |
| Koneksi JDBC dari Java ke database | **Belum diuji** (kelas koneksi baru dibuat pada STEP 4) |

Seluruh klaim "lulus" di bawah berasal dari perintah yang benar-benar dijalankan; keluaran mentahnya dapat dihasilkan ulang dengan `database/tests/verify_stage2.sql`.

## 2. Lingkungan uji
- MySQL Server `8.0.46-0ubuntu0.24.04.5` (paket apt), engine InnoDB, charset server `utf8mb4`.
- `sql_mode` bawaan (strict): `ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION`.
- JDK 21.0.13 (untuk `Pbkdf2HashGenerator`).
- Perbedaan dengan lingkungan pengguna: OS (Linux vs Windows) dan build MySQL. Perilaku SQL yang diuji (constraint, FK, ENUM, CHECK) sama untuk MySQL 8.0.16+.

## 3. File yang diverifikasi
`database/schema.sql`, `database/seed.sql`, `database/reset.sql`, `database/tools/Pbkdf2HashGenerator.java`, `database/tests/verify_stage2.sql`, `config/db.properties.example`.

## 4. Hasil siklus penggunaan ulang script (dijalankan)

| Langkah | Hasil |
|---|---|
| `reset.sql` | Sukses; jumlah tabel di `studio_musik` menjadi **0** |
| `schema.sql` (pertama) | Sukses; **6** tabel |
| `seed.sql` | Sukses; baris admin/customer/room/instrument/booking/booking_instrument = **1/6/5/7/7/12** |
| `schema.sql` dijalankan lagi saat data ada | Sukses tanpa error; data **utuh** (booking tetap 7) |
| `seed.sql` dijalankan lagi tanpa reset | **Gagal sesuai desain** (`ERROR 1062 ... Duplicate entry '1' for key 'admin.PRIMARY'`); data tetap utuh (booking 7, customer 6) |
| `reset.sql` → `schema.sql` → `seed.sql` (siklus penuh) | Sukses; 1/6/5/7/7/12 |
| `verify_stage2.sql` setelah siklus penuh | **57 LULUS, 0 GAGAL** |
| Pemanggilan gaya PowerShell: `mysql -e "source database/xxx.sql"` | Sukses untuk reset, schema, seed, dan verify (diuji di Linux; PowerShell tidak mendukung operator `<`) |
| Setelah verifikasi: reset → schema → seed | `AUTO_INCREMENT` kembali bersih (admin 2, customer 7, room 6, instrument 8, booking 8, booking_instrument 13); database sementara `studio_musik_verify_tmp` tidak tersisa |

## 5. Hasil verifikasi per uji (57 uji, keluaran `verify_stage2.sql`)

#### A. Struktur (AC-D01, AC-D02)

| No | Uji | Diharapkan | Aktual | Hasil |
|---|---|---|---|---|
| A01 | jumlah tabel tepat 6 | 6 | 6 | **LULUS** |
| A02 | nama enam tabel | admin,booking,booking_instrument,customer,instrument,room | admin,booking,booking_instrument,customer,instrument,room | **LULUS** |
| A03 | semua tabel InnoDB + utf8mb4 | 0 | 0 | **LULUS** |
| A04 | setiap tabel punya PRIMARY KEY | 6 | 6 | **LULUS** |
| A05 | FK dan aturan ON DELETE | fk_bi_booking:CASCADE,fk_bi_instrument:RESTRICT,fk_booking_customer:RESTRICT,fk_booking_room:RESTRICT | fk_bi_booking:CASCADE,fk_bi_instrument:RESTRICT,fk_booking_customer:RESTRICT,fk_booking_room:RESTRICT | **LULUS** |
| A06 | UNIQUE (booking_id, instrument_id) | booking_id,instrument_id | booking_id,instrument_id | **LULUS** |
| A07 | UNIQUE customer.email | email | email | **LULUS** |
| A08 | UNIQUE admin.username | username | username | **LULUS** |
| A09 | jumlah CHECK constraint | 6 | 6 | **LULUS** |
| A10 | indeks booking(room_id, booking_date, status) | room_id,booking_date,status | room_id,booking_date,status | **LULUS** |
| A11 | kolom uang bertipe DECIMAL(10,2) | 4 | 4 | **LULUS** |
| A12 | tidak ada kolom bernama password (plaintext) | 0 | 0 | **LULUS** |
| A13 | password_hash VARCHAR(255) NOT NULL di admin & customer | 2 | 2 | **LULUS** |
| A14 | booking.created_at NOT NULL DEFAULT CURRENT_TIMESTAMP | 1 | 1 | **LULUS** |

#### B. Seed (AC-D03)

| No | Uji | Diharapkan | Aktual | Hasil |
|---|---|---|---|---|
| B01 | jumlah baris admin/customer/room/instrument/booking/booking_instrument | 1/6/5/7/7/12 | 1/6/5/7/7/12 | **LULUS** |
| B02 | semua password_hash berformat pbkdf2_sha256$iter$salt$hash | 0 | 0 | **LULUS** |
| B03 | setiap akun punya hash/salt berbeda | 7 | 7 | **LULUS** |
| B04 | seed memuat keempat status booking | CANCELLED,COMPLETED,CONFIRMED,PENDING | CANCELLED,COMPLETED,CONFIRMED,PENDING | **LULUS** |
| B05 | ada Room dan Instrument MAINTENANCE | 1/1 | 1/1 | **LULUS** |
| B06 | total_cost seed = harga ruangan x jam + subtotal alat (BR-09/10/11) | 0 | 0 | **LULUS** |
| B07 | snapshot harga alat berbeda dari harga instrument sekarang (BR-12) | 8000.00 vs 10000.00 | 8000.00 vs 10000.00 | **LULUS** |
| B08 | M:N dua arah: instrument 1 dan 5 dipakai 3 booking masing-masing | 3-3 | 3-3 | **LULUS** |
| B09 | Read M:N: JOIN booking+booking_instrument+instrument (booking 5) | Drum Set Pearl x1,Gitar Elektrik Fender x1,Bass Elektrik Ibanez x1,Mikrofon Shure SM58 x1 | Drum Set Pearl x1,Gitar Elektrik Fender x1,Bass Elektrik Ibanez x1,Mikrofon Shure SM58 x1 | **LULUS** |

#### C. Query overlap (BR-07, BR-08)

| No | Uji | Diharapkan | Aktual | Hasil |
|---|---|---|---|---|
| C01 | overlap sebagian 11:00-13:00 | OVERLAP | OVERLAP (bentrok dgn booking 1,2) | **LULUS** |
| C02 | rentang BARU mengandung booking lama 09:00-15:00 | OVERLAP | OVERLAP (bentrok dgn booking 1,2) | **LULUS** |
| C03 | rentang BARU berada di dalam booking lama 10:30-11:30 | OVERLAP | OVERLAP (bentrok dgn booking 1) | **LULUS** |
| C04 | rentang identik 10:00-12:00 (booking 3 CANCELLED tidak dihitung) | OVERLAP | OVERLAP (bentrok dgn booking 1) | **LULUS** |
| C05 | berurutan setelah booking 2: 14:00-16:00 | BEBAS | BEBAS | **LULUS** |
| C06 | berurutan sebelum booking 1: 08:00-10:00 | BEBAS | BEBAS | **LULUS** |
| C07 | booking CANCELLED sendirian (Studio Rekaman 13:00-17:00) | BEBAS | BEBAS | **LULUS** |
| C08 | booking COMPLETED tidak memblokir (Studio A 2026-11-10 10:00-12:00) | BEBAS | BEBAS | **LULUS** |
| C09 | tanggal berbeda, room sama | BEBAS | BEBAS | **LULUS** |
| C10 | room berbeda, tanggal & jam sama | BEBAS | BEBAS | **LULUS** |
| C11 | PENDING memblokir (Studio B 2026-12-02 16:00-17:00) | OVERLAP | OVERLAP (bentrok dgn booking 5) | **LULUS** |

#### D. Data tidak valid harus ditolak (AC-D05 dan constraint lain)

| No | Uji | Diharapkan | Aktual | Hasil |
|---|---|---|---|---|
| D01 | UNIQUE(booking_id,instrument_id): duplikat ditolak | ditolak, error 1062 | error 1062: Duplicate entry '1-1' for key 'booking_instrument.uq_booking_instrument_pair' | **LULUS** |
| D02 | CHECK end_time = start_time ditolak | ditolak, error 3819 | error 3819: Check constraint 'chk_booking_time' is violated. | **LULUS** |
| D03 | CHECK end_time < start_time ditolak | ditolak, error 3819 | error 3819: Check constraint 'chk_booking_time' is violated. | **LULUS** |
| D04 | FK booking.customer_id tidak ada ditolak | ditolak, error 1452 | error 1452: Cannot add or update a child row: a foreign key constraint fails (`studio_musik`.`booking`, CON... | **LULUS** |
| D05 | FK booking.room_id tidak ada ditolak | ditolak, error 1452 | error 1452: Cannot add or update a child row: a foreign key constraint fails (`studio_musik`.`booking`, CON... | **LULUS** |
| D06 | FK booking_instrument.booking_id tidak ada ditolak | ditolak, error 1452 | error 1452: Cannot add or update a child row: a foreign key constraint fails (`studio_musik`.`booking_instr... | **LULUS** |
| D07 | FK booking_instrument.instrument_id tidak ada ditolak | ditolak, error 1452 | error 1452: Cannot add or update a child row: a foreign key constraint fails (`studio_musik`.`booking_instr... | **LULUS** |
| D08 | RESTRICT: hapus customer yang punya booking ditolak (BR-22) | ditolak, error 1451 | error 1451: Cannot delete or update a parent row: a foreign key constraint fails (`studio_musik`.`booking`,... | **LULUS** |
| D09 | RESTRICT: hapus room yang punya booking ditolak (BR-22) | ditolak, error 1451 | error 1451: Cannot delete or update a parent row: a foreign key constraint fails (`studio_musik`.`booking`,... | **LULUS** |
| D10 | RESTRICT: hapus instrument yang dipakai booking ditolak (BR-22) | ditolak, error 1451 | error 1451: Cannot delete or update a parent row: a foreign key constraint fails (`studio_musik`.`booking_i... | **LULUS** |
| D11 | UNIQUE customer.email duplikat ditolak (BR-20) | ditolak, error 1062 | error 1062: Duplicate entry 'ayu.lestari@example.com' for key 'customer.uq_customer_email' | **LULUS** |
| D12 | UNIQUE admin.username duplikat ditolak | ditolak, error 1062 | error 1062: Duplicate entry 'admin' for key 'admin.uq_admin_username' | **LULUS** |
| D13 | CHECK room.price_per_hour negatif ditolak | ditolak, error 3819 | error 3819: Check constraint 'chk_room_price' is violated. | **LULUS** |
| D14 | CHECK instrument.rental_price negatif ditolak | ditolak, error 3819 | error 3819: Check constraint 'chk_instrument_price' is violated. | **LULUS** |
| D15 | CHECK quantity = 0 ditolak (BR-18) | ditolak, error 3819 | error 3819: Check constraint 'chk_bi_quantity' is violated. | **LULUS** |
| D16 | CHECK booking_instrument.rental_price negatif ditolak | ditolak, error 3819 | error 3819: Check constraint 'chk_bi_price' is violated. | **LULUS** |
| D17 | CHECK booking.total_cost negatif ditolak | ditolak, error 3819 | error 3819: Check constraint 'chk_booking_total' is violated. | **LULUS** |
| D18 | ENUM status booking tidak valid ditolak | ditolak, error 1265 | error 1265: Data truncated for column 'status' at row 1 | **LULUS** |
| D19 | ENUM status room tidak valid ditolak | ditolak, error 1265 | error 1265: Data truncated for column 'status' at row 1 | **LULUS** |
| D20 | NOT NULL customer.password_hash NULL ditolak (BR-20) | ditolak, error 1048 | error 1048: Column 'password_hash' cannot be null | **LULUS** |

#### E. Perilaku FK yang diizinkan

| No | Uji | Diharapkan | Aktual | Hasil |
|---|---|---|---|---|
| E01 | CASCADE: hapus booking 5 menghapus 4 detail alatnya, 8 detail lain tetap | 4 -> 0, sisa 8 | 4 -> 0, sisa 8 | **LULUS** |
| E02 | Room 5, Instrument 7, Customer 6 (tanpa booking) dapat dihapus | 1/1/1 | 1/1/1 | **LULUS** |
| E03 | setelah ROLLBACK data seed utuh (detail/booking/room/customer/instrument) | 12/7/5/6/7 | 12/7/5/6/7 | **LULUS** |

### Cuplikan `SHOW CREATE TABLE` (bukti constraint nyata)
```sql
CREATE TABLE `booking_instrument` (
  `booking_instrument_id` int NOT NULL AUTO_INCREMENT,
  `booking_id` int NOT NULL,
  `instrument_id` int NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `rental_price` decimal(10,2) NOT NULL,
  PRIMARY KEY (`booking_instrument_id`),
  UNIQUE KEY `uq_booking_instrument_pair` (`booking_id`,`instrument_id`),
  KEY `fk_bi_instrument` (`instrument_id`),
  CONSTRAINT `fk_bi_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bi_instrument` FOREIGN KEY (`instrument_id`) REFERENCES `instrument` (`instrument_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_bi_price` CHECK ((`rental_price` >= 0)),
  CONSTRAINT `chk_bi_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
```
```sql
CREATE TABLE `booking` (
  ... `status` enum('PENDING','CONFIRMED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  `total_cost` decimal(10,2) NOT NULL DEFAULT '0.00',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`booking_id`),
  KEY `fk_booking_customer` (`customer_id`),
  KEY `idx_booking_room_date_status` (`room_id`,`booking_date`,`status`),
  CONSTRAINT `fk_booking_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_booking_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`room_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_booking_time` CHECK ((`end_time` > `start_time`)),
  CONSTRAINT `chk_booking_total` CHECK ((`total_cost` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
```

### Verifikasi utilitas hash dan seed (dijalankan)
- Hash admin di database cocok dengan password demo admin (`COCOK`) dan **tidak** cocok dengan password demo customer.
- Hash customer 1 dan customer 6 keduanya cocok dengan password demo customer; salt keduanya berbeda; hash customer **tidak** cocok dengan password admin.
- Mode interaktif (`System.console()`) diuji lewat pseudo-terminal dan menghasilkan hash valid. Penyembunyian ketikan (tanpa echo) **tidak** diverifikasi mandiri karena sandbox tidak memiliki terminal sungguhan.
- Tanpa konsol dan tanpa `--stdin`, utilitas berhenti dengan pesan error yang jelas.
- Tidak ada password plaintext di `seed.sql`, `Pbkdf2HashGenerator.java`, maupun `config/` (diperiksa dengan pencarian teks).

## 6. Status AC-D01 s.d. AC-D05

| ID | Kriteria | Status | Bukti |
|---|---|---|---|
| AC-D01 | `schema.sql` membuat 6 tabel sesuai kontrak tanpa error di MySQL 8.0+ | **Lulus** (MySQL 8.0.46) | Siklus §4; A01–A02 |
| AC-D02 | FK, UNIQUE, CHECK, indeks sesuai kontrak | **Lulus** | A04–A11; `SHOW CREATE TABLE` |
| AC-D03 | `seed.sql` berisi data demo, password berupa hash | **Lulus** | B01–B09; hash cocok dengan password demo |
| AC-D04 | `reset.sql` mengembalikan database ke kondisi awal | **Lulus** | Siklus §4 (tabel menjadi 0, lalu pulih) |
| AC-D05 | Insert `booking_instrument` duplikat ditolak DB | **Lulus** | D01 (error 1062, kunci `uq_booking_instrument_pair`) |

Status di atas berlaku untuk lingkungan sandbox. Verifikasi di MySQL lokal pengguna masih disarankan (bagian 7).

## 7. Menjalankan verifikasi di MySQL lokal (Windows, PowerShell)

Prasyarat: MySQL Community Server 8.0.16+ terpasang dan `mysql.exe` ada di `PATH` (atau gunakan jalur lengkap, mis. `& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"`). Jalankan dari root `studio-musik\`. PowerShell tidak mendukung `<`, jadi gunakan `-e "source ..."`:

```powershell
mysql --version                                   # harus 8.0.16 atau lebih baru
mysql -u root -p -e "source database/reset.sql"
mysql -u root -p -e "source database/schema.sql"
mysql -u root -p -e "source database/seed.sql"
mysql -u root -p --table -e "source database/tests/verify_stage2.sql"
```
Ekspektasi: tabel ringkasan akhir menunjukkan `LULUS | 57` dan tidak ada baris `GAGAL`. Setelah itu jalankan lagi `reset` → `schema` → `seed` agar penghitung ID bersih. Akun Anda harus boleh `CREATE`/`DROP` database sementara `studio_musik_verify_tmp` (akun `root` lokal cukup).

Jika ada uji `GAGAL`, salin baris hasilnya ke percakapan berikutnya.

Centang saat selesai (isi dengan bukti):
- [ ] `mysql --version` ≥ 8.0.16
- [ ] reset, schema, seed berjalan tanpa error
- [ ] `verify_stage2.sql` menampilkan `LULUS | 57`
- [ ] reset → schema → seed ulang

## 8. Masalah yang ditemukan dan diperbaiki selama verifikasi
Semuanya pada **skrip uji**, bukan pada skema atau seed:
1. `GROUP_CONCAT` pada kolom ENUM terurut menurut indeks enum, bukan abjad → pengurutan diubah ke teks (uji B04).
2. SQL dinamis di dalam prosedur memakai database prosedur (sementara), sehingga tabel tidak ditemukan → nama tabel dikualifikasi `studio_musik.<tabel>` dan `DEALLOCATE` tidak lagi menimpa kode error asli.
3. Error 1265 (nilai ENUM tidak valid) berkelas SQLSTATE 01000 sehingga lolos dari handler `SQLEXCEPTION` dan menghentikan skrip → ditambahkan handler `SQLWARNING`.

## 9. Keterbatasan yang tersisa
- Belum diuji di MySQL lokal pengguna (Windows) dan belum ada uji koneksi JDBC dari Java.
- `verify_stage2.sql` menguji **struktur dan data seed**; pengujian aturan bisnis lewat Service baru ada mulai STEP 5.
- Cek `total_cost` memakai durasi jam bulat (A-05, belum dikonfirmasi tim); bila A-05 berubah, kolom dan seed tetap valid tetapi uji B06 perlu disesuaikan.
- Seed memakai tanggal tetap (D-027).
