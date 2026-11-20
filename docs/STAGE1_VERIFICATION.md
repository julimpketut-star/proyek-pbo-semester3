# STAGE 1 VERIFICATION — Maven Project Setup

**Status akhir STEP 1: SELESAI** — gerbang verifikasi lokal lulus pada 2026-11-19 (lihat bagian 0). Bagian 1–5 adalah catatan verifikasi awal di sandbox Claude (2026-11-18) dan dipertahankan sebagai riwayat.

## 0. Bukti Verifikasi Lokal (2026-11-19) — dari pengguna

Sumber: dua screenshot terminal VS Code milik pengguna (Windows, folder `...\Project TK II\studio-musik`, branch Git aktif `feature/database`, status bar "Java: Ready", 0 error / 0 warning).

| Perintah | Bukti pada screenshot | Hasil |
|---|---|---|
| `mvn clean test` | `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` untuk `com.studiomusik.AppInfoTest`; `BUILD SUCCESS`; total 26.342 s; selesai 2026-11-19T11:50:05+08:00 | **Lulus** |
| `mvn package` | Unduhan dari Maven Central berhasil; `Building jar: ...\target\studio-musik-app-1.0.0-SNAPSHOT.jar`; `BUILD SUCCESS`; total 21.224 s; selesai 2026-11-19T11:51:09+08:00 | **Lulus** |
| `mvn clean javafx:run` | Dilaporkan pengguna berhasil (aplikasi tampil di VS Code). **Tidak ada screenshot** pada percakapan ini | Dilaporkan pengguna |
| Tombol "Uji Interaksi" | Kolom hasil pada prompt dibiarkan placeholder `[BERHASIL/GAGAL]` | **Belum dikonfirmasi** |

Catatan: bagian awal output `mvn clean test` (fase `clean`) tidak terlihat pada screenshot karena terpotong; perintah dijalankan sebagai `mvn clean test` dan berakhir `BUILD SUCCESS`.

### Status AC-B setelah bukti lokal
| ID | Status | Keterangan |
|---|---|---|
| AC-B01 | Terpenuhi secara tidak langsung | Output `java -version` tidak ditampilkan, tetapi Maven mengompilasi dengan `release 21` hingga `BUILD SUCCESS`, sehingga JDK yang dipakai Maven minimal 21 |
| AC-B02 | Lulus | `mvn clean test` berhasil |
| AC-B03 | Lulus | Build berhasil dengan dependency terkunci (JavaFX 21.0.5, Connector/J 8.4.0, JUnit 5.10.2); unduhan terlihat pada `mvn package` |
| AC-B04 | Lulus | 5 tes, 0 gagal |
| AC-B05 | Lulus | JAR `studio-musik-app-1.0.0-SNAPSHOT.jar` terbentuk |
| AC-B06 | Dilaporkan pengguna | Tanpa screenshot |
| AC-B07 | Sebagian | Tampilan/CSS dilaporkan berhasil tampil; tombol belum dikonfirmasi |
| AC-B08 | Lulus | Pemeriksaan file (STEP 1) |

**Tindak lanjut ringan (tidak menghambat STEP 3):** klik tombol "Uji Interaksi" sekali dan ambil screenshot jendela untuk lampiran laporan.

---

## Riwayat: Verifikasi Awal di Sandbox (2026-11-18)

Tanggal: 2026-11-18 · Lingkungan: sandbox Linux (Ubuntu 24.04) milik Claude, **tanpa akses ke Maven Central** (`repo.maven.apache.org` → *Unknown host*).

> **Kesimpulan jujur (kondisi saat itu):** Perintah Maven yang diminta (`mvn clean/test/package/javafx:run`) **TIDAK berhasil di sandbox** karena Maven tidak dapat mengunduh plugin dan dependency. Itu keterbatasan jaringan sandbox, bukan error yang terdeteksi pada proyek. Yang terbukti hanya verifikasi **parsial** (bagian 3). Verifikasi penuh harus dijalankan di VS Code/komputer lokal Anda (bagian 4).

## 1. Hasil perintah yang diminta (aktual)

| Perintah | Hasil di sandbox | Catatan |
|---|---|---|
| `java -version` | **Berhasil** — OpenJDK 21.0.13 | Awalnya hanya JRE; JDK dipasang via `apt` |
| `javac -version` | **Berhasil** — javac 21.0.13 | Tidak ada sebelum `apt install openjdk-21-jdk-headless` |
| `mvn -version` | **Berhasil** — Apache Maven 3.8.7 (Java 21) | Dipasang via `apt` (README menyarankan Maven 3.9+) |
| `mvn clean` | **GAGAL** (exit 1) | `maven-clean-plugin:2.5` tidak dapat diunduh: *Unknown host repo.maven.apache.org* |
| `mvn test` | **GAGAL** (exit 1) | `maven-resources-plugin:2.6` tidak dapat diunduh (jaringan) |
| `mvn package` | **GAGAL** (exit 1) | Penyebab sama |
| `mvn clean javafx:run` | **GAGAL** (exit 1) | Plugin compiler/surefire/javafx tidak dapat diunduh (jaringan) |

