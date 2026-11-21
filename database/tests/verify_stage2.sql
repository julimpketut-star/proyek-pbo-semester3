-- ============================================================================
-- verify_stage2.sql — Verifikasi otomatis STEP 2 (struktur, constraint, seed, overlap)
--
-- Prasyarat : reset.sql -> schema.sql -> seed.sql sudah dijalankan (database berisi data seed).
-- Cara pakai: mysql -u <user> -p --table < database/tests/verify_stage2.sql
--   (akun harus boleh CREATE/DROP database sementara 'studio_musik_verify_tmp')
-- Aman: semua uji yang mengubah data berjalan di dalam transaksi lalu di-ROLLBACK.
-- Catatan: INSERT yang gagal tetap menaikkan penghitung AUTO_INCREMENT. Setelah verifikasi,
--          jalankan ulang reset.sql -> schema.sql -> seed.sql agar ID kembali bersih.
-- Hasil: tabel akhir berisi kolom 'hasil' = LULUS / GAGAL untuk setiap uji, plus ringkasan.
-- ============================================================================

DROP DATABASE IF EXISTS studio_musik_verify_tmp;
CREATE DATABASE studio_musik_verify_tmp;
USE studio_musik_verify_tmp;

-- Tabel hasil bertipe MEMORY agar tidak ikut ter-ROLLBACK
CREATE TABLE results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tes VARCHAR(150) NOT NULL,
    diharapkan VARCHAR(250) NOT NULL,
    aktual VARCHAR(250) NOT NULL,
    hasil VARCHAR(10) NOT NULL
) ENGINE=MEMORY;

DELIMITER $$

-- Membandingkan nilai aktual dengan nilai yang diharapkan
CREATE PROCEDURE check_value(IN p_tes VARCHAR(150), IN p_expected VARCHAR(250), IN p_actual VARCHAR(250))
BEGIN
    INSERT INTO results (tes, diharapkan, aktual, hasil)
    VALUES (p_tes, p_expected, IFNULL(p_actual, 'NULL'), IF(p_expected <=> p_actual, 'LULUS', 'GAGAL'));
END$$

-- Menjalankan SQL yang HARUS gagal dengan nomor error tertentu.
-- Catatan: prosedur berjalan di database sementara, jadi SQL dinamis wajib memakai nama tabel berkualifikasi 'studio_musik.<tabel>'.
CREATE PROCEDURE expect_error(IN p_tes VARCHAR(150), IN p_sql TEXT, IN p_errno INT)
BEGIN
    DECLARE v_errno INT DEFAULT 0;
    DECLARE v_msg TEXT DEFAULT '';
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_msg = MESSAGE_TEXT;
    END;
    -- Error 1265 (nilai ENUM tidak valid) berkelas SQLSTATE 01000 (warning yang dinaikkan menjadi error
    -- pada mode strict), sehingga perlu handler tersendiri.
    DECLARE CONTINUE HANDLER FOR SQLWARNING
    BEGIN
        GET DIAGNOSTICS CONDITION 1 v_errno = MYSQL_ERRNO, v_msg = MESSAGE_TEXT;
    END;
    SET @verify_stmt = p_sql;
    PREPARE verify_s FROM @verify_stmt;
    IF v_errno = 0 THEN
        EXECUTE verify_s;
        DEALLOCATE PREPARE verify_s;
    END IF;
    INSERT INTO results (tes, diharapkan, aktual, hasil)
    VALUES (p_tes,
            CONCAT('ditolak, error ', p_errno),
            IF(v_errno = 0, 'DITERIMA (tidak ada error)', CONCAT('error ', v_errno, ': ', LEFT(v_msg, 120))),
            IF(v_errno = p_errno, 'LULUS', 'GAGAL'));
END$$

