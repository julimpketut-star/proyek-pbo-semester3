package com.studiomusik;

import java.net.URL;

import com.studiomusik.controller.Navigator;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Entry point aplikasi JavaFX. Membuat satu Scene (app.css), lalu menyerahkan navigasi halaman
 * ke {@link Navigator}; halaman pertama adalah Landing. Starter STEP 1 (main-view.fxml +
 * MainController) dipertahankan sebagai referensi dan tidak lagi dimuat.
 */
public class MainApp extends Application {

    private static final String CSS_PATH = "/com/studiomusik/css/app.css";

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new StackPane(), 1180, 760);
        URL css = MainApp.class.getResource(CSS_PATH);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        Navigator.init(stage, scene);
        Navigator.showLanding();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
