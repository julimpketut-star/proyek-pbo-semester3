# STAGE 4B VERIFICATION — DAO & JDBC Queries

Tanggal: 2026-11-21 · Cakupan: enam DAO + tes integrasi DAO (bagian B STEP 4; bagian A = `DatabaseConnection`/`PasswordUtil` dikerjakan worker 4A secara paralel).

## 1. Kesimpulan jujur

| Item | Status |
|---|---|
| Enam DAO di `src/main/java/com/studiomusik/dao/` | **Dibuat**; 45 method, seluruhnya cocok dengan `API_CONTRACTS.md` §3 (nama, tipe parameter, tipe kembalian, `throws SQLException`) — diperiksa otomatis lewat `javap` |
| Kompilasi `javac --release 21 -Xlint:all` (6 DAO + model + enum) | **Sukses, tanpa warning** — memakai **stub sementara** `DatabaseConnection` (lihat §3) |
| Tes DAO (49 tes) terhadap MySQL 8.0.46 nyata | **49 berhasil, 0 gagal, 0 dilewati** |
| Seluruh suite (model + `AppInfoTest` + DAO) | **128 ditemukan, 128 berhasil, 0 gagal, 0 dilewati** |
| `mvn clean test` untuk kode STEP 4B | **BELUM dijalankan** (Maven tidak dapat mengunduh dependency di sandbox; `DatabaseConnection` asli milik 4A belum ada di workspace ini) |
| Diuji dengan MySQL Connector/J 8.4.0 | **BELUM**. Sandbox hanya memiliki driver MariaDB Connector/J 2.7.6 (lihat §3 dan §7) |
| Build penuh proyek sukses | **TIDAK diklaim.** Kompilasi penuh baru valid setelah `DatabaseConnection` 4A digabung |

### Gerbang STEP 4 (dilaporkan pengguna pada prompt ini; tidak dapat diverifikasi ulang oleh Claude)
| Gerbang | Hasil yang dilaporkan |
|---|---|
| `mvn clean test` lokal | 79 tes, Failures 0, Errors 0, Skipped 0, BUILD SUCCESS |
| Verifikasi MySQL lokal | `LULUS \| 57` |
| Versi MySQL lokal / branch | 8.0.46 / `feature/model` |

Kedua gerbang terpenuhi, sehingga pembuatan DAO dilanjutkan.

## 2. File

**Dibuat (main):** `AdminDAO`, `CustomerDAO`, `RoomDAO`, `InstrumentDAO`, `BookingDAO`, `BookingInstrumentDAO` di `src/main/java/com/studiomusik/dao/`.

**Dibuat (test):** `src/test/java/com/studiomusik/dao/` — `DaoTestSupport` (pendukung; namanya sengaja tidak berpola `Test*`/`*Test` agar tidak diambil Surefire sebagai kelas tes), `AdminDAOTest` (3), `CustomerDAOTest` (7), `RoomDAOTest` (7), `InstrumentDAOTest` (5), `BookingDAOTest` (13), `BookingInstrumentDAOTest` (14) = **49 tes**.

**Dibuat (dokumen):** file ini.

**Tidak diubah:** model/enum, `pom.xml`, `database/*.sql`, `PROGRESS_HANDOFF.md`, `PROGRESS.md`, `README.md`, `DECISION_LOG.md`, `ACCEPTANCE_CRITERIA.md`, `dao/package-info.java`. **Tidak dibuat:** `DatabaseConnection`, util lain, Service, controller, FXML.

## 3. Cara verifikasi di sandbox (dan batasannya)

| Komponen | Dipakai | Catatan |
|---|---|---|
| MySQL server | 8.0.46 (apt), mode strict; `reset → schema → seed` dari `database/*.sql` | `verify_stage2.sql` = **LULUS \| 57** sebelum dan **sesudah** seluruh tes (seed utuh) |
| JDBC driver | **MariaDB Connector/J 2.7.6**, URL `jdbc:mariadb://` | **Bukan** MySQL Connector/J 8.4.0. Kode DAO hanya memakai `java.sql`/`java.time`, tetapi perilaku Connector/J 8.4.0 belum terbukti di sini |
| `DatabaseConnection` | **Stub sementara di luar proyek** (membaca `config/db.properties`, `DriverManager`) | **Bukan bagian hasil kerja dan tidak ada di ZIP.** Hanya agar DAO dapat dikompilasi/dijalankan di sandbox |
| Runner | JUnit Platform console 1.9.1 + Jupiter 5.10.1 (apt) | Proyek mengunci JUnit 5.10.2 lewat Maven; **bukan** `mvn test` |
| Autentikasi user sandbox | `mysql_native_password` | Driver MariaDB 2.7.6 tidak mendukung `caching_sha2_password` tanpa SSL; hanya masalah sandbox, bukan kode DAO |

