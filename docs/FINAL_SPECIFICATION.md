# FINAL SPECIFICATION — Sistem Pengelolaan Operasional Studio Musik

> Status: **TERKUNCI (STEP 0)** — tanggal 2026-11-18
> Sumber: Laporan PBO Tugas Kelompok I, Instruksi Tugas Kelompok II PBO (handoff), Master Prompt STEP 0 & 1.
> Perubahan setelah ini wajib dicatat di `docs/DECISION_LOG.md`.

## 1. Identitas Proyek

| Item | Nilai |
|---|---|
| Nama sistem | Sistem Pengelolaan Operasional Studio Musik |
| Jenis | Aplikasi **desktop** Java (bukan website, bukan layanan booking via browser/HP) |
| Mata kuliah | Pemrograman Berorientasi Objek — Tugas Kelompok II (Kelompok 1, Kelas D) |
| Dosen | I Made Satria Bimantara, S.Kom., M.Kom. |
| Scope | **Satu** studio musik (bukan multi-cabang) dengan beberapa ruangan, instrumen, pelanggan, dan pemesanan |
| Bahasa / JDK | Java 21 |
| GUI | JavaFX 21 + FXML (dirancang dengan Scene Builder), non-modular (tanpa `module-info.java`) |
| Build | Maven (`groupId com.studiomusik`, `artifactId studio-musik-app`, `1.0.0-SNAPSHOT`) |
| Database | MySQL 8.0+, database `studio_musik`, driver MySQL Connector/J |
| Arsitektur | View → Controller → Service → DAO → MySQL |
| IDE target | Visual Studio Code |

## 2. Persyaratan Wajib Akademik (dari instruksi TK II)

1. Bahasa Java. 2. Aplikasi desktop. 3. Framework GUI (JavaFX). 4. Minimal lima konsep PBO (dipetakan 7: lihat §6).
5. MySQL. 6. CRUD pada relasi Many-to-Many (`booking_instrument`). 7. Pengujian aplikasi.
8. Deliverable: laporan akhir, screenshot, source code ZIP, README, peer review.
Fitur tambahan tidak boleh mengganggu pemenuhan persyaratan wajib.

## 3. Aktor dan Kemampuan

### 3.1 Customer
- Register dan login (identitas login: **email**).
- Melihat daftar ruangan beserta harga per jam, dan daftar alat beserta harga sewa.
- Memilih tanggal dan waktu booking; memeriksa ketersediaan ruangan.
- Memilih alat musik beserta jumlah (opsional).
- Melihat estimasi total biaya sebelum menyimpan.
- Membuat booking mandiri (status awal `PENDING`).
- Melihat status dan riwayat booking **miliknya sendiri**.
- Customer **tidak** dapat melihat/mengubah booking Customer lain.

### 3.2 Admin
- Login dan logout (identitas login: **username**). Akun Admin disediakan lewat seed, bukan self-register.
- Melihat dashboard operasional (ringkasan jumlah Customer, Room, Instrument, dan Booking per status).
- Mengelola data Customer, Room, Instrument (CRUD).
- Melihat seluruh Booking dan riwayatnya.
- Mengelola status Booking (transisi status, lihat BR-14).
- Mengelola detail alat pada Booking (CRUD pada `booking_instrument`: tambah, lihat, ubah quantity, hapus).

## 4. Aturan Bisnis (BR)

| ID | Aturan |
|---|---|
| BR-01 | Satu Booking terkait tepat satu Customer dan satu Room. |
| BR-02 | Satu Booking memiliki `bookingDate`, `startTime`, `endTime` pada hari yang sama (tidak lintas tengah malam). |
| BR-03 | `endTime` harus **lebih besar** dari `startTime`. |
| BR-04 | Durasi adalah bilangan bulat jam; `startTime` dan `endTime` berada pada menit 00 (lihat A-05). |
| BR-05 | Room berstatus `MAINTENANCE` tidak dapat dipesan. |
| BR-06 | Instrument berstatus `MAINTENANCE` tidak dapat dipilih. |
| BR-07 | Booking `PENDING` dan `CONFIRMED` **memblokir** jadwal. `CANCELLED` dan `COMPLETED` **tidak** memblokir. |
| BR-08 | Booking yang bertumpang tindih pada Room dan tanggal yang sama ditolak. Rumus overlap: `existingStart < requestedEnd AND existingEnd > requestedStart`. Booking berurutan tanpa tumpang tindih (mis. 10:00–12:00 lalu 12:00–14:00) **diperbolehkan**. |
| BR-09 | Biaya ruangan = `pricePerHour × durasi (jam)`. |
| BR-10 | Biaya alat = `quantity × rentalPrice` (harga sewa **per unit per sesi**, lihat A-01). |
| BR-11 | `totalCost` = biaya ruangan + jumlah biaya semua alat. Semua nilai uang memakai `BigDecimal`, skala 2, `RoundingMode.HALF_UP`. |
| BR-12 | Harga sewa alat dicatat pada `booking_instrument.rental_price` saat alat ditambahkan (snapshot). Perubahan harga di tabel `instrument` tidak mengubah booking lama. Update detail hanya mengubah `quantity`; snapshot tidak diubah. |
| BR-13 | Booking baru dibuat dengan status `PENDING`. |
| BR-14 | Transisi status yang diizinkan (Admin): `PENDING→CONFIRMED`, `PENDING→CANCELLED`, `CONFIRMED→COMPLETED`, `CONFIRMED→CANCELLED`. `COMPLETED` dan `CANCELLED` bersifat final (lihat A-04). |
| BR-15 | Penyimpanan Booking beserta seluruh detail `booking_instrument` memakai **satu transaksi** database (semua berhasil atau semua dibatalkan/rollback). |
| BR-16 | Customer hanya dapat mengakses booking miliknya sendiri (ditegakkan di Service, bukan hanya di UI). |
| BR-17 | Pasangan (`booking_id`, `instrument_id`) unik. Menambahkan alat yang sudah ada pada booking ditolak; gunakan operasi ubah quantity. |
| BR-18 | `quantity` ≥ 1 (bilangan bulat). |
| BR-19 | Detail alat hanya dapat diubah saat Booking berstatus `PENDING` atau `CONFIRMED`. Setiap perubahan menghitung ulang `totalCost` dalam transaksi yang sama. |
| BR-20 | Password tidak pernah disimpan plaintext (hash, kolom `password_hash`). Email Customer unik dan wajib; username Admin unik. |
| BR-21 | `bookingDate` tidak boleh di masa lalu saat Booking dibuat (lihat A-06). |
| BR-22 | Room, Instrument, atau Customer yang sudah dipakai Booking tidak dapat dihapus (FK `RESTRICT`); Service mengembalikan pesan yang jelas dan menyarankan status `MAINTENANCE` untuk Room/Instrument. |

