-- ============================================================================
-- schema.sql — Sistem Pengelolaan Operasional Studio Musik (PBO TK II, STEP 2)
-- Sumber kebenaran: docs/DATABASE_CONTRACT.md (jangan ubah tanpa entri Decision Log)
-- Target : MySQL 8.0.16+ (CHECK ditegakkan mulai versi ini), InnoDB, utf8mb4
--
-- Cara pakai (dari root proyek):
--   mysql -u <user> -p < database/schema.sql
-- Aman dijalankan ulang: memakai IF NOT EXISTS (tidak menghapus data).
-- Untuk mengosongkan database gunakan database/reset.sql (DESTRUKTIF).
-- ============================================================================

CREATE DATABASE IF NOT EXISTS studio_musik
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE studio_musik;

-- ---------------------------------------------------------------------------
-- 1. admin
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin (
    admin_id      INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    CONSTRAINT pk_admin PRIMARY KEY (admin_id),
    CONSTRAINT uq_admin_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 2. customer  (email = identitas login, wajib unik; D-003)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer (
    customer_id   INT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT pk_customer PRIMARY KEY (customer_id),
    CONSTRAINT uq_customer_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 3. room
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS room (
    room_id        INT           NOT NULL AUTO_INCREMENT,
    name           VARCHAR(50)   NOT NULL,
    type           VARCHAR(50)   NOT NULL,
    price_per_hour DECIMAL(10,2) NOT NULL,
    status         ENUM('AVAILABLE','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT pk_room PRIMARY KEY (room_id),
    CONSTRAINT chk_room_price CHECK (price_per_hour >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 4. instrument  (rental_price = harga per unit per sesi, A-01)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS instrument (
    instrument_id INT           NOT NULL AUTO_INCREMENT,
    name          VARCHAR(50)   NOT NULL,
    type          VARCHAR(50)   NOT NULL,
    rental_price  DECIMAL(10,2) NOT NULL,
    status        ENUM('AVAILABLE','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT pk_instrument PRIMARY KEY (instrument_id),
    CONSTRAINT chk_instrument_price CHECK (rental_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 5. booking
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS booking (
    booking_id   INT           NOT NULL AUTO_INCREMENT,
    customer_id  INT           NOT NULL,
    room_id      INT           NOT NULL,
    booking_date DATE          NOT NULL,
    start_time   TIME          NOT NULL,
    end_time     TIME          NOT NULL,
    status       ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    total_cost   DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_booking PRIMARY KEY (booking_id),
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id)
        REFERENCES customer (customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id)
        REFERENCES room (room_id) ON DELETE RESTRICT,
    CONSTRAINT chk_booking_time CHECK (end_time > start_time),
    CONSTRAINT chk_booking_total CHECK (total_cost >= 0),
    -- Indeks pencarian jadwal/overlap per room, tanggal, dan status (BR-08)
    INDEX idx_booking_room_date_status (room_id, booking_date, status)
    -- Indeks customer_id dibuat otomatis oleh FK fk_booking_customer (riwayat Customer)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 6. booking_instrument  (tabel penghubung M:N; composition terhadap booking)
--    booking_id    -> ON DELETE CASCADE  (detail ikut hilang bersama booking; D-020)
--    instrument_id -> ON DELETE RESTRICT (alat yang pernah dipakai tidak bisa dihapus)
--    rental_price  = snapshot harga saat alat ditambahkan (BR-12)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS booking_instrument (
    booking_instrument_id INT           NOT NULL AUTO_INCREMENT,
    booking_id            INT           NOT NULL,
    instrument_id         INT           NOT NULL,
    quantity              INT           NOT NULL DEFAULT 1,
    rental_price          DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_booking_instrument PRIMARY KEY (booking_instrument_id),
    CONSTRAINT uq_booking_instrument_pair UNIQUE (booking_id, instrument_id),
    CONSTRAINT fk_bi_booking FOREIGN KEY (booking_id)
        REFERENCES booking (booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_bi_instrument FOREIGN KEY (instrument_id)
        REFERENCES instrument (instrument_id) ON DELETE RESTRICT,
    CONSTRAINT chk_bi_quantity CHECK (quantity > 0),
    CONSTRAINT chk_bi_price CHECK (rental_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
