# GUI CONTRACT — SONORA (STEP 6)

> Status: **STEP 6 selesai (hanya View). Controller belum dibuat; dibuat pada STEP 7.**
> Sumber visual: `Spesifikasi_GUI.pdf` (GUI Design Specification V1.0). Backend: `API_CONTRACTS.md` §2 & §4 (tidak berubah), `ARCHITECTURE_CONTRACT.md`.
> Lokasi: `src/main/resources/com/studiomusik/view/*.fxml` dan `src/main/resources/com/studiomusik/css/app.css`. `main-view.fxml` (starter STEP 1) dipertahankan sebagai referensi dan tetap memakai `MainController`.
> Keterbatasan: FXML belum dimuat di JavaFX pada STEP 6 (tidak ada controller); FXML hanya diperiksa sebagai XML yang well-formed dan konsistensi `fx:id`/handler-nya dengan dokumen ini.

## 1. Deviasi dan keputusan GUI (usulan D-GUI-xx; belum masuk `DECISION_LOG.md`)
| ID | Keputusan / deviasi | Alasan |
|---|---|---|
| **D-GUI-01** | **Deviasi dari Spesifikasi GUI §8.1 / §12.2 ("Customer tidak wajib login").** Landing, katalog studio/fasilitas, Cara Booking, dan pencarian jadwal Quick Booking terbuka **tanpa login**. Namun **mengirim booking mewajibkan login/registrasi Customer**: `BookingService.createBooking` membutuhkan `customerId` dan `booking.customer_id` adalah FK wajib. Tidak ada guest booking, tidak ada perubahan backend/DB. | Kontrak backend & DB (STEP 0–5) terkunci; guest booking palsu melanggar skema. Perlu konfirmasi tim/dosen. |
| D-GUI-02 | Satu aplikasi JavaFX; Landing, Login, Register, dan dua dashboard adalah *scene* terpisah yang berpindah lewat controller (navigasi STEP 7). Dashboard Admin dan Customer tidak berbagi layar (AC-G03). | Spesifikasi §12.3 meminta keputusan. |
| D-GUI-03 | Navbar publik disusun **horizontal** satu baris (usulan §5.2 spesifikasi), bukan vertikal seperti screenshot referensi. | Usulan resmi di spesifikasi; lebih efisien di desktop. |
| D-GUI-04 | Quick Booking hanya memiliki Tanggal, Durasi, Jenis Studio (sesuai referensi). Backend hanya punya `isRoomAvailable(roomId, date, start, end)`, sehingga "Cari Jadwal" dilakukan controller dengan memeriksa tiap ruangan AVAILABLE berjenis itu pada tiap jam mulai bulat. Rentang jam **belum diputuskan** (jam operasional tidak ada di spesifikasi); default usulan: semua jam bulat yang lolos Service (sesi terakhir berakhir 23:00, D-040). | Tidak mengubah kontrak; jam operasional butuh konfirmasi. |
| D-GUI-05 | Login tanpa pilihan peran: identifier berisi `@` → Customer (email), selain itu Admin (username). Ini routing UI, bukan autentikasi; verifikasi tetap di `AuthService`. | Memenuhi "username/email" dalam satu field. |
| D-GUI-06 | CRUD Customer/Ruangan/Instrumen dan CRUD detail alat booking memakai **form/panel inline** (bukan dialog). Konfirmasi hapus memakai `Alert` standar dari controller. | Semua kontrol terdokumentasi di FXML; sedikit kelas dialog. |
| D-GUI-07 | Field "Ulangi Password" ditambahkan di Register (validasi UX di controller sesuai `ARCHITECTURE_CONTRACT` §4.5). Aturan password sesungguhnya (8–128) tetap di Service. | Mengurangi salah ketik. |
| D-GUI-08 | Wordmark: SONO putih + RA `#E87932` pada latar gelap; pada latar terang SONO `#292522` (`styleClass="on-light"`). Deskriptor "MUSIC STUDIO & RENT" sesuai screenshot (varian "RENTAL" menunggu keputusan). Admin memakai deskriptor "ADMIN — PENGELOLAAN OPERASIONAL". | Spesifikasi §4. |
| D-GUI-09 | **Tidak ada aset foto/font di repo.** Hero memakai gradient charcoal–oranye; `heroImageView` disiapkan tetapi tersembunyi. CSS memakai `"Poppins"` dan `"Roboto"` (satu keluarga per aturan); bila font belum dimuat/terpasang, JavaFX memakai font default sistem. Tidak ada font palsu/unduhan. Bobot 600/800 hanya presisi bila file TTF statis terkait dimuat (`Font.loadFont`) pada STEP 7/8. | Instruksi STEP 6. |
| D-GUI-10 | Token warna ditulis sebagai *looked-up colors* di `.root` (`-sonora-*`) karena JavaFX CSS tidak mendukung `--var`. Status ditampilkan dengan teks + warna (tidak hanya warna; spesifikasi §11.5). | Spesifikasi §9.4, §11. |
| D-GUI-11 | Controller memakai `fx:controller` di bawah (nama sesuai instruksi STEP 6). Rencana nama di `ARCHITECTURE_CONTRACT` §3 (`BookingController`, `MyBookingController`, `RoomController`, dst.) **digantikan** oleh 5 controller berikut dengan pembagian tanggung jawab sama; perlu dicatat di Decision Log. | Satu FXML per dashboard. |