-- Query overlap persis dari docs/DATABASE_CONTRACT.md §4
CREATE PROCEDURE expect_overlap(IN p_tes VARCHAR(150), IN p_room INT, IN p_date DATE,
                                IN p_start TIME, IN p_end TIME, IN p_expected VARCHAR(10))
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_ids VARCHAR(100);
    DECLARE v_actual VARCHAR(10);
    SELECT COUNT(*), GROUP_CONCAT(booking_id ORDER BY booking_id)
      INTO v_count, v_ids
      FROM studio_musik.booking
     WHERE room_id = p_room
       AND booking_date = p_date
       AND status IN ('PENDING','CONFIRMED')
       AND start_time < p_end
       AND end_time > p_start;
    SET v_actual = IF(v_count > 0, 'OVERLAP', 'BEBAS');
    INSERT INTO results (tes, diharapkan, aktual, hasil)
    VALUES (p_tes, p_expected,
            CONCAT(v_actual, IFNULL(CONCAT(' (bentrok dgn booking ', v_ids, ')'), '')),
            IF(v_actual = p_expected, 'LULUS', 'GAGAL'));
END$$

DELIMITER ;

USE studio_musik;

-- ===========================================================================
-- A. STRUKTUR (AC-D01, AC-D02)
-- ===========================================================================
SET @v = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='studio_musik' AND TABLE_TYPE='BASE TABLE');
CALL studio_musik_verify_tmp.check_value('A01 jumlah tabel tepat 6', '6', CAST(@v AS CHAR));

SET @v = (SELECT GROUP_CONCAT(TABLE_NAME ORDER BY TABLE_NAME) FROM information_schema.TABLES WHERE TABLE_SCHEMA='studio_musik' AND TABLE_TYPE='BASE TABLE');
CALL studio_musik_verify_tmp.check_value('A02 nama enam tabel', 'admin,booking,booking_instrument,customer,instrument,room', @v);

SET @v = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='studio_musik' AND (ENGINE <> 'InnoDB' OR TABLE_COLLATION NOT LIKE 'utf8mb4%'));
CALL studio_musik_verify_tmp.check_value('A03 semua tabel InnoDB + utf8mb4', '0', CAST(@v AS CHAR));

SET @v = (SELECT COUNT(DISTINCT TABLE_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='studio_musik' AND INDEX_NAME='PRIMARY');
CALL studio_musik_verify_tmp.check_value('A04 setiap tabel punya PRIMARY KEY', '6', CAST(@v AS CHAR));

SET @v = (SELECT GROUP_CONCAT(CONCAT(CONSTRAINT_NAME, ':', DELETE_RULE) ORDER BY CONSTRAINT_NAME) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='studio_musik');
CALL studio_musik_verify_tmp.check_value('A05 FK dan aturan ON DELETE', 'fk_bi_booking:CASCADE,fk_bi_instrument:RESTRICT,fk_booking_customer:RESTRICT,fk_booking_room:RESTRICT', @v);

SET @v = (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='studio_musik' AND TABLE_NAME='booking_instrument' AND INDEX_NAME='uq_booking_instrument_pair' AND NON_UNIQUE=0);
CALL studio_musik_verify_tmp.check_value('A06 UNIQUE (booking_id, instrument_id)', 'booking_id,instrument_id', @v);

SET @v = (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='studio_musik' AND TABLE_NAME='customer' AND INDEX_NAME='uq_customer_email' AND NON_UNIQUE=0);
CALL studio_musik_verify_tmp.check_value('A07 UNIQUE customer.email', 'email', @v);

SET @v = (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='studio_musik' AND TABLE_NAME='admin' AND INDEX_NAME='uq_admin_username' AND NON_UNIQUE=0);
CALL studio_musik_verify_tmp.check_value('A08 UNIQUE admin.username', 'username', @v);

SET @v = (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='studio_musik');
CALL studio_musik_verify_tmp.check_value('A09 jumlah CHECK constraint', '6', CAST(@v AS CHAR));

SET @v = (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='studio_musik' AND TABLE_NAME='booking' AND INDEX_NAME='idx_booking_room_date_status');
CALL studio_musik_verify_tmp.check_value('A10 indeks booking(room_id, booking_date, status)', 'room_id,booking_date,status', @v);

SET @v = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='studio_musik' AND DATA_TYPE='decimal' AND NUMERIC_PRECISION=10 AND NUMERIC_SCALE=2);
CALL studio_musik_verify_tmp.check_value('A11 kolom uang bertipe DECIMAL(10,2)', '4', CAST(@v AS CHAR));

