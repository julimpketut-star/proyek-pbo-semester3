package com.studiomusik.controller;

import com.studiomusik.AppInfo;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller untuk tampilan starter (main-view.fxml). Hanya menangani tombol uji
 * interaksi; tidak berisi SQL maupun aturan bisnis.
 */
public class MainController {

    @FXML
    private Label appTitleLabel;

    @FXML
    private Label versionLabel;

    @FXML
    private Label statusLabel;

    private int clickCount;

    @FXML
    private void initialize() {
        appTitleLabel.setText(AppInfo.APP_NAME);
        versionLabel.setText("Versi " + AppInfo.VERSION + "  |  Java " + AppInfo.JAVA_RELEASE
                + "  |  " + AppInfo.COURSE);
    }

    @FXML
    private void onTestButtonClick() {
        clickCount++;
        statusLabel.setText("Tombol berfungsi. Jumlah klik: " + clickCount);
        if (!statusLabel.getStyleClass().contains("status-ok")) {
            statusLabel.getStyleClass().add("status-ok");
        }
    }
}
