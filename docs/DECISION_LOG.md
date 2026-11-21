# DECISION LOG

Format: **ID — judul**. Sumber konflik → Keputusan → Dampak. Status: `DECIDED` (dikunci) atau `OPEN` (perlu konfirmasi tim).
Perubahan di masa depan ditambahkan sebagai entri baru, bukan menimpa entri lama.

---

### D-001 — Aktor: Admin saja (TK I) vs Customer + Admin
- **Konflik:** Laporan TK I hanya punya aktor Admin/Pengelola dan alur "Pilih Customer"; handoff dan master prompt menetapkan revisi Customer Self-Service.
- **Keputusan:** Dua aktor: Customer (self-service) dan Admin. Alur booking Customer memakai Customer yang sedang login (bukan "Pilih Customer").
- **Dampak:** Use case dan flow di laporan akhir perlu diperbarui; ditambah login/register. `DECIDED`

### D-002 — Nama kolom password
- **Konflik:** PDM TK I memakai `admin.password`; kebutuhan keamanan menuntut hash.
- **Keputusan:** `admin.password_hash` dan `customer.password_hash` (`VARCHAR(255)`, NOT NULL). `DECIDED`

### D-003 — Autentikasi Customer
- **Konflik:** ADM TK I: `customer.email` nullable, tanpa password.
- **Keputusan:** `customer.email` `NOT NULL UNIQUE` (identitas login) dan `customer.password_hash NOT NULL`. `customer.phone` tetap NOT NULL. `DECIDED`

### D-004 — Kolom/constraint tambahan
- **Keputusan:** `booking.created_at` (audit); `UNIQUE(booking_id, instrument_id)` pada `booking_instrument`; `CHECK` untuk harga ≥ 0, `quantity` > 0, `end_time > start_time`; indeks `booking(room_id, booking_date, status)`; `booking_instrument.quantity` menjadi NOT NULL. Detail di `DATABASE_CONTRACT.md`. `DECIDED`

### D-005 — Admin tidak punya phone/email di tabel, tetapi Person punya
- **Konflik:** Class Diagram: `Person{id,name,phone,email}`; tabel `admin` hanya `admin_id, username, password, name`.
- **Keputusan:** Struktur `Person` dipertahankan. Objek `Admin` mengisi `phone` dan `email` dengan `null` (kolom tidak ada di tabel `admin`). `Admin` menambah `username` dan `passwordHash`; `Customer` menambah `passwordHash` (field autentikasi diletakkan di subclass agar kontrak `Person` TK I tidak berubah). `DECIDED`

### D-006 — Tipe uang: `double` vs `BigDecimal`
- **Konflik:** Class Diagram memakai `double`; prinsip implementasi mewajibkan `BigDecimal`.
- **Keputusan:** Semua nilai uang (`pricePerHour`, `rentalPrice`, `totalCost`, subtotal) bertipe `BigDecimal`; kolom DB `DECIMAL(10,2)`. `DECIDED`

### D-007 — Tarif alat
- **Konflik:** Handoff: "jangan mengarang; gunakan keputusan tim". Master prompt: per unit per sesi.
- **Keputusan:** Mengikuti master prompt (A-01). **Perlu konfirmasi tim.** `DECIDED` (dapat direvisi) · `OPEN` untuk konfirmasi

### D-008 — Stok alat
- **Konflik:** Rancangan awal tidak punya stok fisik.
- **Keputusan:** Tidak ada stok (A-02); keterbatasan dicatat untuk laporan. `DECIDED`

### D-009 — Pembatalan oleh Customer
- **Konflik:** Handoff mengajukan pertanyaan terbuka (rekomendasi: hanya `PENDING`), master prompt tidak memasukkannya ke kemampuan Customer.
- **Keputusan:** Tidak dibuat di v1 (A-03). Tidak ada method service untuk itu pada kontrak. `OPEN`

### D-010 — Status memblokir jadwal dan konfirmasi Admin
- **Keputusan:** `PENDING` dan `CONFIRMED` memblokir; booking baru `PENDING`; Admin yang mengonfirmasi (BR-07, BR-13, BR-14). `DECIDED`

### D-011 — Tabel transisi status
- **Keputusan:** Diusulkan di BR-14 / A-04. `OPEN` untuk konfirmasi

### D-012 — Durasi bilangan bulat jam
- **Konflik:** Master prompt: "harga per jam × durasi" tanpa menyebut pecahan; Class Diagram memakai `int` untuk jam.
- **Keputusan:** Durasi `int` jam, waktu pada menit 00 (A-05). `OPEN` untuk konfirmasi

