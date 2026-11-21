# ACCEPTANCE CRITERIA

> Status: **TERKUNCI (STEP 0)**. Setiap kriteria dinilai **Lulus** hanya jika ada bukti nyata (hasil `mvn test`, query, screenshot, atau catatan uji manual). Jangan menandai Lulus tanpa bukti.
> Kolom *Tahap* = tahap yang pertama kali dapat memverifikasi kriteria.

## A. Build dan Setup (STEP 1)
| ID | Kriteria | Bukti | Tahap |
|---|---|---|---|
| AC-B01 | `java -version` dan `javac -version` menunjukkan Java 21 | Output perintah | 1 |
| AC-B02 | `mvn clean` berhasil | Output Maven | 1 |
| AC-B03 | Dependency (JavaFX 21.0.5, Connector/J 8.4.0, JUnit 5.10.2) terunduh | Output Maven | 1 |
| AC-B04 | `mvn test` lulus (`AppInfoTest`) | Output surefire | 1 |
| AC-B05 | `mvn package` menghasilkan `target/studio-musik-app-1.0.0-SNAPSHOT.jar` | Output Maven | 1 |
| AC-B06 | `mvn clean javafx:run` membuka jendela starter | Screenshot | 1 |
| AC-B07 | FXML dimuat, CSS diterapkan (latar gelap, tombol ungu), tombol "Uji Interaksi" mengubah label status | Screenshot | 1 |
| AC-B08 | Tidak ada `module-info.java`; tidak ada credential di repo; `config/db.properties` di-ignore Git | Pemeriksaan file | 1 |

## B. Database (STEP 2)
| ID | Kriteria | Bukti |
|---|---|---|
| AC-D01 | `schema.sql` membuat 6 tabel sesuai `DATABASE_CONTRACT.md` tanpa error di MySQL 8.0+ | Output MySQL |
| AC-D02 | FK, UNIQUE, CHECK, indeks sesuai kontrak | `SHOW CREATE TABLE` |
| AC-D03 | `seed.sql` berisi data demo, password berupa hash | Isi file + query |
| AC-D04 | `reset.sql` mengembalikan database ke kondisi awal | Output MySQL |
| AC-D05 | Insert `booking_instrument` duplikat (booking_id, instrument_id) ditolak DB | Output error |

## C. Model dan OOP (STEP 3)
| ID | Kriteria | Bukti |
|---|---|---|
| AC-M01 | `Person` abstract; `new Person(...)` tidak dapat dikompilasi | Kompilasi/Unit test |
| AC-M02 | `Customer.getRole()` = "Customer", `Admin.getRole()` = "Admin" lewat referensi `Person` | Unit test |
| AC-M03 | `Booking.calculateDuration()` dan `calculateTotalCost()` benar (contoh: room Rp100.000/jam × 2 jam + 2 alat × Rp25.000 = Rp250.000) | Unit test |
| AC-M04 | Semua uang `BigDecimal`, pembulatan `HALF_UP` skala 2 | Unit test |
| AC-M05 | Tujuh konsep PBO dapat ditunjuk ke class/method nyata | Tabel di laporan |

### Status AC-M (STEP 3, 2026-11-19)
Bukti: 79 tes (74 baru + 5 lama) lulus lewat `javac` + JUnit console di sandbox; **`mvn clean test` belum dijalankan** (Maven tidak dapat mengunduh dependency di sandbox) dan menunggu konfirmasi pengguna secara lokal. Rincian: `docs/STAGE3_VERIFICATION.md`.