## 2. Aturan umum View
- FXML **tidak** berisi SQL, aturan bisnis, perhitungan harga, atau autentikasi. Teks statis hanya label/petunjuk; angka (harga, total, jumlah) diisi controller dari Service.
- Label `*MessageLabel`, `messageLabel`, `availabilityLabel` tersembunyi (`visible=false`, `managed=false`) sampai controller menampilkannya; controller menambah satu styleClass: `message-error`, `message-success`, `message-info`, atau `message-warning` (hapus yang lama saat ganti).
- `TableColumn` tidak punya *cell value factory* di FXML; controller memasangnya. `TableView` memakai `CONSTRAINED_RESIZE_POLICY` (kolom menyesuaikan lebar, tanpa clipping).
- Lencana status: controller membuat `Label` di sel tabel dengan styleClass `badge` + `status-pending|confirmed|completed|cancelled|available|maintenance` dan teks status.
- Ukuran jendela usulan STEP 7: awal 1180×760, minimum 960×640 (ukuran minimum belum final di spesifikasi §12.3).
- Handler ditulis `#nama`; controller wajib punya `@FXML private void nama()` (boleh dengan `ActionEvent`).
- `Spinner<Integer>` dibuat di FXML (min 1, max 99, initial 1); tipe generik `ComboBox`/`TableView` ditentukan controller.

## 3. Ringkasan berkas
| FXML | `fx:controller` | Fungsi | fx:id | handler unik |
|---|---|---|---|---|
| `landing-view.fxml` | `com.studiomusik.controller.LandingController` | Landing Page publik (tanpa login) | 33 | 9 |
| `login-view.fxml` | `com.studiomusik.controller.LoginController` | Login Admin & Customer | 7 | 3 |
| `register-view.fxml` | `com.studiomusik.controller.RegisterController` | Registrasi Customer | 9 | 2 |
| `customer-dashboard-view.fxml` | `com.studiomusik.controller.CustomerDashboardController` | Dashboard Customer (Katalog, Booking, Riwayat) | 59 | 11 |
| `admin-dashboard-view.fxml` | `com.studiomusik.controller.AdminDashboardController` | Dashboard Admin (Ringkasan, Booking, Customer, Ruangan, Instrumen) | 92 | 19 |

## 4. `landing-view.fxml`
- **Deskripsi:** Landing Page publik (tanpa login)
- **`fx:controller`:** `com.studiomusik.controller.LandingController`

**Alur.** Browsing publik tanpa login (`RoomService.getAllRooms`, `InstrumentService.getAllInstruments`, hanya baca). `loginButton` → Login. Slot jadwal yang dipilih → Login/Register bila `SessionManager.isLoggedIn()` salah (D-GUI-01).

### fx:id
| fx:id | Tipe | Fungsi |
|---|---|---|
| `rootPane` | BorderPane | Kontainer akar; dipakai controller untuk memperoleh Scene/Stage saat navigasi. |
| `navHomeButton` | Button | Tombol "Beranda". Handler `onNavHome`. |
| `navStudioButton` | Button | Tombol "Studio". Handler `onNavStudio`. |
| `navFacilityButton` | Button | Tombol "Fasilitas". Handler `onNavFacility`. |
| `navHowToButton` | Button | Tombol "Cara Booking". Handler `onNavHowTo`. |
| `navContactButton` | Button | Tombol "Kontak". Handler `onNavContact`. |
| `loginButton` | Button | Tombol "Login". Handler `onLoginClick`. |
| `landingScroll` | ScrollPane | ScrollPane utama; dipakai untuk menggulung ke bagian (`setVvalue`) saat menu navigasi diklik. |
| `contentBox` | VBox | VBox isi halaman; dipakai menghitung posisi gulir tiap bagian. |
| `heroSection` | StackPane | StackPane hero (background gradient). Pada STEP 7 dapat memasang foto lokal bila aset ada. |
| `heroImageView` | ImageView | Opsional: foto hero. Tersembunyi (`visible=false, managed=false`); controller hanya mengaktifkan bila resource gambar lokal ada. Tidak ada aset gambar pada STEP 6. |
| `startButton` | Button | Tombol "Mulai Sekarang". Handler `onStartClick`. |
| `bookingDatePicker` | DatePicker | Pemilih tanggal booking (tidak dapat diketik; nilai `LocalDate`). |
| `durationComboBox` | ComboBox | Pilihan durasi dalam jam bulat (diisi controller, mis. 1–8 jam, default 2). |
| `studioTypeComboBox` | ComboBox | Pilihan jenis studio; diisi controller dari nilai `Room.getRoomType()` distinct milik `RoomService.getBookableRooms()`. |
| `searchScheduleButton` | Button | Tombol "Cari Jadwal Studio  →". Handler `onSearchSchedule`. |
| `quickBookingMessageLabel` | Label | Pesan validasi quick booking (tanggal/durasi/jenis wajib; tidak ada jadwal). |
| `scheduleResultBox` | VBox | Wadah hasil pencarian jadwal (tersembunyi sampai ada hasil). |
| `scheduleResultTitleLabel` | Label | Judul hasil pencarian jadwal. |
| `scheduleResultPane` | FlowPane | FlowPane tombol/chip slot jadwal tersedia yang dibuat dinamis oleh controller. |
| `studioSection` | VBox | Bagian Studio (target gulir). |
| `studioCardsPane` | FlowPane | FlowPane kartu ruangan; kartu dibuat dinamis dari `RoomService.getAllRooms()` (nama, tipe, harga/jam, status). |
| `studioEmptyLabel` | Label | Teks bila tidak ada data ruangan. |
| `facilitySection` | VBox | Bagian Fasilitas (target gulir). |
| `facilityCardsPane` | FlowPane | FlowPane kartu instrumen; dibuat dinamis dari `InstrumentService.getAllInstruments()`. |
| `facilityEmptyLabel` | Label | Teks bila tidak ada data instrumen. |
| `howToSection` | VBox | Bagian Cara Booking (target gulir; konten statis). |
| `registerLinkButton` | Button | Tautan ke halaman registrasi Customer. Handler `onRegisterClick`. |
| `contactSection` | VBox | Bagian Kontak (target gulir). |
| `contactAddressLabel` | Label | Alamat studio. Data belum ada di backend; default "Belum diatur oleh pengelola." (jangan mengarang data). |
| `contactPhoneLabel` | Label | Nomor kontak (sama: belum ada sumber data). |
| `contactEmailLabel` | Label | Email kontak (sama). |
| `contactHoursLabel` | Label | Jam operasional (sama; belum diputuskan). |

