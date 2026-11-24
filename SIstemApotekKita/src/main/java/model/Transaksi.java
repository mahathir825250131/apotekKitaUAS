package main.java.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Data transaksi yang sudah tersimpan (utk tab Riwayat Transaksi)
public class Transaksi {
    private final int id;
    private final Pembayaran pembayaran;

    public Transaksi(int id, Pembayaran pembayaran) {
        this.id = id;
        this.pembayaran = pembayaran;
    }

    public int getId() { return id; }
    public Pembayaran getPembayaran() { return pembayaran; }

    // getter di bawah dipakai PropertyValueFactory pada TableView
    public String getBillNumber() { return pembayaran.getBillNumber(); }
    public String getTanggal() {
        LocalDateTime waktu = pembayaran.getWaktu();
        return waktu == null ? "-" : waktu.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
    public int getJumlahObat() { return pembayaran.getListObat().size(); }

    // kode obat yang dibeli + jumlahnya, misal "R001 (5x), B001 (2x)"
    public String getDaftarObat() {
        StringBuilder sb = new StringBuilder();
        for (ItemKeranjang item : pembayaran.getItemKeranjang()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(item.getKode()).append(" (").append(item.getQty()).append("x)");
        }
        return sb.toString();
    }
    public String getTotalText() { return "Rp." + Format.rupiah(pembayaran.getTotalHarga()); }
    public String getMetodeBayar() { return pembayaran.getMetodeBayar(); }
}
