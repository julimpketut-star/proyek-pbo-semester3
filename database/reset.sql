-- ============================================================================
-- reset.sql — Sistem Pengelolaan Operasional Studio Musik (STEP 2)
--
-- !!! PERINGATAN: SCRIPT INI DESTRUKTIF !!!
-- Menghapus SELURUH tabel dan SEMUA data di database studio_musik
-- (admin, customer, room, instrument, booking, booking_instrument).
-- Data tidak dapat dikembalikan. Jangan jalankan pada database yang berisi data penting.
-- Database 'studio_musik' sendiri TIDAK dihapus; hanya tabel-tabelnya.
--
-- Urutan DROP mengikuti foreign key: tabel anak dihapus lebih dulu sebelum tabel induk.
--   booking_instrument -> (booking, instrument)
--   booking            -> (customer, room)
--
-- Mengembalikan ke kondisi awal setelah reset:
--   mysql -u <user> -p < database/reset.sql
--   mysql -u <user> -p < database/schema.sql
--   mysql -u <user> -p < database/seed.sql
-- ============================================================================

CREATE DATABASE IF NOT EXISTS studio_musik CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE studio_musik;

DROP TABLE IF EXISTS booking_instrument;
DROP TABLE IF EXISTS booking;
DROP TABLE IF EXISTS instrument;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS admin;
