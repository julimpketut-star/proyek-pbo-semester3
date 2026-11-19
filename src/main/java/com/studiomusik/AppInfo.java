package com.studiomusik;

/**
 * Metadata aplikasi. Hanya berisi konstanta dan helper kecil; tidak boleh
 * berisi logika bisnis.
 */
public final class AppInfo {

    public static final String APP_NAME = "Sistem Pengelolaan Operasional Studio Musik";
    public static final String VERSION = "1.0.0-SNAPSHOT";
    public static final String BASE_PACKAGE = "com.studiomusik";
    public static final String DATABASE_NAME = "studio_musik";
    public static final String COURSE = "Pemrograman Berorientasi Objek (PBO) - Tugas Kelompok II";
    public static final int JAVA_RELEASE = 21;

    private AppInfo() {
        // utility class
    }

    /** Judul jendela utama, contoh: "Sistem ... Studio Musik v1.0.0-SNAPSHOT". */
    public static String getWindowTitle() {
        return APP_NAME + " v" + VERSION;
    }
}