## 2. Status kriteria AC-B (dari `ACCEPTANCE_CRITERIA.md`)

| ID | Status | Keterangan |
|---|---|---|
| AC-B01 | Lulus (sandbox) | Java 21.0.13 terdeteksi. Perlu diulang di komputer Anda |
| AC-B02 | **Belum terverifikasi** | Butuh jaringan ke Maven Central |
| AC-B03 | **Belum terverifikasi** | Versi JavaFX 21.0.5, Connector/J 8.4.0, JUnit 5.10.2 **belum diuji unduh** |
| AC-B04 | Parsial | 5 tes lulus lewat JUnit console (bukan lewat Maven); `mvn test` belum terverifikasi |
| AC-B05 | **Belum terverifikasi** | JAR belum dibuat |
| AC-B06 | Parsial | Jendela dijalankan di Xvfb dengan JavaFX 11 (bukan 21); `javafx:run` belum terverifikasi |
| AC-B07 | Parsial | Lihat bagian 3 (tanpa screenshot visual) |
| AC-B08 | Lulus | Tidak ada `module-info.java`; `.gitignore` mengabaikan `config/db.properties`; `db.properties.example` hanya placeholder |

## 3. Verifikasi parsial yang benar-benar dijalankan

Memakai paket apt (OpenJFX **11.0.11**, JUnit 5.10.1 + platform console 1.9.1) sebagai pengganti artefak Maven. Ini **bukan** pengganti `mvn`, hanya bukti bahwa kode dan resource bebas dari kesalahan dasar.

1. `javac --release 21` untuk seluruh `src/main/java` → **exit 0** (tanpa error).
2. `javac --release 21` untuk `AppInfoTest` → **exit 0**.
3. JUnit console: **5 tes ditemukan, 5 berhasil, 0 gagal**.
4. `pom.xml` dan `main-view.fxml` divalidasi sebagai XML yang well-formed; `.vscode/extensions.json` valid JSON.
5. Di bawah Xvfb (layar virtual), `MainApp.start()` dijalankan: judul jendela `Sistem Pengelolaan Operasional Studio Musik v1.0.0-SNAPSHOT`; 1 stylesheet terpasang; `appTitleLabel` berisi nama aplikasi; setelah 2 kali `fire()` pada tombol, label status menjadi `Tombol berfungsi. Jumlah klik: 2` dengan style class `status-ok`.
6. Peringatan yang muncul: *"Loading FXML document with JavaFX API of version 21 by JavaFX runtime of version 11"* — wajar karena sandbox memakai JavaFX 11; tidak muncul di JavaFX 21.

Yang **tidak** terbukti: unduhan dependency, `mvn` pada versi yang dikunci, JavaFX 21, penampilan visual (tidak ada screenshot), `mvn package`.

## 4. Yang harus Anda jalankan di lokal (verifikasi penuh)

Dari root `studio-musik/` dengan JDK 21 dan Maven 3.9+ serta internet:
```bash
java -version
javac -version
mvn -version
mvn clean
mvn test
mvn package
mvn clean javafx:run
```
Ekspektasi: `mvn test` → `Tests run: 5, Failures: 0`; `mvn package` → `target/studio-musik-app-1.0.0-SNAPSHOT.jar`; `javafx:run` → jendela gelap bertombol ungu "Uji Interaksi"; setiap klik mengubah label status menjadi hijau "Tombol berfungsi. Jumlah klik: N".

**Jika unduhan versi gagal** (artefak/versi tidak ditemukan): jalankan `mvn dependency:resolve`, lihat versi mana yang gagal, ganti hanya properti versi di `pom.xml` (`javafx.version`, `mysql.connector.version`, `junit.version`, dst.) dengan versi stabil 21.x/8.4.x/5.10.x yang tersedia, lalu catat di `DECISION_LOG.md` (entri baru berikutnya). Jangan memakai beta/snapshot.

Centang saat selesai (isi dengan bukti/screenshot):
- [ ] `mvn clean` sukses
- [ ] Dependency terunduh
- [ ] `mvn test` — 5 tes lulus
- [ ] `mvn package` — JAR terbentuk
- [ ] `mvn clean javafx:run` — jendela terbuka (screenshot)
- [ ] CSS terlihat (latar gelap, tombol ungu)
- [ ] Tombol mengubah label status

## 5. Pemeriksaan konsistensi dokumen
Dijalankan setelah STEP 0 (lihat `PROGRESS_HANDOFF.md` §6): versi di `pom.xml` = versi di `ARCHITECTURE_CONTRACT.md` dan `DECISION_LOG.md`; nomor tahap seragam (0 Spesifikasi, 1 Maven, 2 SQL, 3 Model, 4 DAO, 5 Service, 6 FXML, 7 Controller, 8 Integrasi, 9 README/ZIP); semua BR yang dirujuk ada; tidak ada konflik kritis yang menghalangi scaffold.