SET @v = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='studio_musik' AND COLUMN_NAME='password');
CALL studio_musik_verify_tmp.check_value('A12 tidak ada kolom bernama password (plaintext)', '0', CAST(@v AS CHAR));

SET @v = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='studio_musik' AND COLUMN_NAME='password_hash' AND DATA_TYPE='varchar' AND CHARACTER_MAXIMUM_LENGTH=255 AND IS_NULLABLE='NO' AND TABLE_NAME IN ('admin','customer'));
CALL studio_musik_verify_tmp.check_value('A13 password_hash VARCHAR(255) NOT NULL di admin & customer', '2', CAST(@v AS CHAR));

SET @v = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='studio_musik' AND TABLE_NAME='booking' AND COLUMN_NAME='created_at' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT LIKE 'CURRENT_TIMESTAMP%');
CALL studio_musik_verify_tmp.check_value('A14 booking.created_at NOT NULL DEFAULT CURRENT_TIMESTAMP', '1', CAST(@v AS CHAR));

-- ===========================================================================
-- B. SEED (AC-D03)
-- ===========================================================================
SET @v = (SELECT CONCAT((SELECT COUNT(*) FROM admin), '/', (SELECT COUNT(*) FROM customer), '/', (SELECT COUNT(*) FROM room), '/',
                        (SELECT COUNT(*) FROM instrument), '/', (SELECT COUNT(*) FROM booking), '/', (SELECT COUNT(*) FROM booking_instrument)));
CALL studio_musik_verify_tmp.check_value('B01 jumlah baris admin/customer/room/instrument/booking/booking_instrument', '1/6/5/7/7/12', @v);

SET @v = (SELECT COUNT(*) FROM (SELECT password_hash FROM admin UNION ALL SELECT password_hash FROM customer) p
          WHERE password_hash NOT REGEXP '^pbkdf2_sha256[$][0-9]+[$][A-Za-z0-9+/=]+[$][A-Za-z0-9+/=]+$');
CALL studio_musik_verify_tmp.check_value('B02 semua password_hash berformat pbkdf2_sha256$iter$salt$hash', '0', CAST(@v AS CHAR));

SET @v = (SELECT COUNT(DISTINCT password_hash) FROM (SELECT password_hash FROM admin UNION ALL SELECT password_hash FROM customer) p);
CALL studio_musik_verify_tmp.check_value('B03 setiap akun punya hash/salt berbeda', '7', CAST(@v AS CHAR));

SET @v = (SELECT GROUP_CONCAT(DISTINCT CAST(status AS CHAR) ORDER BY CAST(status AS CHAR)) FROM booking);
CALL studio_musik_verify_tmp.check_value('B04 seed memuat keempat status booking', 'CANCELLED,COMPLETED,CONFIRMED,PENDING', @v);

SET @v = (SELECT CONCAT((SELECT COUNT(*) FROM room WHERE status='MAINTENANCE'), '/', (SELECT COUNT(*) FROM instrument WHERE status='MAINTENANCE')));
CALL studio_musik_verify_tmp.check_value('B05 ada Room dan Instrument MAINTENANCE', '1/1', @v);

SET @v = (SELECT COUNT(*) FROM booking b JOIN room r ON r.room_id=b.room_id
          LEFT JOIN (SELECT booking_id, SUM(quantity*rental_price) s FROM booking_instrument GROUP BY booking_id) d ON d.booking_id=b.booking_id
          WHERE b.total_cost <> r.price_per_hour * TIMESTAMPDIFF(HOUR, b.start_time, b.end_time) + IFNULL(d.s,0));
CALL studio_musik_verify_tmp.check_value('B06 total_cost seed = harga ruangan x jam + subtotal alat (BR-09/10/11)', '0', CAST(@v AS CHAR));

SET @v = (SELECT CONCAT(bi.rental_price, ' vs ', i.rental_price) FROM booking_instrument bi JOIN instrument i ON i.instrument_id=bi.instrument_id WHERE bi.booking_instrument_id=5);
CALL studio_musik_verify_tmp.check_value('B07 snapshot harga alat berbeda dari harga instrument sekarang (BR-12)', '8000.00 vs 10000.00', @v);

