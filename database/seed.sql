-- ============================================================================
-- seed.sql — Sistem Pengelolaan Operasional Studio Musik (STEP 2)
-- Data demo/pengujian. Sumber kebenaran struktur: docs/DATABASE_CONTRACT.md
--
-- Prasyarat : database/schema.sql sudah dijalankan dan tabel masih KOSONG.
--             (Menjalankan seed dua kali tanpa reset akan gagal karena ID/UNIQUE bentrok.)
-- Cara pakai: mysql -u <user> -p < database/seed.sql
-- Ulang dari awal: reset.sql -> schema.sql -> seed.sql
--
-- Password: SEMUA berupa hash PBKDF2 (format D-018: pbkdf2_sha256$iterasi$salt$hash),
--           dibuat dengan database/tools/Pbkdf2HashGenerator.java. Tidak ada plaintext di file ini.
--           Kredensial demo disampaikan terpisah oleh tim (tidak disimpan di repository).
--
-- Peta skenario uji (semua pada Studio A = room_id 1, tanggal 2026-12-01 kecuali disebut):
--   booking 1  10:00-12:00 CONFIRMED  -> booking aktif
--   booking 2  12:00-14:00 PENDING    -> BERURUTAN dengan booking 1 (tidak boleh dianggap overlap)
--   booking 3  10:00-12:00 CANCELLED  -> rentang SAMA dengan booking 1 tetapi tidak memblokir
--   booking 4  Studio A 2026-11-10 10:00-13:00 COMPLETED -> tidak memblokir; snapshot harga alat
--   booking 5  Studio B 2026-12-02 15:00-18:00 PENDING   -> 4 alat berbeda
--   booking 6  Studio C 2026-12-03 09:00-10:00 CONFIRMED -> tanpa alat
--   booking 7  Studio Rekaman 2026-12-05 13:00-17:00 CANCELLED (sendirian) -> uji "cancelled tidak memblokir"
--   Room 5 (MAINTENANCE), Instrument 7 (MAINTENANCE), Customer 6: belum dipakai booking -> dapat dihapus (uji BR-22)
--   Booking 4 menyimpan rental_price alat (8000) yang BERBEDA dari harga instrument sekarang (10000) -> uji BR-12
-- ============================================================================

USE studio_musik;

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- admin
-- ---------------------------------------------------------------------------
INSERT INTO admin (admin_id, username, password_hash, name) VALUES
(1, 'admin', 'pbkdf2_sha256$600000$Ecjw2gV6NW3rGbUOR0xgSw==$V+M7Y98OS1a7pS58hxKhoX5OfsiszwGxAhjxZic1ezc=', 'Administrator Studio');

-- ---------------------------------------------------------------------------
-- customer (email memakai domain example.com yang dicadangkan untuk contoh)
-- ---------------------------------------------------------------------------
INSERT INTO customer (customer_id, name, phone, email, password_hash) VALUES
(1, 'Made Ayu Lestari',     '081234567801', 'ayu.lestari@example.com',   'pbkdf2_sha256$600000$bSnbdx5kIk6bbS88SoqTiw==$/GhAxjaOtEO+sNEn/C53cb2tUqqfKU0iJIqKVhyUM4w='),
(2, 'Kadek Budi Santoso',   '081234567802', 'budi.santoso@example.com',  'pbkdf2_sha256$600000$CzNDNA48quftMum+wC76Kg==$cywm6Qn9oLK18EVp3VarhWKVM6L62nuHjehuBv51Fhc='),
(3, 'Putu Eka Wijaya',      '081234567803', 'eka.wijaya@example.com',    'pbkdf2_sha256$600000$4w4RSpuKK/YPf8ip2y5gMg==$JQX7wTiLJ3M9ZxjuB3Kb2KVF5RJFOyAC7GH8DpeDL6A='),
(4, 'Komang Sari Dewi',     '081234567804', 'sari.dewi@example.com',     'pbkdf2_sha256$600000$G1w+hgjRGSRlk7FsSiGOSQ==$UDz1ribytQLxyceRriRskndP2l97Gyxvi+U4ZHbnwMM='),
(5, 'Nyoman Dharma Putra',  '081234567805', 'dharma.putra@example.com',  'pbkdf2_sha256$600000$hu9bMqtBCuygm9RqpSM2UQ==$dPQ3R+mHtDbQMgsZ0dBp+B6r4ZF77Bb3ELKfI17IoJs='),
(6, 'Wayan Tirta Yasa',     '081234567806', 'tirta.yasa@example.com',    'pbkdf2_sha256$600000$k+CB61rHBt18pfBfpBCGdA==$lbSSwz9R+pboXPGk2XOQ3GmbeitM1HjxIEsuGn0kmHc=');

