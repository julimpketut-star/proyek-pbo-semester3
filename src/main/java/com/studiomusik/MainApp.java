package com.studiomusik;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point aplikasi JavaFX. Pada STEP 1 hanya memuat tampilan starter
 * (main-view.fxml + app.css). Navigasi login/dashboard ditambahkan pada tahap
 * Controller dan Navigation.
 */
public class MainApp extends Application {

    private static final String VIEW_PATH = "/com/studiomusik/view/main-view.fxml";
    private static final String CSS_PATH = "/com/studiomusik/css/app.css";

    @Override
    public void start(Stage stage) throws IOException {
        URL fxml = MainApp.class.getResource(VIEW_PATH);
        if (fxml == null) {
            throw new IOException("File FXML tidak ditemukan: " + VIEW_PATH);
        }
        Parent root = FXMLLoader.load(fxml);

        Scene scene = new Scene(root, 760, 460);
        URL css = MainApp.class.getResource(CSS_PATH);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle(AppInfo.getWindowTitle());
        stage.setScene(scene);
        stage.setMinWidth(560);
        stage.setMinHeight(360);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