SET @v = (SELECT CONCAT(MIN(c), '-', MAX(c)) FROM (SELECT instrument_id, COUNT(DISTINCT booking_id) c FROM booking_instrument WHERE instrument_id IN (1,5) GROUP BY instrument_id) t);
CALL studio_musik_verify_tmp.check_value('B08 M:N dua arah: instrument 1 dan 5 dipakai 3 booking masing-masing', '3-3', @v);

SET @v = (SELECT GROUP_CONCAT(CONCAT(i.name, ' x', bi.quantity) ORDER BY i.instrument_id) FROM booking b
          JOIN booking_instrument bi ON bi.booking_id=b.booking_id JOIN instrument i ON i.instrument_id=bi.instrument_id WHERE b.booking_id=5);
CALL studio_musik_verify_tmp.check_value('B09 Read M:N: JOIN booking+booking_instrument+instrument (booking 5)', 'Drum Set Pearl x1,Gitar Elektrik Fender x1,Bass Elektrik Ibanez x1,Mikrofon Shure SM58 x1', @v);

-- ===========================================================================
-- C. QUERY OVERLAP (BR-07, BR-08) — Studio A (room 1), 2026-12-01
--    aktif: booking 1 (10-12 CONFIRMED), booking 2 (12-14 PENDING); booking 3 (10-12) CANCELLED
-- ===========================================================================
CALL studio_musik_verify_tmp.expect_overlap('C01 overlap sebagian 11:00-13:00', 1, '2026-12-01', '11:00:00', '13:00:00', 'OVERLAP');
CALL studio_musik_verify_tmp.expect_overlap('C02 rentang BARU mengandung booking lama 09:00-15:00', 1, '2026-12-01', '09:00:00', '15:00:00', 'OVERLAP');
CALL studio_musik_verify_tmp.expect_overlap('C03 rentang BARU berada di dalam booking lama 10:30-11:30', 1, '2026-12-01', '10:30:00', '11:30:00', 'OVERLAP');
CALL studio_musik_verify_tmp.expect_overlap('C04 rentang identik 10:00-12:00 (booking 3 CANCELLED tidak dihitung)', 1, '2026-12-01', '10:00:00', '12:00:00', 'OVERLAP');
CALL studio_musik_verify_tmp.expect_overlap('C05 berurutan setelah booking 2: 14:00-16:00', 1, '2026-12-01', '14:00:00', '16:00:00', 'BEBAS');
CALL studio_musik_verify_tmp.expect_overlap('C06 berurutan sebelum booking 1: 08:00-10:00', 1, '2026-12-01', '08:00:00', '10:00:00', 'BEBAS');
CALL studio_musik_verify_tmp.expect_overlap('C07 booking CANCELLED sendirian (Studio Rekaman 13:00-17:00)', 4, '2026-12-05', '13:00:00', '17:00:00', 'BEBAS');
CALL studio_musik_verify_tmp.expect_overlap('C08 booking COMPLETED tidak memblokir (Studio A 2026-11-10 10:00-12:00)', 1, '2026-11-10', '10:00:00', '12:00:00', 'BEBAS');
CALL studio_musik_verify_tmp.expect_overlap('C09 tanggal berbeda, room sama', 1, '2026-12-02', '10:00:00', '12:00:00', 'BEBAS');
CALL studio_musik_verify_tmp.expect_overlap('C10 room berbeda, tanggal & jam sama', 2, '2026-12-01', '10:00:00', '12:00:00', 'BEBAS');
CALL studio_musik_verify_tmp.expect_overlap('C11 PENDING memblokir (Studio B 2026-12-02 16:00-17:00)', 2, '2026-12-02', '16:00:00', '17:00:00', 'OVERLAP');

-- ===========================================================================
-- D. CONSTRAINT: data tidak valid HARUS ditolak (AC-D05 dan lainnya)
--    Dibungkus transaksi + ROLLBACK agar aman bila ada yang tidak sengaja lolos.
-- ===========================================================================
START TRANSACTION;

