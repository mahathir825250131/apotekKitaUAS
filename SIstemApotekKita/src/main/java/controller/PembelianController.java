package main.java.controller;

import java.sql.SQLException;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import main.java.database.ObatDAO;
import main.java.database.TransaksiDAO;
import main.java.model.Format;
import main.java.model.ItemKeranjang;
import main.java.model.MetodeBayar;
import main.java.model.Obat;
import main.java.model.Pembayaran;
import main.java.model.QRIS;
import main.java.model.Tunai;
import main.java.view.StrukWindow;

public class PembelianController implements Refreshable {

    @FXML private TextField cariField;
    @FXML private TableView<Obat> obatTable;
    @FXML private TableColumn<Obat, String> hargaColumn;
    @FXML private TableColumn<Obat, Integer> sisaColumn;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private TableView<ItemKeranjang> keranjangTable;
    @FXML private Label totalLabel;
    @FXML private ComboBox<MetodeBayar> metodeComboBox;
    @FXML private Label uangLabel;
    @FXML private TextField uangField;
    @FXML private Label kembalianTitle;
    @FXML private Label kembalianLabel;

    private final ObatDAO obatDAO = new ObatDAO();
    private final TransaksiDAO transaksiDAO = new TransaksiDAO();
    private final ObservableList<Obat> dataObat = FXCollections.observableArrayList();

    // keranjang belanja = object Pembayaran (berisi list obat, total, metode bayar)
    private Pembayaran pembayaran = new Pembayaran();

