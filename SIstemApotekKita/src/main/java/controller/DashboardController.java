package main.java.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class DashboardController {

    @FXML private TabPane mainTabPane;

    @FXML
    public void initialize() {
        // muat ulang data setiap kali pindah tab (misal stok berubah setelah transaksi)
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && newTab.getUserData() instanceof Refreshable) {
                ((Refreshable) newTab.getUserData()).refresh();
            }
        });
        showPembelianView();
    }

    @FXML
    private void showObatView() {
        openOrSelectTab("Data Obat", "/main/resources/obat_table.fxml");
    }

    @FXML
    private void showPembelianView() {
        openOrSelectTab("Pembelian Obat", "/main/resources/pembelian.fxml");
    }

    @FXML
    private void showRiwayatView() {
        openOrSelectTab("Riwayat Transaksi", "/main/resources/riwayat_table.fxml");
    }

    @FXML
    private void handleExit() {
        // memicu konfirmasi keluar yang sama dengan tombol close jendela (lihat Main)
        Window window = mainTabPane.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    /** Membuka tab baru, atau pindah ke tab tersebut jika sudah terbuka. */
    private void openOrSelectTab(String title, String fxmlFile) {
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getText().equals(title)) {
                mainTabPane.getSelectionModel().select(tab);
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent content = loader.load();

            Tab newTab = new Tab(title);
            newTab.setContent(content);
            newTab.setUserData(loader.getController());
            mainTabPane.getTabs().add(newTab);
            mainTabPane.getSelectionModel().select(newTab);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal membuka " + title + ": " + e.getMessage());
            alert.showAndWait();
        }
    }
}