| ID | Status | Bukti |
|---|---|---|
| AC-M01 | **Lulus (sandbox)** | `javac` menolak `new Person(...)`: *"Person is abstract; cannot be instantiated"*. Tes: `PersonHierarchyTest.personIsAbstractAndDeclaresAbstractGetRole`, `personCannotBeInstantiatedEvenViaReflection` |
| AC-M02 | **Lulus (sandbox)** | `PersonHierarchyTest.getRoleIsPolymorphicThroughPersonReference`, `customerAndAdminOverrideGetRoleThemselves`; `OopConceptsTest.polymorphism_*` |
| AC-M03 | **Lulus (sandbox)** | `BookingTest.exampleFromContractRoom100kTwoHoursPlusTwoInstrumentsAt25kIs250k` (= `250000.00`), `sameTotalWhenTwoDifferentInstrumentsAreUsedOnceEach`, `reproducesSeedTotalsFromDatabaseContract` (400000, 470000, 496000 sama dengan seed) |
| AC-M04 | **Lulus (sandbox)** | `RoomTest.costUsesHalfUpRoundingOnExactDecimals`, `InstrumentTest.costUsesHalfUpRounding`, `BookingInstrumentTest.subtotalIsBigDecimalWithScaleTwoAndHalfUp`, `BookingTest.totalIsBigDecimalWithScaleTwo`, `totalRoundsEachComponentHalfUpThenSums`; tipe kembalian `BigDecimal` diperiksa lewat refleksi; tidak ada `double`/`float` di package model |
| AC-M05 | **Lulus (sandbox)** | Tabel di bawah; setiap konsep dibuktikan oleh tes perilaku di `OopConceptsTest` |

### Pemetaan tujuh konsep PBO ke kode nyata (AC-M05)
| Konsep | Class / method nyata | Bukti perilaku (tes) |
|---|---|---|
| Encapsulation | Semua field instance di `Person`, `Customer`, `Admin`, `Room`, `Instrument`, `BookingInstrument`, `Booking` bersifat `private`. Invariant dijaga setter/konstruktor (mis. `BookingInstrument.setQuantity` menolak < 1); `Booking.getInstruments()` hanya tampilan baca-saja | `OopConceptsTest.encapsulation_everyInstanceFieldOfEveryModelClassIsPrivate`, `encapsulation_stateIsReachedOnlyThroughMethodsThatEnforceInvariants` |
| Abstraction | `abstract class Person` dengan `public abstract String getRole()` | `OopConceptsTest.abstraction_*`; `javac` menolak `new Person(...)` |
| Inheritance | `Customer extends Person`, `Admin extends Person`; `getName()` dll. diwarisi (dideklarasikan di `Person`) | `OopConceptsTest.inheritance_*` |
| Polymorphism | `Customer.getRole()` → "Customer", `Admin.getRole()` → "Admin", dipanggil lewat referensi `Person` (override sungguhan: dideklarasikan di subclass) | `OopConceptsTest.polymorphism_*`, `PersonHierarchyTest.getRoleIsPolymorphicThroughPersonReference` |
| Association | `Booking` → `Customer` dan `Room` (`getCustomer()`, `getRoom()`; 1 Customer : N Booking); `BookingInstrument` → `Instrument` (many-to-many lewat `Booking.addInstrument`) | `OopConceptsTest.association_*` |
| Composition | `Booking` membuat dan memiliki `BookingInstrument` (`addInstrument`), daftar internalnya tidak dapat dimodifikasi dari luar, `setBookingId` menyebar ke detail, total induk dihitung dari bagian-bagiannya | `OopConceptsTest.composition_*`, `BookingTest.setBookingIdPropagatesToExistingAndFutureDetails`, `getInstrumentsIsAnUnmodifiableView` |
| Enumeration | `BookingStatus` (dengan `blocksSchedule()` dan `canTransitionTo()`), `RoomStatus`, `InstrumentStatus`; field status bertipe enum | `OopConceptsTest.enumeration_*`, `BookingStatusTest` (16 kombinasi transisi) |

