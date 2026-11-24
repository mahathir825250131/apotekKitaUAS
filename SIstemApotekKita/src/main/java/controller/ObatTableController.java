package main.java.controller;

import java.sql.SQLException;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import main.java.database.ObatDAO;
import main.java.model.Format;
import main.java.model.Obat;
import main.java.model.ObatBebas;
import main.java.model.ObatResep;

public class ObatTableController implements Refreshable {

    @FXML private TextField kodeField;
    @FXML private TextField namaField;
    @FXML private TextField hargaField;
    @FXML private TextField stokField;
    @FXML private ComboBox<String> jenisComboBox;
    @FXML private Label nomorResepLabel;
    @FXML private TextField nomorResepField;
    @FXML private Label kategoriLabel;
    @FXML private TextField kategoriField;
    @FXML private TableView<Obat> obatTable;
    @FXML private TableColumn<Obat, String> hargaColumn;
    @FXML private TableColumn<Obat, String> deskripsiColumn;
    @FXML private Label lblStatus;

    private final ObatDAO obatDAO = new ObatDAO();
    private final ObservableList<Obat> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        obatTable.setItems(data);
        hargaColumn.setCellValueFactory(c -> new SimpleStringProperty("Rp." + Format.rupiah(c.getValue().getHarga())));
        // polymorphism: deskripsiObat() milik ObatResep / ObatBebas
        deskripsiColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().deskripsiObat()));

        jenisComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateSubtypeFields());
        updateSubtypeFields();
        refresh();

        obatTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                clearForm();
            } else {
                populateForm(selected);
            }
        });
    }

    @Override
    public void refresh() {
        try {
            data.setAll(obatDAO.getAllObat());
            lblStatus.setText("Status: Terhubung ke PostgreSQL. " + data.size() + " obat.");
        } catch (SQLException ex) {
            lblStatus.setText("Status: Koneksi gagal! " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleSubmit() {
        Obat input = readForm();
        if (input == null) return;
        try {
            obatDAO.saveObat(input);
            refresh();
            showAlert("Sukses", "Data obat berhasil ditambahkan.");
            obatTable.getSelectionModel().clearSelection();
            clearForm();
        } catch (SQLException ex) {
            showError("Gagal menambah data: " + ex.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        Obat selected = obatTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Belum Memilih", "Pilih baris di tabel yang ingin diubah.");
            return;
        }
        Obat input = readForm();
        if (input == null) return;
        if (!confirm("Yakin ingin mengubah data obat ini?")) return;

        try {
            obatDAO.updateObat(input, selected.getKode());
            refresh();
            showAlert("Sukses", "Data obat berhasil diubah.");
            obatTable.getSelectionModel().clearSelection();
        } catch (SQLException ex) {
            showError("Gagal mengubah data: " + ex.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Obat selected = obatTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Belum Memilih", "Pilih baris di tabel yang ingin dihapus.");
            return;
        }
        if (!confirm("Yakin ingin menghapus " + selected.getNama() + "?")) return;

        try {
            obatDAO.deleteObat(selected.getKode());
            refresh();
            showAlert("Sukses", "Data obat berhasil dihapus.");
            obatTable.getSelectionModel().clearSelection();
        } catch (SQLException ex) {
            showError("Gagal menghapus data: " + ex.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        obatTable.getSelectionModel().clearSelection();
        clearForm();
    }

    /** Membaca form; mengembalikan ObatResep atau ObatBebas, atau null jika input salah. */
    private Obat readForm() {
        String kode = kodeField.getText().trim();
        String nama = namaField.getText().trim();
        String jenis = jenisComboBox.getValue();
        if (kode.isEmpty() || nama.isEmpty() || jenis == null
                || hargaField.getText().trim().isEmpty() || stokField.getText().trim().isEmpty()) {
            showWarning("Kode, nama, harga, stok, dan jenis obat harus diisi.");
            return null;
        }
        if (kode.length() > 10) {
            showWarning("Kode obat maksimal 10 karakter.");
            return null;
        }

        float harga;
        int stok;
        try {
            harga = Float.parseFloat(hargaField.getText().trim());
            if (!Float.isFinite(harga) || harga < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showWarning("Harga harus berupa angka >= 0.");
            return null;
        }
        try {
            stok = Integer.parseInt(stokField.getText().trim());
            if (stok < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showWarning("Stok harus berupa bilangan bulat >= 0.");
            return null;
        }

        if ("Resep".equals(jenis)) {
            String nomorResep = nomorResepField.getText().trim();
            if (nomorResep.isEmpty()) {
                showWarning("Nomor resep harus diisi untuk obat resep.");
                return null;
            }
            return new ObatResep(kode, nama, harga, stok, nomorResep);
        }
        String kategori = kategoriField.getText().trim();
        if (kategori.isEmpty()) {
            showWarning("Kategori harus diisi untuk obat bebas.");
            return null;
        }
        return new ObatBebas(kode, nama, harga, stok, kategori);
    }

    private void populateForm(Obat obat) {
        kodeField.setText(obat.getKode());
        namaField.setText(obat.getNama());
        hargaField.setText(String.valueOf((long) obat.getHarga()));
        stokField.setText(String.valueOf(obat.getStok()));
        nomorResepField.clear();
        kategoriField.clear();
        if (obat instanceof ObatResep) {
            jenisComboBox.setValue("Resep");
            nomorResepField.setText(((ObatResep) obat).getNomorResep());
        } else if (obat instanceof ObatBebas) {
            jenisComboBox.setValue("Bebas");
            kategoriField.setText(((ObatBebas) obat).getKategori());
        }
        updateSubtypeFields();
    }

    private void updateSubtypeFields() {
        boolean resep = "Resep".equals(jenisComboBox.getValue());
        boolean bebas = "Bebas".equals(jenisComboBox.getValue());
        nomorResepLabel.setVisible(resep);
        nomorResepField.setVisible(resep);
        kategoriLabel.setVisible(bebas);
        kategoriField.setVisible(bebas);
    }

    private void clearForm() {
        kodeField.clear();
        namaField.clear();
        hargaField.clear();
        stokField.clear();
        jenisComboBox.setValue(null);
        nomorResepField.clear();
        kategoriField.clear();
        updateSubtypeFields();
    }

    private boolean confirm(String message) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        confirmation.showAndWait();
        return confirmation.getResult() == ButtonType.YES;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        lblStatus.setText("Status: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