## 5. Asumsi (A) — perlu konfirmasi tim

| ID | Asumsi | Status |
|---|---|---|
| A-01 | Tarif alat **per unit per sesi booking**, bukan per jam. Rancangan awal tidak menentukan; ditetapkan oleh master prompt. | Ditetapkan, **tim konfirmasi** sebelum STEP 5 |
| A-02 | Tidak ada kolom stok fisik alat. Tidak ada batas atas `quantity` selain ≥ 1. Konsekuensi: dua booking berbeda pada jam sama dapat memesan alat yang sama tanpa pengecekan stok. Ini keterbatasan yang diketahui dan akan ditulis di laporan. | Keterbatasan diterima |
| A-03 | Pembatalan oleh Customer **tidak** ada di v1 (daftar kemampuan Customer pada master prompt tidak memuatnya). Rekomendasi handoff jika tim menginginkan: hanya untuk booking `PENDING`. | **Terbuka** |
| A-04 | Tabel transisi status pada BR-14 diusulkan karena handoff hanya menyebut "Admin mengelola status". | Diusulkan, **tim konfirmasi** |
| A-05 | Durasi bilangan bulat jam, mengikuti `calculateDuration(): int` dan `calculateCost(hours: int)` pada Class Diagram TK I. Jika tim ingin durasi pecahan, ubah dengan entri baru di Decision Log. | Diusulkan, **tim konfirmasi** |
| A-06 | Booking tidak boleh dibuat untuk tanggal yang sudah lewat. | Diusulkan |
| A-07 | Admin tidak membuat/mengubah jadwal Booking atas nama Customer pada v1; Admin mengelola status dan detail alat saja (sesuai daftar kemampuan Admin pada handoff). | Ditetapkan |
| A-08 | Booking tidak dihapus permanen lewat aplikasi; pembatalan dilakukan dengan status `CANCELLED`. | Ditetapkan |
| A-09 | Tidak ada fitur reset/ubah password pada v1. | Ditetapkan |
| A-10 | Waktu memakai `LocalDate`/`LocalTime` tanpa zona (zona sistem, WITA). | Ditetapkan |
| A-11 | Keamanan password memakai PBKDF2 dari JDK (tanpa dependency tambahan); format hash dikunci di API_CONTRACTS. | Ditetapkan |

## 6. Pemetaan Konsep PBO ke Kode Nyata

| Konsep | Pemetaan |
|---|---|
| Encapsulation | Semua atribut model `private`, akses via getter/setter dengan validasi dasar di setter bila perlu. |
| Abstraction | `Person` adalah `abstract class` dengan `abstract String getRole()`. |
| Inheritance | `Customer extends Person`, `Admin extends Person`. |
| Polymorphism | `getRole()` **dioverride** di `Customer` (`"Customer"`) dan `Admin` (`"Admin"`); `SessionManager` menyimpan `Person` dan memakai `getRole()` secara polimorfik. |
| Association | `Booking` → `Customer`, `Booking` → `Room`, `BookingInstrument` → `Instrument`. |
| Composition | `Booking` memiliki `List<BookingInstrument>`; siklus hidup detail mengikuti Booking (`ON DELETE CASCADE`). |
| Enumeration | `BookingStatus`, `RoomStatus`, `InstrumentStatus`. |

## 7. Di Luar Scope v1
Multi-cabang, pembayaran/payment gateway, antarmuka web/mobile, stok fisik alat, reset password, pembatalan oleh Customer (A-03), laporan/ekspor, notifikasi.

## 8. Batasan Klaim
- Jangan menyebut sistem sebagai website atau pemesanan online dari browser/HP.
- Jangan mengklaim fitur selesai tanpa bukti (hasil pengujian nyata/screenshot).
