package com.studiomusik.controller;

import java.util.Optional;

import com.studiomusik.model.Admin;
import com.studiomusik.model.Customer;
import com.studiomusik.service.AuthService;
import com.studiomusik.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

/**
 * Login Admin dan Customer (D-GUI-05): identifier berisi '@' dianggap email Customer, selain itu
 * username Admin. Ini hanya routing UI; verifikasi password sepenuhnya di {@link AuthService}.
 */
public class LoginController {

    @FXML private StackPane rootPane;
    @FXML private Label messageLabel;
    @FXML private TextField identifierField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerLinkButton;
    @FXML private Button backButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        UiSupport.hide(messageLabel);
        Optional<String> notice = Navigator.consumeLoginNotice();
        if (notice.isPresent()) {
            UiSupport.success(messageLabel, notice.get());
        } else if (Navigator.hasPendingPrefill()) {
            UiSupport.info(messageLabel,
                    "Masuk atau daftar sebagai Customer untuk melanjutkan booking jadwal yang dipilih.");
        }
    }

    @FXML
    private void onLoginClick() {
        String identifier = UiSupport.trim(identifierField.getText());
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        if (identifier.isEmpty() || password.isEmpty()) {
            UiSupport.error(messageLabel, "Username/email dan password wajib diisi.");
            return;
        }

        loginButton.setDisable(true);
        try {
            SessionManager session = SessionManager.getInstance();
            if (identifier.contains("@")) {
                Customer customer = authService.loginCustomer(identifier, password);
                session.login(customer);
                passwordField.clear();
                Navigator.showCustomerDashboard();
            } else {
                Admin admin = authService.loginAdmin(identifier, password);
                session.login(admin);
                passwordField.clear();
                Navigator.consumePrefill(); // Admin tidak membuat booking
                Navigator.showAdminDashboard();
            }
        } catch (RuntimeException e) {
            passwordField.clear();
            UiSupport.error(messageLabel, UiSupport.messageOf(e));
        } finally {
            loginButton.setDisable(false);
        }
    }

    @FXML
    private void onRegisterClick() {
        Navigator.showRegister();
    }

    @FXML
    private void onBackClick() {
        Navigator.showLanding();
    }
}
