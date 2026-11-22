package com.studiomusik.controller;

import com.studiomusik.service.AuthService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;

/**
 * Registrasi Customer. Controller hanya memeriksa field kosong dan kecocokan "Ulangi Password"
 * (UX); format email/telepon, panjang password, dan email unik divalidasi {@link AuthService}.
 */
public class RegisterController {

    @FXML private ScrollPane rootPane;
    @FXML private Label messageLabel;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        UiSupport.hide(messageLabel);
    }

    @FXML
    private void onRegisterClick() {
        String name = UiSupport.trim(nameField.getText());
        String phone = UiSupport.trim(phoneField.getText());
        String email = UiSupport.trim(emailField.getText());
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            UiSupport.error(messageLabel, "Semua kolom wajib diisi.");
            return;
        }
        if (!password.equals(confirm)) {
            UiSupport.error(messageLabel, "Password dan ulangi password tidak sama.");
            return;
        }

        registerButton.setDisable(true);
        try {
            authService.registerCustomer(name, phone, email, password);
            passwordField.clear();
            confirmPasswordField.clear();
            Navigator.setLoginNotice("Registrasi berhasil. Silakan login dengan email dan password Anda.");
            Navigator.showLogin();
        } catch (RuntimeException e) {
            UiSupport.error(messageLabel, UiSupport.messageOf(e));
        } finally {
            registerButton.setDisable(false);
        }
    }

    @FXML
    private void onBackToLoginClick() {
        Navigator.showLogin();
    }
}