## 4. Hasil tes aktual

| Perintah / pemeriksaan | Hasil |
|---|---|
| `javac -Xlint:all` DAO + model + enum + stub | exit 0, tanpa warning |
| `javac -Xlint:all` 7 file tes DAO | exit 0, tanpa warning |
| Tes DAO (JUnit console, MySQL 8.0.46) | **49 ditemukan, 49 berhasil, 0 gagal, 0 dilewati, 0 dibatalkan** |
| Tes DAO dengan `-Duser.timezone=Asia/Tokyo` dan `America/New_York` | 49/49 berhasil di keduanya (pemetaan `java.time` tidak tergeser zona waktu JVM; diuji pada driver MariaDB) |
| Seluruh suite (model 74 + `AppInfoTest` 5 + DAO 49) | **128 berhasil, 0 gagal** |
| Tanpa `config/db.properties` | 6 kelas tes DAO **di-abort otomatis lewat JUnit Assumptions** (0 gagal) → `mvn test` tetap hijau di mesin tanpa database |
| `verify_stage2.sql` sesudah tes | LULUS \| 57; jumlah baris seed tetap 6 customer / 5 room / 7 instrument / 7 booking / 12 booking_instrument / 1 admin; 0 sisa data uji (`dao-test-%`, `Uji-%`, `adm-%`) |
| Kesesuaian signature dengan kontrak §3 | 45/45 method cocok (nama, parameter, return, `throws SQLException`), per DAO |
| Uji mutasi (12 kerusakan sengaja pada DAO) | **12 dari 12 terdeteksi** (≥ 1 tes gagal pada masing-masing) |

Mutasi yang diuji: operator overlap `<=`/`>=` (berurutan dianggap overlap), overlap tanpa filter status, overlap hanya PENDING, parameter start/end tertukar, `updateQuantity` ikut mengubah `rental_price` (dua versi; versi bersih `rental_price + 1` terdeteksi oleh assertion snapshot dan seed tetap 8000.00), `findByIdForUpdate` tanpa `FOR UPDATE`, urutan `findAll` ASC, `CustomerDAO.update` ikut mengubah `password_hash`, filter `findByStatus` dihapus, hash customer bocor ke `Booking.customer`, `AdminDAO` memakai kolom salah.

## 5. Strategi isolasi data uji

- **Seed hanya dibaca.** `reset.sql` tidak pernah dijalankan dari tes.
- **Method ber-`Connection`** diuji di dalam transaksi `autoCommit=false` yang **selalu di-rollback** (`DaoTestSupport.inRollbackTx`). Koneksi dibuka dan ditutup oleh tes, bukan DAO; tes memastikan `conn.isClosed()==false` dan `autoCommit` tetap `false` setelah pemanggilan DAO.
- **Method tanpa `Connection`** (DAO commit sendiri: insert/update/delete Customer, Room, Instrument) memakai identitas unik (UUID) dan dibersihkan di `@AfterEach`/`finally`.
- **Admin:** kontrak `AdminDAO` hanya `findByUsername`/`findById` (tanpa `insert`). Tes "insert dan find Admin" menyisipkan baris lewat SQL setup di tes (ber-parameter), lalu menghapusnya di `finally`.
- Tes **di-skip otomatis** bila database tidak terjangkau, `config/db.properties` tidak ada, atau seed belum dimuat (`Assumptions`).
- **Efek samping yang tidak dapat dihindari:** `AUTO_INCREMENT` ikut maju pada setiap insert, termasuk yang di-rollback atau dihapus (perilaku InnoDB). Data tidak terpengaruh; ID baru di aplikasi tidak lagi mulai dari 7/8. `reset → schema → seed` mengembalikannya.

## 6. Cakupan tes terhadap permintaan