## D. Aturan Bisnis (STEP 5) — setiap BR minimal satu tes
| ID | Kriteria | BR |
|---|---|---|
| AC-R01 | `endTime <= startTime` ditolak | BR-03 |
| AC-R02 | Waktu bukan menit 00 ditolak | BR-04 |
| AC-R03 | Room `MAINTENANCE` tidak dapat dipesan | BR-05 |
| AC-R04 | Instrument `MAINTENANCE` tidak dapat dipilih | BR-06 |
| AC-R05 | Booking `PENDING`/`CONFIRMED` yang beririsan ditolak | BR-07/08 |
| AC-R06 | Booking `CANCELLED`/`COMPLETED` tidak memblokir jadwal | BR-07 |
| AC-R07 | Booking berurutan (12:00 akhir, 12:00 mulai) diterima | BR-08 |
| AC-R08 | Overlap sebagian, mengandung, dan identik semuanya ditolak | BR-08 |
| AC-R09 | Biaya ruangan = harga/jam × durasi; biaya alat = quantity × harga | BR-09/10/11 |
| AC-R10 | Perubahan harga alat setelah booking tidak mengubah `booking_instrument.rental_price` lama | BR-12 |
| AC-R11 | Booking baru berstatus `PENDING` | BR-13 |
| AC-R12 | Transisi tidak valid (mis. `COMPLETED→PENDING`) ditolak; transisi valid diterima | BR-14 |
| AC-R13 | Kegagalan menyimpan satu detail alat membatalkan seluruh booking (rollback), tidak ada booking yatim | BR-15 |
| AC-R14 | Customer A tidak dapat membaca booking Customer B | BR-16 |
| AC-R15 | Menambah alat yang sama dua kali pada booking ditolak; `quantity < 1` ditolak | BR-17/18 |
| AC-R16 | Edit detail alat pada booking final ditolak; total biaya dihitung ulang setelah edit | BR-19 |
| AC-R17 | Password tersimpan sebagai hash; email duplikat ditolak | BR-20 |
| AC-R18 | Tanggal lampau ditolak | BR-21 |
| AC-R19 | Hapus Room/Instrument/Customer yang dipakai booking ditolak dengan pesan jelas | BR-22 |

## E. CRUD Many-to-Many (STEP 4–8)
| ID | Kriteria | Bukti |
|---|---|---|
| AC-C01 | **Create**: menambah alat ke booking menghasilkan baris `booking_instrument` baru | Query + screenshot |
| AC-C02 | **Read**: tampilan menampilkan booking beserta semua alatnya lewat JOIN tiga tabel | Query + screenshot |
| AC-C03 | **Update**: mengubah quantity memperbarui baris dan `total_cost` | Query + screenshot |
| AC-C04 | **Delete**: menghapus alat dari booking menghapus baris dan memperbarui `total_cost` | Query + screenshot |

## F. Arsitektur dan Kualitas
| ID | Kriteria | Bukti |
|---|---|---|
| AC-A01 | Tidak ada `java.sql.*` / SQL di package `controller` | `grep` |
| AC-A02 | Tidak ada `javafx.*` di `model`, `dao`, `service` | `grep` |
| AC-A03 | Semua SQL memakai `PreparedStatement` dan try-with-resources | Review kode |
| AC-A04 | Tidak ada credential/password plaintext di repo | Review + `.gitignore` |
| AC-A05 | `mvn test` lulus pada akhir setiap tahap | Output |

## G. Fungsional GUI (STEP 6–8)
| ID | Kriteria |
|---|---|
| AC-G01 | Customer dapat register, login, melihat ruangan/alat, cek ketersediaan, melihat estimasi biaya, membuat booking, dan melihat riwayat miliknya |
| AC-G02 | Admin dapat login, melihat dashboard, CRUD Customer/Room/Instrument, melihat semua booking, mengubah status, dan mengelola detail alat |
| AC-G03 | Peran menentukan layar yang dapat diakses (Customer tidak dapat membuka layar Admin) |
| AC-G04 | Pesan error tampil ramah (bukan stack trace) |

## H. Deliverable Akhir (STEP 9)
Laporan akhir, screenshot aplikasi, source code ZIP, README, dokumentasi hasil uji, peer review anggota kelompok.