CALL studio_musik_verify_tmp.expect_error('D01 UNIQUE(booking_id,instrument_id): duplikat ditolak',
  'INSERT INTO studio_musik.booking_instrument (booking_id, instrument_id, quantity, rental_price) VALUES (1, 1, 1, 50000)', 1062);
CALL studio_musik_verify_tmp.expect_error('D02 CHECK end_time = start_time ditolak',
  'INSERT INTO studio_musik.booking (customer_id, room_id, booking_date, start_time, end_time, status, total_cost) VALUES (1, 1, ''2027-01-10'', ''10:00:00'', ''10:00:00'', ''PENDING'', 0)', 3819);
CALL studio_musik_verify_tmp.expect_error('D03 CHECK end_time < start_time ditolak',
  'INSERT INTO studio_musik.booking (customer_id, room_id, booking_date, start_time, end_time, status, total_cost) VALUES (1, 1, ''2027-01-10'', ''12:00:00'', ''10:00:00'', ''PENDING'', 0)', 3819);
CALL studio_musik_verify_tmp.expect_error('D04 FK booking.customer_id tidak ada ditolak',
  'INSERT INTO studio_musik.booking (customer_id, room_id, booking_date, start_time, end_time, status, total_cost) VALUES (999, 1, ''2027-01-10'', ''10:00:00'', ''12:00:00'', ''PENDING'', 0)', 1452);
CALL studio_musik_verify_tmp.expect_error('D05 FK booking.room_id tidak ada ditolak',
  'INSERT INTO studio_musik.booking (customer_id, room_id, booking_date, start_time, end_time, status, total_cost) VALUES (1, 999, ''2027-01-10'', ''10:00:00'', ''12:00:00'', ''PENDING'', 0)', 1452);
CALL studio_musik_verify_tmp.expect_error('D06 FK booking_instrument.booking_id tidak ada ditolak',
  'INSERT INTO studio_musik.booking_instrument (booking_id, instrument_id, quantity, rental_price) VALUES (999, 1, 1, 50000)', 1452);
CALL studio_musik_verify_tmp.expect_error('D07 FK booking_instrument.instrument_id tidak ada ditolak',
  'INSERT INTO studio_musik.booking_instrument (booking_id, instrument_id, quantity, rental_price) VALUES (6, 999, 1, 50000)', 1452);
CALL studio_musik_verify_tmp.expect_error('D08 RESTRICT: hapus customer yang punya booking ditolak (BR-22)',
  'DELETE FROM studio_musik.customer WHERE customer_id = 1', 1451);
CALL studio_musik_verify_tmp.expect_error('D09 RESTRICT: hapus room yang punya booking ditolak (BR-22)',
  'DELETE FROM studio_musik.room WHERE room_id = 1', 1451);
CALL studio_musik_verify_tmp.expect_error('D10 RESTRICT: hapus instrument yang dipakai booking ditolak (BR-22)',
  'DELETE FROM studio_musik.instrument WHERE instrument_id = 1', 1451);
CALL studio_musik_verify_tmp.expect_error('D11 UNIQUE customer.email duplikat ditolak (BR-20)',
  'INSERT INTO studio_musik.customer (name, phone, email, password_hash) VALUES (''Uji'', ''0811'', ''ayu.lestari@example.com'', ''x'')', 1062);
CALL studio_musik_verify_tmp.expect_error('D12 UNIQUE admin.username duplikat ditolak',
  'INSERT INTO studio_musik.admin (username, password_hash, name) VALUES (''admin'', ''x'', ''Uji'')', 1062);
CALL studio_musik_verify_tmp.expect_error('D13 CHECK room.price_per_hour negatif ditolak',
  'INSERT INTO studio_musik.room (name, type, price_per_hour, status) VALUES (''Uji'', ''Uji'', -1, ''AVAILABLE'')', 3819);
CALL studio_musik_verify_tmp.expect_error('D14 CHECK instrument.rental_price negatif ditolak',
  'INSERT INTO studio_musik.instrument (name, type, rental_price, status) VALUES (''Uji'', ''Uji'', -1, ''AVAILABLE'')', 3819);