### Handler `onAction`
| Handler | Dipasang pada | Fungsi |
|---|---|---|
| `onNavHome` | `navHomeButton` | Gulir ke Beranda. |
| `onNavStudio` | `navStudioButton` | Gulir ke Studio. |
| `onNavFacility` | `navFacilityButton` | Gulir ke Fasilitas. |
| `onNavHowTo` | `navHowToButton` | Gulir ke Cara Booking. |
| `onNavContact` | `navContactButton` | Gulir ke Kontak. |
| `onLoginClick` | `loginButton` | Landing: buka Login. Login: validasi bentuk (tidak kosong) → bila identifier mengandung `@` panggil `AuthService.loginCustomer`, selain itu `loginAdmin`; sukses → `SessionManager.login` + buka dashboard sesuai peran; gagal → tampilkan `getMessage()`. |
| `onStartClick` | `startButton` | CTA "Mulai Sekarang": gulir ke Quick Booking / fokus ke DatePicker. |
| `onSearchSchedule` | `searchScheduleButton` | Validasi tanggal/durasi/jenis wajib; panggil `BookingService.isRoomAvailable` untuk tiap ruangan AVAILABLE berjenis itu pada tiap jam bulat; tampilkan slot di `scheduleResultPane`. Memilih slot: bila belum login → Login/Register (D-GUI-01), bila Customer login → Dashboard Customer tab Booking dengan data terisi. |
| `onRegisterClick` | `registerLinkButton` | Landing/Login: buka Register. Register: validasi UX (kosong, password = ulangi) lalu `AuthService.registerCustomer`; sukses → pesan sukses dan arahkan ke Login. |

## 5. `login-view.fxml`
- **Deskripsi:** Login Admin & Customer
- **`fx:controller`:** `com.studiomusik.controller.LoginController`

**Alur.** Admin → `admin-dashboard-view.fxml`; Customer → `customer-dashboard-view.fxml`. Pesan gagal login berasal dari Service (generik); controller tidak membedakan akun tidak ada vs password salah.

### fx:id
| fx:id | Tipe | Fungsi |
|---|---|---|
| `rootPane` | StackPane | Kontainer akar; dipakai controller untuk memperoleh Scene/Stage saat navigasi. |
| `messageLabel` | Label | Area pesan validasi/error (disembunyikan sampai ada pesan). Controller menambah styleClass `message-error`/`message-success`, mengisi teks dari `getMessage()` exception, lalu `visible/managed = true`. |
| `identifierField` | TextField | Input username (Admin) atau email (Customer). |
| `passwordField` | PasswordField | Input password (disembunyikan). |
| `loginButton` | Button | Tombol "Login". Handler `onLoginClick`. |
| `registerLinkButton` | Button | Tautan ke halaman registrasi Customer. Handler `onRegisterClick`. |
| `backButton` | Button | Kembali ke Landing. Handler `onBackClick`. |

### Handler `onAction`
| Handler | Dipasang pada | Fungsi |
|---|---|---|
| `onLoginClick` | `passwordField`, `loginButton` | Landing: buka Login. Login: validasi bentuk (tidak kosong) → bila identifier mengandung `@` panggil `AuthService.loginCustomer`, selain itu `loginAdmin`; sukses → `SessionManager.login` + buka dashboard sesuai peran; gagal → tampilkan `getMessage()`. |
| `onRegisterClick` | `registerLinkButton` | Landing/Login: buka Register. Register: validasi UX (kosong, password = ulangi) lalu `AuthService.registerCustomer`; sukses → pesan sukses dan arahkan ke Login. |
| `onBackClick` | `backButton` | Kembali ke Landing. |

