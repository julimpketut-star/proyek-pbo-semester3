# DATABASE CONTRACT

> Status: **TERKUNCI (STEP 0)**. File `database/schema.sql`, `seed.sql`, `reset.sql` dibuat pada STEP 2 dan **wajib** mengikuti dokumen ini.
> Dokumen ini adalah kontrak (spesifikasi tabel), bukan skrip SQL.

## 1. Umum
- Database: `studio_musik`; MySQL 8.0.16+ (agar `CHECK` ditegakkan); engine InnoDB; charset `utf8mb4`.
- Nama tabel dan kolom: `snake_case`, huruf kecil.
- Nilai uang: `DECIMAL(10,2)`.
- Tidak ada password plaintext di kolom, seed, maupun source code.

## 2. Tabel

### 2.1 `admin`
| Kolom | Tipe | Constraint |
|---|---|---|
| `admin_id` | INT | PK, AUTO_INCREMENT |
| `username` | VARCHAR(50) | NOT NULL, UNIQUE |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `name` | VARCHAR(100) | NOT NULL |

### 2.2 `customer`
| Kolom | Tipe | Constraint |
|---|---|---|
| `customer_id` | INT | PK, AUTO_INCREMENT |
| `name` | VARCHAR(100) | NOT NULL |
| `phone` | VARCHAR(20) | NOT NULL |
| `email` | VARCHAR(100) | NOT NULL, UNIQUE |
| `password_hash` | VARCHAR(255) | NOT NULL |

### 2.3 `room`
| Kolom | Tipe | Constraint |
|---|---|---|
| `room_id` | INT | PK, AUTO_INCREMENT |
| `name` | VARCHAR(50) | NOT NULL |
| `type` | VARCHAR(50) | NOT NULL |
| `price_per_hour` | DECIMAL(10,2) | NOT NULL, CHECK `>= 0` |
| `status` | ENUM('AVAILABLE','MAINTENANCE') | NOT NULL, DEFAULT 'AVAILABLE' |

### 2.4 `instrument`
| Kolom | Tipe | Constraint |
|---|---|---|
| `instrument_id` | INT | PK, AUTO_INCREMENT |
| `name` | VARCHAR(50) | NOT NULL |
| `type` | VARCHAR(50) | NOT NULL |
| `rental_price` | DECIMAL(10,2) | NOT NULL, CHECK `>= 0` (harga per unit per sesi, A-01) |
| `status` | ENUM('AVAILABLE','MAINTENANCE') | NOT NULL, DEFAULT 'AVAILABLE' |

### 2.5 `booking`
| Kolom | Tipe | Constraint |
|---|---|---|
| `booking_id` | INT | PK, AUTO_INCREMENT |
| `customer_id` | INT | NOT NULL, FK → `customer(customer_id)` ON DELETE RESTRICT |
| `room_id` | INT | NOT NULL, FK → `room(room_id)` ON DELETE RESTRICT |
| `booking_date` | DATE | NOT NULL |
| `start_time` | TIME | NOT NULL |
| `end_time` | TIME | NOT NULL, CHECK `end_time > start_time` |
| `status` | ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED') | NOT NULL, DEFAULT 'PENDING' |
| `total_cost` | DECIMAL(10,2) | NOT NULL, DEFAULT 0, CHECK `>= 0` |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

Indeks: `(room_id, booking_date, status)` untuk query overlap; `(customer_id)` untuk riwayat Customer (otomatis dari FK).

### 2.6 `booking_instrument`
| Kolom | Tipe | Constraint |
|---|---|---|
| `booking_instrument_id` | INT | PK, AUTO_INCREMENT |
| `booking_id` | INT | NOT NULL, FK → `booking(booking_id)` ON DELETE **CASCADE** |
| `instrument_id` | INT | NOT NULL, FK → `instrument(instrument_id)` ON DELETE RESTRICT |
| `quantity` | INT | NOT NULL, DEFAULT 1, CHECK `> 0` |
| `rental_price` | DECIMAL(10,2) | NOT NULL, CHECK `>= 0` (snapshot, BR-12) |

Unique: `(booking_id, instrument_id)`.

## 3. Relasi
- Customer 1 : N Booking (`booking.customer_id`).
- Room 1 : N Booking (`booking.room_id`).
- Booking M : N Instrument melalui `booking_instrument`.

## 4. Query kontrak (perilaku wajib)

**Overlap (BR-08)** — dipakai `BookingDAO.existsOverlap`:
```
room_id = ? AND booking_date = ? AND status IN ('PENDING','CONFIRMED')
AND start_time < :requestedEnd AND end_time > :requestedStart
```

**Read M:N (CRUD-Read)** — JOIN `booking` ↔ `booking_instrument` ↔ `instrument` berdasarkan `booking_id` dan `instrument_id`.

## 5. Aturan transaksi
- `createBooking`: `START TRANSACTION` → kunci baris room (`SELECT ... FOR UPDATE`) → cek status room, cek overlap → insert `booking` → insert semua `booking_instrument` → update `total_cost` → `COMMIT`; error apa pun → `ROLLBACK`.
- Create/Update/Delete detail alat: satu transaksi yang juga menghitung ulang dan menyimpan `booking.total_cost`.
- `autoCommit` dikembalikan ke `true` atau koneksi ditutup (try-with-resources) setelah transaksi.

## 6. Seed (ditulis pada STEP 2)
- Minimal: 1 Admin (password berupa **hash**, kredensial demo dicatat di README, bukan kredensial nyata), beberapa Room (termasuk 1 `MAINTENANCE`), beberapa Instrument (termasuk 1 `MAINTENANCE`), beberapa Customer demo, beberapa Booking dengan status berbeda untuk demonstrasi dan pengujian overlap.

## 7. Perubahan dari ADM TK I (ringkas)
`password` → `password_hash`; `customer.email` NOT NULL UNIQUE; `customer.password_hash` baru; `booking.created_at` baru; unique pada pasangan `booking_instrument`; CHECK dan indeks baru; kolom status/quantity/total_cost menjadi NOT NULL. Alasan lengkap: `DECISION_LOG.md` D-002 s.d. D-004, D-020.

## 8. Status implementasi (STEP 2)
Diimplementasikan di `database/schema.sql`, `database/seed.sql`, `database/reset.sql` **tanpa mengubah** kolom, tipe, atau constraint pada §2.
Tambahan yang tidak mengubah kontrak: constraint diberi nama (`pk_*`, `uq_*`, `fk_*`, `chk_*`, `idx_*`), collation `utf8mb4_0900_ai_ci`, dan seed memakai ID eksplisit. Skenario seed dan hasil verifikasi: `docs/STAGE2_VERIFICATION.md`. Keputusan terkait: `DECISION_LOG.md` D-023 s.d. D-027.
