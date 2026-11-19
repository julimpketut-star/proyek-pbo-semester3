# Sistem Pengelolaan Operasional Studio Musik

Aplikasi **desktop** (bukan website) untuk mengelola operasional satu studio musik: pelanggan, ruangan, alat musik, dan pemesanan (booking). Dibuat untuk **Tugas Kelompok II — Pemrograman Berorientasi Objek**, Program Studi Informatika, Universitas Udayana.

**Teknologi:** Java 21 · JavaFX 21 (FXML) · MySQL 8.0+ · Maven · JDBC · JUnit 5
**Arsitektur:** View → Controller → Service → DAO → MySQL

## Status
| Tahap | Isi | Status |
|---|---|---|
| STEP 0 | Spesifikasi final dan kontrak (`docs/`) | Selesai |
| STEP 1 | Scaffold Maven + starter JavaFX | File dibuat; verifikasi penuh via Maven **menunggu dijalankan lokal** (lihat `docs/STAGE1_VERIFICATION.md`) |
| STEP 2+ | Database, model, DAO, service, GUI | Belum dikerjakan |

Fitur bisnis (login, booking, CRUD) **belum ada**. Aplikasi saat ini hanya menampilkan jendela starter.

## Prasyarat
1. **JDK 21** (mis. Temurin 21). Cek: `java -version` dan `javac -version` harus menunjukkan 21.
2. **Maven 3.9+** (`mvn -version`; Maven harus memakai JDK 21).
3. Koneksi internet untuk unduhan dependency pertama kali.
4. **MySQL 8.0.16+** (dibutuhkan mulai STEP 2; belum diperlukan untuk starter).
5. (Opsional) **Scene Builder** untuk mengedit FXML.

### Konfigurasi JDK 21
- Windows: set `JAVA_HOME` ke folder JDK 21 dan tambahkan `%JAVA_HOME%\bin` ke `PATH`.
- macOS/Linux: `export JAVA_HOME=/path/ke/jdk-21` dan `export PATH="$JAVA_HOME/bin:$PATH"`.
- Pastikan `mvn -version` menampilkan `Java version: 21.x`.

## Membuka di Visual Studio Code
1. Pasang ekstensi yang direkomendasikan (VS Code akan menawarkan dari `.vscode/extensions.json`): *Extension Pack for Java*, *Maven for Java*, *XML*.
2. `File → Open Folder…` → pilih folder `studio-musik/` (yang berisi `pom.xml`).
3. Tunggu proyek Java/Maven selesai diimpor. Pastikan JDK proyek = 21 (`Java: Configure Java Runtime`).

## Struktur proyek
```
studio-musik/
├── pom.xml
├── README.md  PROGRESS.md  PROGRESS_HANDOFF.md
├── config/db.properties.example      ← salin ke config/db.properties (jangan di-commit)
├── database/                         ← schema.sql, seed.sql, reset.sql (STEP 2)
├── docs/                             ← spesifikasi, kontrak, keputusan, acceptance criteria
└── src/
    ├── main/java/com/studiomusik/
    │   ├── MainApp.java, AppInfo.java
    │   ├── controller/  model/  model/enums/  dao/  service/  util/
    ├── main/resources/com/studiomusik/{view,css}/
    └── test/java/com/studiomusik/
```

## Build, test, dan menjalankan
Jalankan dari folder `studio-musik/`:
```bash
mvn clean            # bersihkan target/
mvn test             # jalankan unit test (JUnit 5)
mvn package          # build JAR di target/
mvn clean javafx:run # jalankan aplikasi JavaFX
```
Catatan: jalankan aplikasi dengan `mvn javafx:run` (JAR hasil `package` adalah JAR tipis dan tidak memuat runtime JavaFX).

## Konfigurasi database (mulai STEP 2)
```bash
cp config/db.properties.example config/db.properties   # lalu isi username/password lokal Anda
```
`config/db.properties` diabaikan Git. **Jangan** menaruh credential asli di source code atau repository.

## Dokumen
Mulai dari `docs/FINAL_SPECIFICATION.md`, lalu `docs/DECISION_LOG.md`. Kontrak teknis: `docs/DATABASE_CONTRACT.md`, `docs/ARCHITECTURE_CONTRACT.md`, `docs/API_CONTRACTS.md`. Kriteria penerimaan: `docs/ACCEPTANCE_CRITERIA.md`.

## Tim — Kelompok 1, Kelas D
I Gede Gaura Krishna · I Dewa Gede Putra Sugiatnyana · I Made Ari Andika Putra · Jire Immanuel Nababan · Ketut Juli Mahaputra
Dosen pengampu: I Made Satria Bimantara, S.Kom., M.Kom.