## 6. `register-view.fxml`
- **Deskripsi:** Registrasi Customer
- **`fx:controller`:** `com.studiomusik.controller.RegisterController`

**Alur.** Validasi UX: field kosong, password = ulangi. Selebihnya (format email, telepon, 8–128 karakter, email unik BR-20) divalidasi `AuthService`.

### fx:id
| fx:id | Tipe | Fungsi |
|---|---|---|
| `rootPane` | ScrollPane | Kontainer akar; dipakai controller untuk memperoleh Scene/Stage saat navigasi. |
| `messageLabel` | Label | Area pesan validasi/error (disembunyikan sampai ada pesan). Controller menambah styleClass `message-error`/`message-success`, mengisi teks dari `getMessage()` exception, lalu `visible/managed = true`. |
| `nameField` | TextField | Input nama lengkap. |
| `phoneField` | TextField | Input nomor telepon. |
| `emailField` | TextField | Input email. |
| `passwordField` | PasswordField | Input password (disembunyikan). |
| `confirmPasswordField` | PasswordField | Ulangi password; dicocokkan di controller (validasi UX saja, tidak dikirim ke Service). |
| `registerButton` | Button | Kirim registrasi → `AuthService.registerCustomer`. Handler `onRegisterClick`. |
| `backToLoginButton` | Button | Kembali ke Login. Handler `onBackToLoginClick`. |

### Handler `onAction`
| Handler | Dipasang pada | Fungsi |
|---|---|---|
| `onRegisterClick` | `confirmPasswordField`, `registerButton` | Landing/Login: buka Register. Register: validasi UX (kosong, password = ulangi) lalu `AuthService.registerCustomer`; sukses → pesan sukses dan arahkan ke Login. |
| `onBackToLoginClick` | `backToLoginButton` | Kembali ke Login. |

## 7. `customer-dashboard-view.fxml`
- **Deskripsi:** Dashboard Customer (Katalog, Booking, Riwayat)
- **`fx:controller`:** `com.studiomusik.controller.CustomerDashboardController`

**Akses.** Hanya bila `SessionManager.isCustomer()`; `customerId` dari `getCurrentCustomer().getId()`.

**Baris tabel.** `roomCatalogTable`: `Room` → `getRoomName`, `getRoomType`, `getPricePerHour`, `getStatus`. `instrumentCatalogTable`: `Instrument` → `getName`, `getType`, `getRentalPrice`, `getStatus`. `selectedInstrumentsTable`: baris lokal (Instrument + qty); subtotal tampilan dari `Instrument.calculateCost(qty)`. `historyTable`: `Booking` → `getBookingId`, `getBookingDate`, `getStartTime`, `getEndTime`, `getRoom().getRoomName()`, `getStatus`, `getTotalCost`. `historyInstrumentsTable`: `BookingInstrument` → `getInstrument().getName()`, `getRentalPrice`, `getQuantity`, `calculateSubtotal`. Uang diformat controller ("Rp 150.000"); `LocalDate`/`LocalTime` diformat lokal.

**ComboBox waktu.** `startTimeCombo`/`endTimeCombo`: `LocalTime` jam bulat (D-GUI-04). Validasi BR-03/04/21 dilakukan Service.