| Permintaan | Tes |
|---|---|
| Insert/find Admin, Customer, Room, Instrument | `AdminDAOTest`, `CustomerDAOTest.insertAndFind`, `RoomDAOTest.insertAndQueries`, `InstrumentDAOTest.insertAndQueries` |
| Update dan delete entitas yang boleh dihapus | `*.update`, `*.deleteUnused/deleteAndRestrict` (baris uji unik) |
| Unique email | `CustomerDAOTest.duplicateEmailRejected` (duplikat baris uji dan duplikat email seed); unique username admin di `AdminDAOTest` |
| FK dan RESTRICT | hapus customer 1 / room 1 / instrument 1 → `SQLException` (SQLState 23xxx) dan baris tetap ada; insert booking dengan customer/room tidak ada ditolak; instrument terpakai tidak dapat dihapus |
| Booking `findById`, `findAll`, `findByCustomerId` | `BookingDAOTest` (booking 1, 4, 5; urutan; customer 1/2/6; JOIN customer+room; `passwordHash` null; detail alat tidak dimuat) |
| `existsOverlap`: overlap dan berurutan | sebagian, mengandung, dikandung, identik → true; berakhir = awal berikutnya (kedua arah) → false; CANCELLED, COMPLETED, room/tanggal lain → false; versi `Connection` dalam transaksi (PENDING/CONFIRMED memblokir, CANCELLED tidak) |
| `findByIdForUpdate` | membaca pada koneksi transaksi; transaksi kedua **terkunci** (lock wait timeout 1 detik) |
| `BookingInstrumentDAO` CREATE/READ/UPDATE/DELETE | `insert`, `insertAll` (batch, bookingId dari parameter), READ lewat JOIN (booking 4 dan 5), `updateQuantity`, `delete`; duplikat pasangan ditolak; FK tidak ada ditolak; CASCADE (hapus booking menghapus detail, lalu rollback) |
| Snapshot `rental_price` tidak berubah | harga instrument diubah **setelah** detail dibuat lalu `updateQuantity`; `rentalPrice` tetap 25000.00 sedangkan `instrument.rentalPrice` = 99999.00; juga seed `booking_instrument` 5 tetap 8000.00 |

## 7. Risiko dan hal yang belum terbukti

1. **MySQL Connector/J 8.4.0 belum diuji.** DAO memakai `setObject/getObject` dengan `LocalDate`, `LocalTime`, `LocalDateTime` (didukung Connector/J 8.x; sengaja tidak memakai `java.sql.Timestamp`/`Time` agar tidak terkena konversi zona waktu karena URL memakai `serverTimezone=Asia/Makassar`). Jalankan `mvn clean test` lokal untuk membuktikannya.
2. **Semantik `boolean update*`/`delete`** = `executeUpdate() > 0` bergantung pada perilaku bawaan "found rows" (`useAffectedRows=false`). Jangan menambahkan `useAffectedRows=true` ke `db.url`: `update` yang nilainya tidak berubah akan mengembalikan `false`.
3. `DatabaseConnection` asli milik 4A belum diuji bersama DAO. Tes memanggil `DatabaseConnection.getConnection()`; bila config tidak ada, tes menangkap `SQLException`, `RuntimeException`, dan `LinkageError` lalu skip, sehingga pesan error 4A boleh berupa salah satunya.
4. Tes membaca seed (booking 1, 4, 5; booking_instrument 5; customer 1; admin `admin`). Bila seed telah diubah pengguna lewat aplikasi, tes tertentu dapat gagal atau ter-skip; pulihkan dengan `reset.sql → schema.sql → seed.sql`.
5. Tes `findByIdForUpdateLocksRow` membutuhkan sekitar 1 detik (menunggu lock timeout).

## 8. Keputusan implementasi yang perlu dicatat di Decision Log (usulan, belum ditulis karena `DECISION_LOG.md` dikoordinasikan)

- **D-036 (usulan):** `Customer` di dalam `Booking` hasil query DAO diisi lewat JOIN **tanpa** `passwordHash` (`null`) agar hash tidak menyebar ke objek booking. `CustomerDAO` tetap mengembalikan `passwordHash` (dibutuhkan login).
- **D-037 (usulan):** `findAll`/`findByCustomerId` mengurutkan `booking_date DESC, start_time DESC` sesuai kontrak, ditambah `booking_id DESC` sebagai pemutus seri (booking 1 dan 3 memiliki tanggal/jam sama) agar urutan deterministik.
- **D-038 (usulan):** `BookingDAO.insert` memakai `created_at` dari model bila ada; bila `null`, kolom dihilangkan dari `INSERT` sehingga database mengisi `CURRENT_TIMESTAMP`.
- **D-039 (usulan):** `BookingInstrumentDAO` membaca lewat JOIN `booking_instrument ↔ booking ↔ instrument`; `rentalPrice` objek detail = snapshot `booking_instrument.rental_price`, `Instrument` di dalamnya membawa harga **saat ini** dari tabel `instrument`. `insertAll` memakai `bookingId` parameter dan mengabaikan `bookingId` pada objek detail.

## 9. Pemetaan method → tabel