    @FXML
    public void initialize() {
        FilteredList<Obat> filtered = new FilteredList<>(dataObat, o -> true);
        cariField.textProperty().addListener((obs, oldValue, teks) -> {
            String cari = teks == null ? "" : teks.trim().toLowerCase();
            filtered.setPredicate(o -> cari.isEmpty()
                    || o.getKode().toLowerCase().contains(cari)
                    || o.getNama().toLowerCase().contains(cari));
        });
        obatTable.setItems(filtered);

        hargaColumn.setCellValueFactory(c -> new SimpleStringProperty("Rp." + Format.rupiah(c.getValue().getHarga())));
        // stok yang masih bisa dibeli = stok di database - yang sudah di keranjang
        sisaColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(sisaStok(c.getValue())));

        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));

        // polymorphism: Tunai dan QRIS sama-sama MetodeBayar
        metodeComboBox.setItems(FXCollections.observableArrayList(new Tunai(), new QRIS()));
        metodeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateFormBayar());
        uangField.textProperty().addListener((obs, oldValue, newValue) -> updateKembalian());

        updateFormBayar();
        refresh();
    }

    @Override
    public void refresh() {
        try {
            dataObat.setAll(obatDAO.getAllObat());
            // pakai data obat terbaru utk isi keranjang (harga/stok bisa berubah)
            Pembayaran baru = new Pembayaran();
            for (ItemKeranjang item : pembayaran.getItemKeranjang()) {
                for (Obat o : dataObat) {
                    if (o.getKode().equals(item.getKode())) {
                        baru.tambahObat(o, Math.min(item.getQty(), o.getStok()));
                    }
                }
            }
            baru.setMetodeBayar(metodeComboBox.getValue());
            pembayaran = baru;
            updateKeranjang();
        } catch (SQLException ex) {
            showError("Gagal memuat data obat: " + ex.getMessage());
        }
    }

    private int sisaStok(Obat obat) {
        return obat.getStok() - pembayaran.cariJumlah(obat.getKode());
    }

    @FXML
    private void handleTambah() {
        Obat obat = obatTable.getSelectionModel().getSelectedItem();
        if (obat == null) {
            showWarning("Pilih obat di tabel terlebih dahulu.");
            return;
        }
        Integer jumlah = readQty();
        if (jumlah == null) return;

        if (sisaStok(obat) <= 0) {
            showWarning("Stok " + obat.getNama() + " habis.");
            return;
        }
        if (jumlah > sisaStok(obat)) {
            showWarning("Jumlah melebihi stok yang tersedia (" + sisaStok(obat) + ").");
            return;
        }

        pembayaran.tambahObat(obat, jumlah);
        qtySpinner.getValueFactory().setValue(1);
        updateKeranjang();
    }

    private Integer readQty() {
        // commit teks yang diketik manual di spinner
        try {
            int nilai = Integer.parseInt(qtySpinner.getEditor().getText().trim());
            if (nilai <= 0) throw new NumberFormatException();
            return nilai;
        } catch (NumberFormatException ex) {
            showWarning("Jumlah harus berupa angka lebih dari 0.");
            return null;
        }
    }

    @FXML
    private void handleHapusItem() {
        ItemKeranjang item = keranjangTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            showWarning("Pilih item di keranjang yang ingin dihapus.");
            return;
        }
        pembayaran.hapusObat(item.getKode());
        updateKeranjang();
    }

    @FXML
    private void handleKosongkan() {
        pembayaran.kosongkan();
        updateKeranjang();
    }

    @FXML
    private void handleBayar() {
        if (pembayaran.getListObat().isEmpty()) {
            showWarning("Keranjang masih kosong.");
            return;
        }
        MetodeBayar metode = metodeComboBox.getValue();
        if (metode == null) {
            showWarning("Pilih metode pembayaran.");
            return;
        }

        float total = pembayaran.getTotalHarga();
        float uang;
        if (metode instanceof Tunai) {
            Float diterima = parseUang();
            if (diterima == null) {
                showWarning("Masukkan jumlah uang yang diterima.");
                return;
            }
            if (diterima < total) {
                showWarning("Uang yang diterima kurang dari total (Rp." + Format.rupiah(total) + ").");
                return;
            }
            uang = diterima;
        } else {
            uang = total; // QRIS dibayar pas
        }

        pembayaran.setMetodeBayar(metode);
        pembayaran.setUangBayar(uang);

        try {
            transaksiDAO.simpanTransaksi(pembayaran);
        } catch (SQLException ex) {
            showError("Transaksi gagal disimpan: " + ex.getMessage());
            refresh();
            return;
        }

        pembayaran.bayar();      // method milik Tunai / QRIS
        pembayaran.cetakInfo();  // dari interface Cetak
        pembayaran.cetakStruk(); // dari interface Cetak (console)
        StrukWindow.show(pembayaran, obatTable.getScene().getWindow());

        // mulai transaksi baru
        pembayaran = new Pembayaran();
        metodeComboBox.setValue(null);
        uangField.clear();
        refresh();
    }

    private void updateKeranjang() {
        keranjangTable.setItems(FXCollections.observableArrayList(pembayaran.getItemKeranjang()));
        totalLabel.setText("Rp." + Format.rupiah(pembayaran.getTotalHarga()));
        obatTable.refresh();
        updateKembalian();
    }

    private void updateFormBayar() {
        boolean tunai = metodeComboBox.getValue() instanceof Tunai;
        uangLabel.setVisible(tunai);
        uangField.setVisible(tunai);
        kembalianTitle.setVisible(tunai);
        kembalianLabel.setVisible(tunai);
        updateKembalian();
    }

    private void updateKembalian() {
        Float uang = parseUang();
        float total = pembayaran.getTotalHarga();
        if (uang == null) {
            kembalianLabel.setText("Rp.0");
        } else if (uang < total) {
            kembalianLabel.setText("Kurang Rp." + Format.rupiah(total - uang));
        } else {
            kembalianLabel.setText("Rp." + Format.rupiah(uang - total));
        }
    }

    private Float parseUang() {
        String teks = uangField.getText() == null ? "" : uangField.getText().replace(".", "").trim();
        if (teks.isEmpty()) return null;
        try {
            float uang = Float.parseFloat(teks);
            return (Float.isFinite(uang) && uang >= 0) ? uang : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