### fx:id
| fx:id | Tipe | Fungsi |
|---|---|---|
| `rootPane` | BorderPane | Kontainer akar; dipakai controller untuk memperoleh Scene/Stage saat navigasi. |
| `welcomeLabel` | Label | Sapaan pengguna aktif (nama dari `SessionManager`). |
| `homeButton` | Button | Kembali ke Landing tanpa logout. Handler `onHomeClick`. |
| `logoutButton` | Button | Logout: `SessionManager.logout()` lalu kembali ke Landing. Handler `onLogoutClick`. |
| `mainTabPane` | TabPane | TabPane utama. |
| `catalogTab` | Tab | Tab "Katalog". |
| `refreshCatalogButton` | Button | Tombol "Muat Ulang". Handler `onRefreshCatalog`. |
| `roomCatalogTable` | TableView | `TableView<Room>` katalog ruangan. |
| `roomNameColumn` | TableColumn | Kolom "Nama". |
| `roomTypeColumn` | TableColumn | Kolom "Tipe". |
| `roomPriceColumn` | TableColumn | Kolom "Harga / Jam". |
| `roomStatusColumn` | TableColumn | Kolom "Status". |
| `instrumentCatalogTable` | TableView | `TableView<Instrument>` katalog instrumen. |
| `instrumentNameColumn` | TableColumn | Kolom "Nama". |
| `instrumentTypeColumn` | TableColumn | Kolom "Tipe". |
| `instrumentPriceColumn` | TableColumn | Kolom "Harga Sewa / Unit". |
| `instrumentStatusColumn` | TableColumn | Kolom "Status". |
| `catalogMessageLabel` | Label | Pesan area Katalog. |
| `bookingTab` | Tab | Tab "Booking". |
| `bookingMessageLabel` | Label | Pesan area Booking. |
| `bookingDatePicker` | DatePicker | Pemilih tanggal booking (tidak dapat diketik; nilai `LocalDate`). |
| `startTimeCombo` | ComboBox | Jam mulai (`ComboBox<LocalTime>`, jam bulat). |
| `endTimeCombo` | ComboBox | Jam selesai (`ComboBox<LocalTime>`, jam bulat). |
| `roomComboBox` | ComboBox | `ComboBox<Room>` dari `RoomService.getBookableRooms()`. |
| `checkAvailabilityButton` | Button | Tombol "Cek Ketersediaan". Handler `onCheckAvailability`. |
| `availabilityLabel` | Label | Hasil cek ketersediaan (tersedia / bertabrakan / error). |
| `instrumentComboBox` | ComboBox | `ComboBox<Instrument>` dari `InstrumentService.getSelectableInstruments()`. |
| `quantitySpinner` | Spinner | `Spinner<Integer>` jumlah unit (1–99; batas UX saja, validasi sebenarnya di Service). |
| `addInstrumentButton` | Button | Tombol "Tambah". Handler `onAddInstrument`. |
| `selectedInstrumentsTable` | TableView | Tabel pilihan instrumen sementara (model baris dibuat controller; belum tersimpan sampai Buat Booking). |
| `selInstrumentNameColumn` | TableColumn | Kolom "Instrumen". |
| `selInstrumentPriceColumn` | TableColumn | Kolom "Harga / Unit". |
| `selInstrumentQtyColumn` | TableColumn | Kolom "Jumlah". |
| `selInstrumentSubtotalColumn` | TableColumn | Kolom "Subtotal". |
| `removeInstrumentButton` | Button | Tombol "Hapus Terpilih". Handler `onRemoveInstrument`. |
| `clearInstrumentsButton` | Button | Tombol "Kosongkan". Handler `onClearInstruments`. |
| `estimatedCostLabel` | Label | Estimasi total dari `BookingService.estimateTotalCost`, diformat rupiah. Tidak ada perhitungan di FXML. |
| `estimateInfoLabel` | Label | Catatan statis bahwa angka final dihitung ulang Service. |
| `estimateButton` | Button | Tombol "Hitung Estimasi". Handler `onEstimateCost`. |
| `resetBookingButton` | Button | Tombol "Reset". Handler `onResetBookingForm`. |
| `createBookingButton` | Button | Tombol "Buat Booking". Handler `onCreateBooking`. |
| `historyTab` | Tab | Tab "Riwayat". |
| `refreshHistoryButton` | Button | Tombol "Muat Ulang". Handler `onRefreshHistory`. |
| `historyMessageLabel` | Label | Pesan area Riwayat. |
| `historyTable` | TableView | `TableView<Booking>` dari `BookingService.getBookingsByCustomer(id)` (tanpa detail alat). |
| `historyIdColumn` | TableColumn | Kolom "No.". |
| `historyDateColumn` | TableColumn | Kolom "Tanggal". |
| `historyStartColumn` | TableColumn | Kolom "Mulai". |
| `historyEndColumn` | TableColumn | Kolom "Selesai". |
| `historyRoomColumn` | TableColumn | Kolom "Ruangan". |
| `historyStatusColumn` | TableColumn | Kolom "Status". |
| `historyTotalColumn` | TableColumn | Kolom "Total Biaya". |
| `historyDetailTitleLabel` | Label | Judul panel detail booking terpilih. |
| `historyDetailInfoLabel` | Label | Ringkasan booking terpilih / petunjuk bila belum ada pilihan. |
| `historyInstrumentsTable` | TableView | Detail alat dari `BookingService.getBookingForCustomer(bookingId, customerId)`. |
| `histInstrumentNameColumn` | TableColumn | Kolom "Instrumen". |
| `histInstrumentPriceColumn` | TableColumn | Kolom "Harga / Unit". |
| `histInstrumentQtyColumn` | TableColumn | Kolom "Jumlah". |
| `histInstrumentSubtotalColumn` | TableColumn | Kolom "Subtotal". |

### Handler `onAction`
| Handler | Dipasang pada | Fungsi |
|---|---|---|
| `onHomeClick` | `homeButton` | Kembali ke Landing. |
| `onLogoutClick` | `logoutButton` | `SessionManager.logout()`, kembali ke Landing. |
| `onRefreshCatalog` | `refreshCatalogButton` | Muat ulang `RoomService.getAllRooms()` dan `InstrumentService.getAllInstruments()`. |
| `onCheckAvailability` | `checkAvailabilityButton` | `BookingService.isRoomAvailable(roomId, date, start, end)`; tampilkan hasil di `availabilityLabel`. |
| `onAddInstrument` | `addInstrumentButton` | Tambah pilihan (instrumen, jumlah) ke daftar sementara; instrumen sama → digabung/ditolak di controller (UX). |
| `onRemoveInstrument` | `removeInstrumentButton` | Hapus baris terpilih dari daftar sementara. |
| `onClearInstruments` | `clearInstrumentsButton` | Kosongkan daftar sementara. |
| `onEstimateCost` | `estimateButton` | `BookingService.estimateTotalCost(roomId, start, end, Map<instrumentId, qty>)` → `estimatedCostLabel`. |
| `onResetBookingForm` | `resetBookingButton` | Kosongkan seluruh form Booking. |
| `onCreateBooking` | `createBookingButton` | `BookingService.createBooking(customerId dari SessionManager, ...)`; sukses → pesan + reset form + muat ulang Riwayat; gagal → `getMessage()`. |
| `onRefreshHistory` | `refreshHistoryButton` | `BookingService.getBookingsByCustomer(customerId)`. |

