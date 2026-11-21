# ARCHITECTURE CONTRACT

> Status: **TERKUNCI (STEP 0)**.

## 1. Alur lapisan
```
View (FXML + CSS) → Controller → Service → DAO → MySQL
                         ↑ Model (entitas)   ↑ Util (fungsi umum)
```

| Layer | Package / lokasi | Tanggung jawab | Dilarang |
|---|---|---|---|
| View | `src/main/resources/com/studiomusik/view/*.fxml`, `.../css/app.css` | Tata letak, binding `fx:id`/`onAction` | Logika bisnis |
| Controller | `com.studiomusik.controller` | Ambil input UI, panggil Service, tampilkan hasil/pesan error, navigasi | **SQL**, aturan bisnis, `Connection` |
| Service | `com.studiomusik.service` | Validasi input, aturan bisnis (BR-xx), mengatur transaksi, otorisasi kepemilikan data | Import `javafx.*`, SQL mentah |
| DAO | `com.studiomusik.dao` | Satu-satunya tempat SQL; `PreparedStatement`; try-with-resources; mapping `ResultSet` ↔ Model | Aturan bisnis, `javafx.*` |
| Model | `com.studiomusik.model`, `model.enums` | Entitas, state, perhitungan murni | SQL, `javafx.*`, akses DB |
| Util | `com.studiomusik.util` | Koneksi DB, sesi, format tanggal/mata uang, hash password | Aturan bisnis |

## 2. Struktur proyek (kontrak)
```
studio-musik/
├── pom.xml  README.md  PROGRESS.md  PROGRESS_HANDOFF.md  .gitignore
├── .vscode/extensions.json
├── config/db.properties.example            (db.properties lokal, di-ignore Git)
├── database/                               (STEP 2)
│   ├── schema.sql  seed.sql  reset.sql
│   ├── tools/Pbkdf2HashGenerator.java      (utilitas mandiri, JDK saja)
│   └── tests/verify_stage2.sql             (verifikasi otomatis struktur/constraint/seed/overlap)
├── docs/
└── src/
    ├── main/java/com/studiomusik/
    │   ├── MainApp.java  AppInfo.java
    │   ├── controller/  model/  model/enums/  dao/  service/  util/
    └── main/resources/com/studiomusik/
        ├── view/*.fxml   css/app.css
    └── test/java/com/studiomusik/
```

## 3. Daftar class sasaran per package (kontrak penamaan)
- `model`: `Person` (abstract), `Admin`, `Customer`, `Room`, `Instrument`, `Booking`, `BookingInstrument`.
- `model.enums`: `BookingStatus`, `RoomStatus`, `InstrumentStatus`.
- `dao`: `AdminDAO`, `CustomerDAO`, `RoomDAO`, `InstrumentDAO`, `BookingDAO`, `BookingInstrumentDAO`.
- `service`: `AuthService`, `CustomerService`, `RoomService`, `InstrumentService`, `BookingService`, `ServiceException`, `ValidationException`, `BusinessRuleException`.
- `controller` (STEP 7): `LoginController`, `RegisterController`, `CustomerDashboardController`, `BookingController`, `MyBookingController`, `AdminDashboardController`, `CustomerManagementController`, `RoomController`, `InstrumentController`, `BookingManagementController` (+ `MainController` starter STEP 1).
- `util`: `DatabaseConnection`, `SessionManager`, `DateTimeUtil`, `CurrencyUtil`, `PasswordUtil`.

Nama file boleh disesuaikan hanya dengan alasan teknis yang dicatat di `DECISION_LOG.md`; pembagian tanggung jawab tidak boleh diubah.

## 4. Aturan implementasi
1. Java 21; JavaFX + FXML; Maven; non-modular (tanpa `module-info.java`).
2. JDBC dengan `PreparedStatement` (tanpa penyambungan string SQL dari input), try-with-resources untuk `Connection`/`Statement`/`ResultSet`.
3. Transaksi untuk Booking + detail alat (BR-15, D-019).
4. `BigDecimal` untuk uang (D-006).
5. Validasi input di Service sebelum menyimpan; Controller boleh melakukan validasi bentuk dasar (kosong/format) hanya untuk UX.
6. Password di-hash (D-018); jangan log password atau hash.
7. Konfigurasi DB dibaca dari `config/db.properties` (relatif terhadap working directory = root proyek); tidak ada credential di source/repo.
8. Penanganan error: DAO `SQLException` → Service `ServiceException` (D-017) → Controller menampilkan pesan.
9. Otorisasi: Service memeriksa kepemilikan data Customer (BR-16); Controller memakai `SessionManager` hanya untuk mengetahui pengguna aktif dan peran.
10. Setiap modul harus dapat dikompilasi dan diuji sebelum integrasi; jangan mengklaim selesai tanpa bukti.

## 5. Pemetaan Class Diagram TK I → kode
Lihat `DECISION_LOG.md` D-005, D-006, D-013, D-014.

## 6. Dependency terkunci (STEP 1)
| Komponen | Versi |
|---|---|
| Java (release) | 21 |
| JavaFX (`javafx-controls`, `javafx-fxml`) | 21.0.5 |
| MySQL Connector/J (`com.mysql:mysql-connector-j`, runtime) | 8.4.0 |
| JUnit Jupiter (`junit-jupiter`, test) | 5.10.2 |
| maven-compiler-plugin | 3.13.0 |
| maven-surefire-plugin | 3.2.5 |
| javafx-maven-plugin | 0.0.8 |
Main class: `com.studiomusik.MainApp`.