-- ---------------------------------------------------------------------------
-- room (harga per jam, Rupiah)
-- ---------------------------------------------------------------------------
INSERT INTO room (room_id, name, type, price_per_hour, status) VALUES
(1, 'Studio A',        'Band Rehearsal', 150000.00, 'AVAILABLE'),
(2, 'Studio B',        'Band Rehearsal', 120000.00, 'AVAILABLE'),
(3, 'Studio C',        'Vocal Booth',     80000.00, 'AVAILABLE'),
(4, 'Studio Rekaman',  'Recording',      250000.00, 'AVAILABLE'),
(5, 'Studio D',        'Band Rehearsal', 100000.00, 'MAINTENANCE');

-- ---------------------------------------------------------------------------
-- instrument (harga sewa per unit per sesi, A-01)
-- ---------------------------------------------------------------------------
INSERT INTO instrument (instrument_id, name, type, rental_price, status) VALUES
(1, 'Drum Set Pearl',          'Drum',      50000.00, 'AVAILABLE'),
(2, 'Gitar Elektrik Fender',   'Gitar',     25000.00, 'AVAILABLE'),
(3, 'Bass Elektrik Ibanez',    'Bass',      25000.00, 'AVAILABLE'),
(4, 'Keyboard Yamaha PSR',     'Keyboard',  35000.00, 'AVAILABLE'),
(5, 'Mikrofon Shure SM58',     'Mikrofon',  10000.00, 'AVAILABLE'),
(6, 'Amplifier Marshall',      'Amplifier', 30000.00, 'AVAILABLE'),
(7, 'Gitar Akustik Taylor',    'Gitar',     25000.00, 'MAINTENANCE');

-- ---------------------------------------------------------------------------
-- booking
-- total_cost = harga ruangan x jam + SUM(quantity x rental_price detail)   (BR-09, BR-10, BR-11)
-- ---------------------------------------------------------------------------
INSERT INTO booking (booking_id, customer_id, room_id, booking_date, start_time, end_time, status, total_cost, created_at) VALUES
(1, 1, 1, '2026-12-01', '10:00:00', '12:00:00', 'CONFIRMED',  400000.00, '2026-11-20 09:00:00'),  -- 2x150000 + 50000 + 2x25000
(2, 2, 1, '2026-12-01', '12:00:00', '14:00:00', 'PENDING',    335000.00, '2026-11-20 09:30:00'),  -- 2x150000 + 35000
(3, 3, 1, '2026-12-01', '10:00:00', '12:00:00', 'CANCELLED',  350000.00, '2026-11-19 16:00:00'),  -- 2x150000 + 50000
(4, 4, 1, '2026-11-10', '10:00:00', '13:00:00', 'COMPLETED',  496000.00, '2026-11-05 10:00:00'),  -- 3x150000 + 2x8000 + 30000
(5, 5, 2, '2026-12-02', '15:00:00', '18:00:00', 'PENDING',    470000.00, '2026-11-21 11:00:00'),  -- 3x120000 + 25000 + 25000 + 50000 + 10000
(6, 1, 3, '2026-12-03', '09:00:00', '10:00:00', 'CONFIRMED',   80000.00, '2026-11-21 12:00:00'),  -- 1x80000
(7, 2, 4, '2026-12-05', '13:00:00', '17:00:00', 'CANCELLED', 1055000.00, '2026-11-22 08:00:00');  -- 4x250000 + 2x10000 + 35000

-- ---------------------------------------------------------------------------
-- booking_instrument (relasi M:N; rental_price = snapshot saat booking)
-- Drum Set (1) dan Mikrofon (5) muncul pada beberapa booking -> M:N dua arah
-- ---------------------------------------------------------------------------
INSERT INTO booking_instrument (booking_instrument_id, booking_id, instrument_id, quantity, rental_price) VALUES
(1,  1, 1, 1, 50000.00),
(2,  1, 2, 2, 25000.00),
(3,  2, 4, 1, 35000.00),
(4,  3, 1, 1, 50000.00),
(5,  4, 5, 2,  8000.00),   -- snapshot lama (harga instrument sekarang 10000)
(6,  4, 6, 1, 30000.00),
(7,  5, 2, 1, 25000.00),
(8,  5, 3, 1, 25000.00),
(9,  5, 1, 1, 50000.00),
(10, 5, 5, 1, 10000.00),
(11, 7, 5, 2, 10000.00),
(12, 7, 4, 1, 35000.00);

COMMIT;