## 8. `admin-dashboard-view.fxml`
- **Deskripsi:** Dashboard Admin (Ringkasan, Booking, Customer, Ruangan, Instrumen)
- **`fx:controller`:** `com.studiomusik.controller.AdminDashboardController`

**Akses.** Hanya bila `SessionManager.isAdmin()`.

**Baris tabel.** `bookingTable`: `Booking` → `getBookingId`, `getCustomer().getName()`, `getRoom().getRoomName()`, `getBookingDate`, `getStartTime`, `getEndTime`, `getStatus`, `getTotalCost`. `detailInstrumentTable`: `BookingInstrument` (idem Customer). `customerTable`: `Customer` → `getId`, `getName`, `getPhone`, `getEmail` (tanpa hash). `roomTable`: `Room` → `getRoomId`, `getRoomName`, `getRoomType`, `getPricePerHour`, `getStatus`. `instrumentTable`: `Instrument` → `getInstrumentId`, `getName`, `getType`, `getRentalPrice`, `getStatus`.

**Form CRUD inline.** Pilih baris → form terisi, judul "Ubah …", Simpan memanggil `update*`; tanpa pilihan → `add*`. `onNew*` mengosongkan pilihan. `getBookingDetail(id)` dipanggil saat baris Booking dipilih (karena `getAllBookings` tanpa detail alat).

**BR-19.** Tombol detail alat dinonaktifkan controller untuk booking COMPLETED/CANCELLED (UX); penegakan tetap di Service.