| DAO | Method | Tabel / operasi |
|---|---|---|
| `AdminDAO` | `findByUsername`, `findById` | `admin` — SELECT |
| `CustomerDAO` | `insert` / `update` / `delete` | `customer` — INSERT (generated key) / UPDATE name, phone, email / DELETE |
| | `findById`, `findByEmail`, `findAll`, `existsByEmail`, `existsByEmailExcludingId`, `count` | `customer` — SELECT |
| `RoomDAO` | `insert` / `update` / `delete` | `room` — INSERT / UPDATE / DELETE |
| | `findById`, `findAll`, `findByStatus`, `count` | `room` — SELECT |
| | `findByIdForUpdate(conn, id)` | `room` — `SELECT ... FOR UPDATE` pada koneksi transaksi |
| `InstrumentDAO` | `insert` / `update` / `delete` | `instrument` — INSERT / UPDATE / DELETE |
| | `findById` (2 overload), `findAll`, `findByStatus`, `count` | `instrument` — SELECT |
| `BookingDAO` | `insert(conn, booking)` | `booking` — INSERT (generated key; `created_at` default bila null) |
| | `findById` (2 overload), `findAll`, `findByCustomerId` | `booking` JOIN `customer` JOIN `room` — SELECT |
| | `existsOverlap` (2 overload) | `booking` — SELECT 1: `status IN ('PENDING','CONFIRMED') AND start_time < :end AND end_time > :start` |
| | `updateStatus`, `updateTotalCost` | `booking` — UPDATE (koneksi transaksi) |
| | `countByStatus` | `booking` — `COUNT(*)` |
| `BookingInstrumentDAO` | CREATE `insert`, `insertAll` | `booking_instrument` — INSERT (batch untuk `insertAll`) |
| | READ `findByBookingId` (2), `findById`, `findByBookingIdAndInstrumentId` | `booking_instrument` JOIN `booking` JOIN `instrument` — SELECT |
| | UPDATE `updateQuantity` | `booking_instrument` — UPDATE **hanya** `quantity` |
| | DELETE `delete` | `booking_instrument` — DELETE |

## 10. Bukti AC-A (hasil pencarian teks pada `src/main/java/com/studiomusik/dao`)

| Pemeriksaan | Hasil |
|---|---|
| AC-A02: `javafx` di `dao` dan `model` | 0 temuan |
| AC-A01: `java.sql` / kata kunci SQL di `controller` | 0 temuan |
| AC-A03: `createStatement`, `Statement x =`, `executeQuery("...")`, `executeUpdate("...")` | 0 temuan; seluruh SQL lewat `prepareStatement` (AdminDAO 2, BookingDAO 8, BookingInstrumentDAO 7, CustomerDAO 9, InstrumentDAO 7, RoomDAO 8) |
| AC-A03: input pengguna disambung ke SQL | 0 temuan; string SQL hanya digabung dari konstanta (`SELECT_BASE`, `SELECT_JOINED`, `ORDER_NEWEST_FIRST`) dan semua nilai di-bind dengan `?` |
| AC-A03: try-with-resources | semua `PreparedStatement`, `ResultSet`, dan `Connection` yang dibuka DAO berada di `try (...)` |
| Method ber-`Connection` tidak menutup/commit/rollback/`setAutoCommit` | 0 temuan `commit`, `rollback`, `setAutoCommit`, `conn.close`; `DatabaseConnection.getConnection()` hanya dipanggil di method tanpa parameter `Connection` |
| Tepat enam DAO | `AdminDAO`, `CustomerDAO`, `RoomDAO`, `InstrumentDAO`, `BookingDAO`, `BookingInstrumentDAO` (+ `package-info.java` lama); tidak ada `DatabaseConnection` di workspace ini |

Status `ACCEPTANCE_CRITERIA.md` untuk AC-A01…A03 sebaiknya diperbarui koordinator setelah 4A dan 4B digabung dan `mvn clean test` lokal lulus.

## 11. Menjalankan lokal (setelah 4A digabung)

```
1. Salin config/db.properties.example -> config/db.properties; isi db.username dan db.password lokal.
2. Siapkan database:  mysql -u <user> -p < database/reset.sql
                      mysql -u <user> -p < database/schema.sql
                      mysql -u <user> -p < database/seed.sql
3. mvn clean test
```
Hasil yang diharapkan: seluruh tes lulus (79 model/lama + 49 DAO + tes 4A); tanpa `config/db.properties` atau tanpa database, enam kelas DAO ter-skip dan build tetap hijau.

## 12. Checklist

- [x] Enam DAO sesuai kontrak §3 (45 method, signature identik)
- [x] PreparedStatement + try-with-resources; tanpa penyambungan input
- [x] Method ber-Connection tidak menutup/commit/rollback/mengubah autoCommit
- [x] `BigDecimal` untuk uang; generated key pada insert; pemetaan D-014
- [x] `existsOverlap` memakai interval kontrak dan status PENDING/CONFIRMED
- [x] `findByIdForUpdate` mengunci baris (terbukti oleh tes lock)
- [x] Snapshot `rental_price` tidak berubah oleh `updateQuantity`
- [x] 49 tes DAO lulus di MySQL 8.0.46 (sandbox), seed utuh
- [ ] `mvn clean test` dengan `DatabaseConnection` 4A + MySQL Connector/J 8.4.0 (**menunggu koordinator/pengguna**)
- [ ] Pembaruan `DECISION_LOG.md` (D-036…D-039), `ACCEPTANCE_CRITERIA.md`, `PROGRESS*.md`, `README.md` (**koordinator**)
