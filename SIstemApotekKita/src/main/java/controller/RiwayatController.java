package main.java.controller;

import java.sql.SQLException;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import main.java.database.TransaksiDAO;
import main.java.model.Format;
import main.java.model.Transaksi;
import main.java.view.StrukWindow;

public class RiwayatController implements Refreshable {

    @FXML private TableView<Transaksi> transaksiTable;
    @FXML private Label ringkasanLabel;

    private final TransaksiDAO transaksiDAO = new TransaksiDAO();

    @FXML
    public void initialize() {
        // double-click baris -> tampilkan struk
        transaksiTable.setRowFactory(tv -> {
            TableRow<Transaksi> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    StrukWindow.show(row.getItem().getPembayaran(), transaksiTable.getScene().getWindow());
                }
            });
            return row;
        });
        refresh();
    }

    @FXML
    @Override
    public void refresh() {
        try {
            List<Transaksi> list = transaksiDAO.getAllTransaksi();
            transaksiTable.setItems(FXCollections.observableArrayList(list));
            double pendapatan = 0;
            for (Transaksi t : list) {
                pendapatan += t.getPembayaran().getTotalHarga();
            }
            ringkasanLabel.setText(list.size() + " transaksi  |  Total pendapatan: Rp." + Format.rupiah(pendapatan));
        } catch (SQLException ex) {
            ringkasanLabel.setText("Koneksi gagal! " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleLihatStruk() {
        Transaksi selected = transaksiTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Pilih transaksi di tabel terlebih dahulu.");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }
        StrukWindow.show(selected.getPembayaran(), transaksiTable.getScene().getWindow());
    }
}
