package com.studiomusik.controller;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.studiomusik.AppInfo;
import com.studiomusik.util.SessionManager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Navigasi sederhana antar-halaman (STEP 7). Satu {@link Scene} dipakai bersama; berpindah halaman
 * = memuat FXML lalu mengganti root. Juga membawa dua data sementara antar-halaman:
 * jadwal pilihan dari Landing ({@link BookingPrefill}) dan pesan untuk halaman Login.
 * Tidak berisi aturan bisnis; guard peran dasar ada di {@code showCustomerDashboard/showAdminDashboard}.
 */
public final class Navigator {

    private static final Logger LOG = Logger.getLogger(Navigator.class.getName());

    private static final String VIEW_DIR = "/com/studiomusik/view/";
    private static final String LANDING = VIEW_DIR + "landing-view.fxml";
    private static final String LOGIN = VIEW_DIR + "login-view.fxml";
    private static final String REGISTER = VIEW_DIR + "register-view.fxml";
    private static final String CUSTOMER_DASHBOARD = VIEW_DIR + "customer-dashboard-view.fxml";
    private static final String ADMIN_DASHBOARD = VIEW_DIR + "admin-dashboard-view.fxml";

    /** Jadwal yang dipilih di Landing, diteruskan ke form Booking Customer. */
    public record BookingPrefill(LocalDate date, LocalTime start, LocalTime end, int roomId) {
    }

    private static Stage stage;
    private static Scene scene;
    private static BookingPrefill pendingPrefill;
    private static String loginNotice;

    private Navigator() {
    }

    public static void init(Stage primaryStage, Scene sharedScene) {
        stage = primaryStage;
        scene = sharedScene;
    }

    // ---- Halaman ----

    /** Landing; membuang jadwal pilihan yang belum terpakai. */
    public static void showLanding() {
        pendingPrefill = null;
        show(LANDING);
    }

    public static void showLogin() {
        show(LOGIN);
    }

    public static void showRegister() {
        show(REGISTER);
    }

    /** Dashboard Customer; bila bukan Customer yang login, diarahkan ke Login. */
    public static void showCustomerDashboard() {
        if (!SessionManager.getInstance().isCustomer()) {
            showLogin();
            return;
        }
        show(CUSTOMER_DASHBOARD);
    }

    /** Dashboard Admin; bila bukan Admin yang login, diarahkan ke Login. */
    public static void showAdminDashboard() {
        if (!SessionManager.getInstance().isAdmin()) {
            showLogin();
            return;
        }
        show(ADMIN_DASHBOARD);
    }

    /** Dashboard sesuai peran pengguna aktif; Login bila belum login. */
    public static void showDashboardForCurrentUser() {
        SessionManager session = SessionManager.getInstance();
        if (session.isAdmin()) {
            showAdminDashboard();
        } else if (session.isCustomer()) {
            showCustomerDashboard();
        } else {
            showLogin();
        }
    }

    // ---- Data sementara antar-halaman ----

    public static void setPendingPrefill(BookingPrefill prefill) {
        pendingPrefill = prefill;
    }

    public static boolean hasPendingPrefill() {
        return pendingPrefill != null;
    }

    /** Mengambil dan menghapus jadwal pilihan Landing. */
    public static Optional<BookingPrefill> consumePrefill() {
        BookingPrefill value = pendingPrefill;
        pendingPrefill = null;
        return Optional.ofNullable(value);
    }

    public static void setLoginNotice(String notice) {
        loginNotice = notice;
    }

    /** Mengambil dan menghapus pesan sukses untuk Login (mis. setelah registrasi). */
    public static Optional<String> consumeLoginNotice() {
        String value = loginNotice;
        loginNotice = null;
        return Optional.ofNullable(value);
    }

    // ---- Internal ----

    private static void show(String path) {
        if (scene == null || stage == null) {
            throw new IllegalStateException("Navigator belum diinisialisasi.");
        }
        URL url = Navigator.class.getResource(path);
        try {
            if (url == null) {
                throw new IOException("File FXML tidak ditemukan: " + path);
            }
            Parent root = FXMLLoader.load(url);
            scene.setRoot(root);
            stage.setTitle(AppInfo.getWindowTitle());
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.SEVERE, "Gagal memuat halaman " + path, e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Halaman tidak dapat dimuat. Silakan mulai ulang aplikasi.");
            alert.setHeaderText("Kesalahan aplikasi");
            alert.showAndWait();
        }
    }
}