### D-013 — Method pada Class Diagram yang butuh database
- **Konflik:** Class Diagram memuat `Booking.checkAvailability()`, `Booking.saveBooking()`, `Customer.createBooking()`, `Customer.viewBookingHistory()`, `Admin.manage*()`; arsitektur berlapis melarang model mengakses DB.
- **Keputusan:** Model hanya berisi perhitungan murni dan pengecekan status (`calculateDuration`, `calculateTotalCost`, `addInstrument`, `removeInstrument`, `calculateSubtotal`, `Room/Instrument.isAvailable`, `calculateCost`). Perilaku yang butuh DB direalisasikan di Service: `BookingService.isRoomAvailable` (≙ `checkAvailability`), `BookingService.createBooking` (≙ `saveBooking`/`createBooking`), `BookingService.getBookingsByCustomer` (≙ `viewBookingHistory`), `*Service` Admin (≙ `manage*`). Pemetaan ini ditulis di laporan akhir. `DECIDED`

### D-014 — Nama atribut Java vs kolom DB
- **Keputusan:** Atribut Java mengikuti Class Diagram (camelCase: `roomName`, `roomType`, `pricePerHour`, `rentalPrice`, `bookingId`, `instrumentId`, dst.); kolom DB mengikuti ADM (`name`, `type`, `price_per_hour`, `rental_price`, ...). Pemetaan dilakukan di DAO. Atribut `Room.roomId` pada diagram (tipe tidak tertulis) ditetapkan `int`. `DECIDED`

### D-015 — Lokasi resource dan config
- **Konflik:** Handoff: `resources/view`, `resources/css`, `resources/db.properties.example`. Master prompt: `src/main/resources/com/studiomusik/view|css` dan `config/db.properties.example`.
- **Keputusan:** Mengikuti master prompt (lebih baru dan lebih spesifik). File SQL tetap di `database/` pada root proyek (schema/seed/reset, STEP 2). `DECIDED`

### D-016 — Controller yang dibuat bertahap
- **Konflik:** Handoff memuat daftar 10 controller; STEP 1 hanya meminta `MainController` starter.
- **Keputusan:** Controller lain dibuat pada STEP 7; `MainController` starter dipertahankan atau diganti saat navigasi dibuat (dicatat saat itu). `DECIDED`

### D-017 — Penanganan error lintas layer
- **Keputusan:** DAO melempar `SQLException`. Service menangkapnya dan melempar `ServiceException` (unchecked) beserta subclass `ValidationException` dan `BusinessRuleException`, semuanya di package `com.studiomusik.service` (tidak menambah package baru). Controller hanya menangkap `ServiceException` dan menampilkan pesannya. `DECIDED`

### D-018 — Hashing password
- **Keputusan:** PBKDF2WithHmacSHA256 (JDK, tanpa dependency baru). Format `pbkdf2_sha256$<iterasi>$<saltBase64>$<hashBase64>` (A-11). Seed Admin harus berisi hash hasil generate, bukan plaintext (STEP 2). `DECIDED`

### D-019 — Concurrency pada pengecekan overlap
- **Keputusan:** Dalam transaksi `createBooking`, baris `room` dikunci (`SELECT ... FOR UPDATE`) sebelum cek overlap, agar dua Customer tidak lolos bersamaan. `DECIDED`

### D-020 — Aksi hapus
- **Keputusan:** FK `booking.customer_id` dan `booking.room_id` `ON DELETE RESTRICT`; `booking_instrument.instrument_id` `RESTRICT`; `booking_instrument.booking_id` `CASCADE`. Booking tidak dihapus lewat aplikasi (A-08). `DECIDED`

### D-021 — Versi dependency (STEP 1)
- **Keputusan:** JavaFX 21.0.5, MySQL Connector/J 8.4.0, JUnit Jupiter 5.10.2, maven-compiler-plugin 3.13.0, maven-surefire-plugin 3.2.5, javafx-maven-plugin 0.0.8. **Catatan:** versi ini belum dapat diverifikasi terhadap Maven Central dari lingkungan pembuatan (akses jaringan ke Maven Central diblokir); lihat `docs/STAGE1_VERIFICATION.md`. `DECIDED` (verifikasi lokal diperlukan)

### D-022 — Dokumen handoff
- **Keputusan:** `PROGRESS_HANDOFF.md` adalah satu-satunya ringkasan handoff hidup lintas akun. `docs/PROJECT_HANDOFF_PBO.md` adalah konteks proyek yang statis (versi terkunci dari handoff awal) dan `PROGRESS.md` adalah log tahap singkat yang menunjuk ke `PROGRESS_HANDOFF.md`. Tidak ada file handoff lain. `DECIDED`