### fx:id
| fx:id | Tipe | Fungsi |
|---|---|---|
| `rootPane` | BorderPane | Kontainer akar; dipakai controller untuk memperoleh Scene/Stage saat navigasi. |
| `welcomeLabel` | Label | Sapaan pengguna aktif (nama dari `SessionManager`). |
| `logoutButton` | Button | Logout: `SessionManager.logout()` lalu kembali ke Landing. Handler `onLogoutClick`. |
| `mainTabPane` | TabPane | TabPane utama. |
| `summaryTab` | Tab | Tab "Ringkasan". |
| `refreshSummaryButton` | Button | Tombol "Muat Ulang". Handler `onRefreshSummary`. |
| `summaryMessageLabel` | Label | Pesan area Ringkasan. |
| `pendingCountLabel` | Label | Jumlah booking PENDING (`getBookingStatusSummary`). |
| `confirmedCountLabel` | Label | Jumlah CONFIRMED. |
| `completedCountLabel` | Label | Jumlah COMPLETED. |
| `cancelledCountLabel` | Label | Jumlah CANCELLED. |
| `customerCountLabel` | Label | `CustomerService.countCustomers()`. |
| `roomCountLabel` | Label | `RoomService.countRooms()`. |
| `instrumentCountLabel` | Label | `InstrumentService.countInstruments()`. |
| `bookingTab` | Tab | Tab "Booking". |
| `bookingStatusFilterCombo` | ComboBox | Filter status tabel booking (opsi "Semua" + 4 status); penyaringan di controller. |
| `refreshBookingsButton` | Button | Tombol "Muat Ulang". Handler `onRefreshBookings`. |
| `bookingMessageLabel` | Label | Pesan area Booking. |
| `bookingTable` | TableView | `TableView<Booking>` dari `BookingService.getAllBookings()`. |
| `bookingIdColumn` | TableColumn | Kolom "No.". |
| `bookingCustomerColumn` | TableColumn | Kolom "Customer". |
| `bookingRoomColumn` | TableColumn | Kolom "Ruangan". |
| `bookingDateColumn` | TableColumn | Kolom "Tanggal". |
| `bookingStartColumn` | TableColumn | Kolom "Mulai". |
| `bookingEndColumn` | TableColumn | Kolom "Selesai". |
| `bookingStatusColumn` | TableColumn | Kolom "Status". |
| `bookingTotalColumn` | TableColumn | Kolom "Total Biaya". |
| `bookingDetailTitleLabel` | Label | Judul panel booking terpilih. |
| `bookingDetailInfoLabel` | Label | Ringkasan booking terpilih (customer, ruangan, jadwal, total). |
| `newStatusCombo` | ComboBox | Pilihan status tujuan; controller mengisi hanya status yang lolos `BookingStatus.canTransitionTo`. |
| `updateStatusButton` | Button | Tombol "Simpan Status". Handler `onUpdateBookingStatus`. |
| `detailInstrumentTable` | TableView | `TableView<BookingInstrument>` dari `BookingService.getBookingInstruments(bookingId)`. |
| `detailInstrumentNameColumn` | TableColumn | Kolom "Instrumen". |
| `detailPriceColumn` | TableColumn | Kolom "Harga / Unit". |
| `detailQuantityColumn` | TableColumn | Kolom "Jumlah". |
| `detailSubtotalColumn` | TableColumn | Kolom "Subtotal". |
| `detailInstrumentCombo` | ComboBox | Instrumen yang akan ditambahkan ke booking terpilih. |
| `detailQuantitySpinner` | Spinner | Jumlah untuk tambah/ubah detail alat. |
| `addDetailButton` | Button | Tombol "Tambah Instrumen". Handler `onAddDetail`. |
| `updateDetailButton` | Button | Tombol "Ubah Jumlah Terpilih". Handler `onUpdateDetailQuantity`. |
| `removeDetailButton` | Button | Tombol "Hapus Terpilih". Handler `onRemoveDetail`. |
| `detailMessageLabel` | Label | Pesan area detail alat (BR-17/18/19 dari Service). |
| `customerTab` | Tab | Tab "Customer". |
| `refreshCustomerButton` | Button | Tombol "Muat Ulang". Handler `onRefreshCustomer`. |
| `customerMessageLabel` | Label | Pesan area Customer. |
| `customerTable` | TableView | `TableView<Customer>` dari `CustomerService.getAllCustomers()`. |
| `customerIdColumn` | TableColumn | Kolom "ID". |
| `customerNameColumn` | TableColumn | Kolom "Nama". |
| `customerPhoneColumn` | TableColumn | Kolom "Telepon". |
| `customerEmailColumn` | TableColumn | Kolom "Email". |
| `customerFormTitleLabel` | Label | Judul form: "Tambah Customer" / "Ubah Customer" sesuai ada-tidaknya pilihan baris. |
| `customerNameField` | TextField | Nama Customer. |
| `customerPhoneField` | TextField | Telepon Customer. |
| `customerEmailField` | TextField | Email Customer. |
| `customerPasswordField` | PasswordField | Password awal; hanya aktif mode tambah (`updateCustomer` tidak mengubah password). |
| `saveCustomerButton` | Button | Tombol "Simpan". Handler `onSaveCustomer`. |
| `newCustomerButton` | Button | Tombol "Baru". Handler `onNewCustomer`. |
| `deleteCustomerButton` | Button | Tombol "Hapus Terpilih". Handler `onDeleteCustomer`. |
| `roomTab` | Tab | Tab "Ruangan". |
| `refreshRoomButton` | Button | Tombol "Muat Ulang". Handler `onRefreshRoom`. |
| `roomMessageLabel` | Label | Pesan area Ruangan. |
| `roomTable` | TableView | `TableView<Room>` dari `RoomService.getAllRooms()`. |
| `roomIdColumn` | TableColumn | Kolom "ID". |
| `roomNameColumn` | TableColumn | Kolom "Nama". |
| `roomTypeColumn` | TableColumn | Kolom "Tipe". |
| `roomPriceColumn` | TableColumn | Kolom "Harga / Jam". |
| `roomStatusColumn` | TableColumn | Kolom "Status". |
| `roomFormTitleLabel` | Label | Judul form Ruangan (tambah/ubah). |
| `roomNameField` | TextField | Nama ruangan. |
| `roomTypeField` | TextField | Tipe ruangan. |
| `roomPriceField` | TextField | Harga per jam (teks → `BigDecimal` di controller; format salah → pesan UX). |
| `roomStatusCombo` | ComboBox | `ComboBox<RoomStatus>`. |
| `saveRoomButton` | Button | Tombol "Simpan". Handler `onSaveRoom`. |
| `newRoomButton` | Button | Tombol "Baru". Handler `onNewRoom`. |
| `deleteRoomButton` | Button | Tombol "Hapus Terpilih". Handler `onDeleteRoom`. |
| `instrumentTab` | Tab | Tab "Instrumen". |
| `refreshInstrumentButton` | Button | Tombol "Muat Ulang". Handler `onRefreshInstrument`. |
| `instrumentMessageLabel` | Label | Pesan area Instrumen. |
| `instrumentTable` | TableView | `TableView<Instrument>` dari `InstrumentService.getAllInstruments()`. |
| `instrumentIdColumn` | TableColumn | Kolom "ID". |
| `instrumentNameColumn` | TableColumn | Kolom "Nama". |
| `instrumentTypeColumn` | TableColumn | Kolom "Tipe". |
| `instrumentPriceColumn` | TableColumn | Kolom "Harga Sewa / Unit". |
| `instrumentStatusColumn` | TableColumn | Kolom "Status". |
| `instrumentFormTitleLabel` | Label | Judul form Instrumen (tambah/ubah). |
| `instrumentNameField` | TextField | Nama instrumen. |
| `instrumentTypeField` | TextField | Tipe instrumen. |
| `instrumentPriceField` | TextField | Harga sewa per unit (teks → `BigDecimal`). |
| `instrumentStatusCombo` | ComboBox | `ComboBox<InstrumentStatus>`. |
| `saveInstrumentButton` | Button | Tombol "Simpan". Handler `onSaveInstrument`. |
| `newInstrumentButton` | Button | Tombol "Baru". Handler `onNewInstrument`. |
| `deleteInstrumentButton` | Button | Tombol "Hapus Terpilih". Handler `onDeleteInstrument`. |