CALL studio_musik_verify_tmp.expect_error('D15 CHECK quantity = 0 ditolak (BR-18)',
  'INSERT INTO studio_musik.booking_instrument (booking_id, instrument_id, quantity, rental_price) VALUES (6, 1, 0, 50000)', 3819);
CALL studio_musik_verify_tmp.expect_error('D16 CHECK booking_instrument.rental_price negatif ditolak',
  'INSERT INTO studio_musik.booking_instrument (booking_id, instrument_id, quantity, rental_price) VALUES (6, 1, 1, -1)', 3819);
CALL studio_musik_verify_tmp.expect_error('D17 CHECK booking.total_cost negatif ditolak',
  'INSERT INTO studio_musik.booking (customer_id, room_id, booking_date, start_time, end_time, status, total_cost) VALUES (1, 1, ''2027-01-10'', ''10:00:00'', ''12:00:00'', ''PENDING'', -5)', 3819);
CALL studio_musik_verify_tmp.expect_error('D18 ENUM status booking tidak valid ditolak',
  'INSERT INTO studio_musik.booking (customer_id, room_id, booking_date, start_time, end_time, status, total_cost) VALUES (1, 1, ''2027-01-10'', ''10:00:00'', ''12:00:00'', ''BOGUS'', 0)', 1265);
CALL studio_musik_verify_tmp.expect_error('D19 ENUM status room tidak valid ditolak',
  'INSERT INTO studio_musik.room (name, type, price_per_hour, status) VALUES (''Uji'', ''Uji'', 1, ''BOGUS'')', 1265);
CALL studio_musik_verify_tmp.expect_error('D20 NOT NULL customer.password_hash NULL ditolak (BR-20)',
  'INSERT INTO studio_musik.customer (name, phone, email, password_hash) VALUES (''Uji'', ''0811'', ''uji.null@example.com'', NULL)', 1048);

ROLLBACK;

-- ===========================================================================
-- E. PERILAKU FK YANG DIIZINKAN (di dalam transaksi, lalu ROLLBACK)
-- ===========================================================================
START TRANSACTION;

SET @before = (SELECT COUNT(*) FROM booking_instrument WHERE booking_id = 5);
DELETE FROM booking WHERE booking_id = 5;
SET @after = (SELECT COUNT(*) FROM booking_instrument WHERE booking_id = 5);
SET @others = (SELECT COUNT(*) FROM booking_instrument);
CALL studio_musik_verify_tmp.check_value('E01 CASCADE: hapus booking 5 menghapus 4 detail alatnya, 8 detail lain tetap',
  '4 -> 0, sisa 8', CONCAT(@before, ' -> ', @after, ', sisa ', @others));

ROLLBACK;

START TRANSACTION;
DELETE FROM room WHERE room_id = 5;
SET @r1 = ROW_COUNT();
DELETE FROM instrument WHERE instrument_id = 7;
SET @r2 = ROW_COUNT();
DELETE FROM customer WHERE customer_id = 6;
SET @r3 = ROW_COUNT();
CALL studio_musik_verify_tmp.check_value('E02 Room 5, Instrument 7, Customer 6 (tanpa booking) dapat dihapus', '1/1/1', CONCAT(@r1, '/', @r2, '/', @r3));
ROLLBACK;

SET @v = (SELECT CONCAT((SELECT COUNT(*) FROM booking_instrument), '/', (SELECT COUNT(*) FROM booking), '/', (SELECT COUNT(*) FROM room), '/', (SELECT COUNT(*) FROM customer), '/', (SELECT COUNT(*) FROM instrument)));
CALL studio_musik_verify_tmp.check_value('E03 setelah ROLLBACK data seed utuh (detail/booking/room/customer/instrument)', '12/7/5/6/7', @v);

-- ===========================================================================
-- HASIL
-- ===========================================================================
SELECT @@version AS mysql_version, @@sql_mode AS sql_mode;
SELECT id, tes, diharapkan, aktual, hasil FROM studio_musik_verify_tmp.results ORDER BY id;
SELECT hasil, COUNT(*) AS jumlah FROM studio_musik_verify_tmp.results GROUP BY hasil ORDER BY hasil;

DROP DATABASE studio_musik_verify_tmp;