---
## Entri STEP 2 (2026-11-19)

### D-023 — Parameter PBKDF2 dan desain utilitas hash
- **Melengkapi D-018** (format dan algoritma tidak berubah: `pbkdf2_sha256$iter$salt$hash`, PBKDF2WithHmacSHA256, salt 16 byte `SecureRandom`, kunci 256 bit).
- **Keputusan:** jumlah iterasi bawaan **600000** (mengikuti rekomendasi OWASP untuk PBKDF2-HMAC-SHA256); tersimpan di dalam string hash sehingga dapat dinaikkan kelak tanpa merusak hash lama. `Pbkdf2HashGenerator` tidak menerima password lewat argumen CLI (menghindari riwayat shell); input lewat konsol tersembunyi atau `--stdin`. Mode `--verify` ditambahkan untuk memeriksa hash. `PasswordUtil` (STEP 4) wajib memakai parameter dan format yang sama dan mampu membaca hash dengan iterasi berbeda. `DECIDED`

### D-024 — Kredensial demo tidak disimpan di repository
- **Konflik:** prompt STEP 2 dari handoff STEP 1 menyebut kredensial demo "ditulis di README"; instruksi STEP 2 terbaru melarang plaintext di repository.
- **Keputusan:** mengikuti instruksi terbaru. Repository hanya berisi hash. Username/email demo (bukan rahasia) boleh tercantum; **password demo disampaikan terpisah** oleh tim. Untuk mengganti password demo: buat hash baru dengan utilitas dan ganti nilai di `seed.sql`. `DECIDED`

### D-025 — Kebijakan idempotensi script SQL
- **Keputusan:** `schema.sql` memakai `IF NOT EXISTS` (aman diulang, tidak menghapus data). `seed.sql` mengharuskan tabel kosong dan **sengaja gagal** bila dijalankan lagi tanpa reset (error duplikat), agar data tidak ganda diam-diam. `reset.sql` hanya `DROP TABLE` (urutan anak → induk) dan tidak menghapus database `studio_musik`. Alur ulang resmi: `reset.sql` → `schema.sql` → `seed.sql`. `DECIDED`

### D-026 — Skrip verifikasi otomatis (file tambahan)
- **Keputusan:** menambah `database/tests/verify_stage2.sql` (di luar daftar file wajib prompt) agar bukti uji dapat diulang oleh siapa pun. Skrip memakai database sementara `studio_musik_verify_tmp` (dihapus di akhir) dan membungkus uji destruktif dalam transaksi + `ROLLBACK`. Tidak mengubah kontrak. `DECIDED`

### D-027 — Tanggal tetap pada seed
- **Keputusan:** booking seed memakai tanggal tetap (10 Nov dan 1–5 Des 2026) supaya skenario overlap deterministik. Booking COMPLETED berada di masa lalu; booking uji lain pada Desember 2026. Setelah tanggal-tanggal itu lewat, data tetap valid sebagai data historis; uji Service yang membuat booking baru (BR-21) harus memakai tanggal relatif terhadap hari ini atau tanggal yang jelas di masa depan, bukan tanggal seed. `DECIDED`

### D-028 — Perubahan status D-021 (versi dependency)
- **Keputusan:** build lokal pengguna pada 2026-11-19 (`mvn clean test` dan `mvn package` sukses dengan versi yang dikunci) membuktikan dependency terunduh dan versi dapat diresolusi. Tidak ada perubahan versi. `DECIDED`

---
## Entri STEP 3 (2026-11-19)

### D-029 — Konvensi pelanggaran invariant di Model dan semantik enum
- **Keputusan:** Model hanya menjaga invariant yang murni dan murah. Argumen tidak valid → `IllegalArgumentException` (mis. harga/jam negatif, `quantity < 1`, jam negatif); `null` pada objek wajib (status enum, harga `BigDecimal`, instrument pada detail) → `NullPointerException` lewat `Objects.requireNonNull`; keadaan objek yang tidak memungkinkan perhitungan (waktu kosong, `end <= start`, menit bukan 00, room kosong) → `IllegalStateException`. Validasi input pengguna (string kosong, format email, dst.) tetap tugas Service. Service menerjemahkan exception model ke `ValidationException`/`BusinessRuleException` (D-017); model tidak mengenal exception milik Service.
- `BookingStatus.canTransitionTo`: perpindahan ke status yang sama dan ke `null` bernilai `false`. `blocksSchedule()` hanya `true` untuk PENDING dan CONFIRMED. `DECIDED`