### Handler `onAction`
| Handler | Dipasang pada | Fungsi |
|---|---|---|
| `onLogoutClick` | `logoutButton` | `SessionManager.logout()`, kembali ke Landing. |
| `onRefreshSummary` | `refreshSummaryButton` | `getBookingStatusSummary` + tiga `count*`. |
| `onRefreshBookings` | `refreshBookingsButton` | `BookingService.getAllBookings()` + filter status. |
| `onUpdateBookingStatus` | `updateStatusButton` | `BookingService.updateBookingStatus(bookingId, newStatus)` lalu muat ulang. |
| `onAddDetail` | `addDetailButton` | `addInstrumentToBooking(bookingId, instrumentId, qty)`. |
| `onUpdateDetailQuantity` | `updateDetailButton` | `updateBookingInstrumentQuantity(bookingInstrumentId, qty)`. |
| `onRemoveDetail` | `removeDetailButton` | `removeInstrumentFromBooking(bookingInstrumentId)` (konfirmasi dulu). |
| `onRefreshCustomer` | `refreshCustomerButton` | `CustomerService.getAllCustomers()`. |
| `onSaveCustomer` | `saveCustomerButton` | Tanpa pilihan baris → `addCustomer`; ada pilihan → `updateCustomer`. |
| `onNewCustomer` | `newCustomerButton` | Kosongkan form & pilihan baris. |
| `onDeleteCustomer` | `deleteCustomerButton` | `deleteCustomer` (konfirmasi; BR-22 ditampilkan dari exception). |
| `onRefreshRoom` | `refreshRoomButton` | `RoomService.getAllRooms()`. |
| `onSaveRoom` | `saveRoomButton` | `addRoom` / `updateRoom`. |
| `onNewRoom` | `newRoomButton` | Kosongkan form. |
| `onDeleteRoom` | `deleteRoomButton` | `deleteRoom` (konfirmasi). |
| `onRefreshInstrument` | `refreshInstrumentButton` | `InstrumentService.getAllInstruments()`. |
| `onSaveInstrument` | `saveInstrumentButton` | `addInstrument` / `updateInstrument`. |
| `onNewInstrument` | `newInstrumentButton` | Kosongkan form. |
| `onDeleteInstrument` | `deleteInstrumentButton` | `deleteInstrument` (konfirmasi). |

## 9. Kelas CSS (`app.css`)
| Kelompok | Kelas |
|---|---|
| Halaman | `page`, `alt-section`, `surface-section`, `auth-backdrop`, `scroll-transparent` |
| Tipografi | `hero-eyebrow`, `hero-heading`, `hero-text`, `section-heading`, `section-subtitle`, `page-title`, `card-heading`, `card-text`, `muted-text`, `caption`, `form-label` |
| Logo | `logo-sono`, `logo-ra`, `logo-tagline` (+ `on-light`) |
| Navbar | `navbar`, `navbar-link`, `navbar-user` |
| Hero | `hero-bg`, `hero-glow`, `booking-panel` |
| Tombol | `primary-button` (+ `cta-large`), `secondary-button`, `outline-button` (+ `on-dark`), `danger-button`, `link-button`, `compact-button` |
| Kartu | `card`, `card-flat`, `auth-card`, `summary-card` (+ `pending`/`confirmed`/`completed`/`cancelled`), `summary-value`, `summary-label`, `cost-box`, `cost-value`, `step-number`, `filter-bar`, `toolbar`, `footer`, `footer-text`, `footer-muted` |
| Pesan | `message-area` + `message-error`/`message-success`/`message-info`/`message-warning`, `field-error` |
| Lencana status | `badge` + `status-pending`/`status-confirmed`/`status-completed`/`status-cancelled`/`status-available`/`status-maintenance` |
| Tabel | `numeric` (kolom rata kanan); gaya `table-view`/`table-row-cell` bersifat global |
| Starter (STEP 1) | `root-pane`, `app-title`, `app-subtitle`, `starter-info`, `status-label`, `status-ok` — untuk `main-view.fxml` (warna disesuaikan palet SONORA; tombolnya memakai `primary-button`) |

## 10. Pekerjaan STEP 7 yang bergantung pada kontrak ini
1. Membuat 5 controller (nama & `fx:id` di atas). `SessionManager`, `DateTimeUtil`, `CurrencyUtil` belum ada di ZIP (handoff §5): buat di STEP 7 sesuai `API_CONTRACTS` §2 atau pakai pemformat lokal tanpa mengubah kontrak.
2. Navigasi (`MainApp`) memuat FXML + `app.css`, mengatur ukuran jendela, dan memuat font bila file tersedia.
3. Tangkap `ValidationException`/`BusinessRuleException` → `getMessage()`; `ServiceException` biasa → pesan umum (AC-G04).
4. Guard peran di tiap controller dashboard (AC-G03).
5. Isi data dinamis (kartu studio/fasilitas, ComboBox, tabel) dan lencana status.
