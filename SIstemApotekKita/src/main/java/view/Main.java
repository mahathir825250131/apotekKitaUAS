package main.java.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/main/resources/main_window.fxml"));

        Scene scene = new Scene(loader.load(), 1150, 680);

        primaryStage.setTitle("Apotek Kita");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Keluar Aplikasi");
            alert.setHeaderText("Aplikasi akan ditutup...");
            alert.setContentText("Yakin ingin keluar dan menutup semua jendela?");
            var result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Platform.exit();
            } else {
                e.consume();
            }
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