### D-030 — `Booking.addInstrument` / `removeInstrument` sebagai pertahanan berlapis
- **Keputusan:** `addInstrument(Instrument, int)` menolak (`IllegalArgumentException`): instrument `null`, `quantity < 1` (BR-18), instrument `MAINTENANCE` (BR-06), dan instrument yang sama berdasarkan `instrumentId` (BR-17). `removeInstrument(int)` menolak id yang tidak ada pada booking. Service **tetap** wajib memeriksa aturan yang sama terlebih dulu agar pesan ke pengguna ramah dan agar aturan tidak bergantung pada model. Detail yang dimuat DAO dari database tidak lewat `addInstrument` (memakai konstruktor `BookingInstrument` dan `setInstruments`), sehingga alat yang kini MAINTENANCE pada booking lama tetap dapat dimuat. `DECIDED`
- Catatan uji: penghapusan cek `quantity < 1` di `addInstrument` tidak terdeteksi oleh tes karena konstruktor `BookingInstrument` menegakkan aturan yang sama (mutan setara), bukan celah tes.

### D-031 — Snapshot harga bersifat tidak dapat diubah (BR-12)
- **Keputusan:** pada `BookingInstrument`, `rentalPrice` dan `instrument` adalah field `final` tanpa setter. Hanya `quantity` (setter memvalidasi ≥ 1), `bookingInstrumentId`, dan `bookingId` yang dapat diubah. `Booking.addInstrument` mengisi `rentalPrice` dari `instrument.getRentalPrice()` saat itu. Ini memperjelas API_CONTRACTS §1.3 tanpa mengubah nama atau tipe atribut. `DECIDED`

### D-032 — Perilaku `calculateDuration()` dan `calculateTotalCost()`
- **Keputusan:** `calculateDuration()` mengembalikan jam bulat dan melempar `IllegalStateException` bila waktu kosong, `end <= start` (BR-03), atau mulai/selesai tidak tepat pada jam (BR-04, A-05). Data lama di database yang menyimpan waktu tidak bulat akan gagal dihitung secara eksplisit (bukan dibulatkan diam-diam); Service/Controller tidak boleh memanggilnya untuk baris seperti itu tanpa penanganan. `calculateTotalCost()` bersifat murni (tidak mengubah field `totalCost`; Service yang memanggil `setTotalCost`), membulatkan **per komponen** (biaya ruangan dan tiap subtotal) dengan HALF_UP skala 2 lalu menjumlahkannya, dan memastikan hasil berskala 2. `DECIDED`

### D-033 — Komposisi Booking–BookingInstrument di memori
- **Keputusan:** `Booking.getInstruments()` mengembalikan tampilan baca-saja (`Collections.unmodifiableList`); perubahan hanya lewat `addInstrument`, `removeInstrument`, atau `setInstruments(List)` (menyalin list, menggantikan isi lama, dipakai DAO setelah JOIN). `setBookingId(int)` menyebarkan id ke seluruh detail agar detail mengikuti identitas induknya. `DECIDED`

### D-034 — Konstruktor dan method tambahan di luar contoh signature
- **Keputusan (aditif, tidak mengubah nama/tipe yang dikunci):** konstruktor lengkap untuk `Room(int,String,String,BigDecimal,RoomStatus)`, `Instrument(int,String,String,BigDecimal,InstrumentStatus)`, `BookingInstrument(int,int,Instrument,int,BigDecimal)`, dan `Booking(int,Customer,Room,LocalDate,LocalTime,LocalTime,BookingStatus,BigDecimal,LocalDateTime)` untuk dipakai DAO; konstruktor kenyamanan `Booking(Customer,Room,LocalDate,LocalTime,LocalTime)` untuk booking baru (id 0, status PENDING sesuai BR-13, total `0.00`, `createdAt` null). `id = 0` berarti belum disimpan. Getter/setter standar untuk semua atribut kecuali yang dibatasi pada D-031 dan D-033. Sengaja **tidak** ada `equals/hashCode/toString` (tidak dibutuhkan; juga mencegah `passwordHash` bocor lewat `toString` otomatis). `DECIDED`

### D-035 — Pembagian util per tahap
- **Konteks:** judul tahap "Util JDBC & DAO" tidak merinci util mana. Lima util pada API_CONTRACTS §2 punya kebutuhan berbeda.
- **Keputusan:** STEP 4 mengerjakan `DatabaseConnection`, `PasswordUtil` (kompatibel dengan `database/tools/Pbkdf2HashGenerator.java`), dan enam DAO. `DateTimeUtil` dikerjakan pada STEP 5 (Service; validasi waktu dan overlap), `CurrencyUtil` dan `SessionManager` pada STEP 6–7 (GUI/Controller), yaitu tahap pertama yang memakainya. `DECIDED`
