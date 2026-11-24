package main.java.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import main.java.model.ItemKeranjang;
import main.java.model.Obat;
import main.java.model.ObatBebas;
import main.java.model.ObatResep;
import main.java.model.Pembayaran;
import main.java.model.Transaksi;

public class TransaksiDAO {

    /**
     * Simpan pembayaran + detail obat, lalu kurangi stok.
     * Semua dalam satu transaksi database: kalau stok tidak cukup, semuanya dibatalkan.
     * BILL# dan waktu diisi ke object pembayaran.
     */
    public void simpanTransaksi(Pembayaran pembayaran) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LocalDateTime waktu = LocalDateTime.now().withNano(0);
                String bill = buatBillNumber(conn, waktu);

                int transaksiId;
                // kode obat yang dibeli, disimpan sebagai array di kolom list_obat
                List<String> kodeObat = new ArrayList<>();
                for (ItemKeranjang item : pembayaran.getItemKeranjang()) {
                    kodeObat.add(item.getKode());
                }

                String sql = "INSERT INTO transaksi (bill_no, tgl_waktu, list_obat, total_harga, metode_bayar, uang_bayar) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, bill);
                    stmt.setTimestamp(2, Timestamp.valueOf(waktu));
                    stmt.setArray(3, conn.createArrayOf("varchar", kodeObat.toArray()));
                    stmt.setFloat(4, pembayaran.getTotalHarga());
                    stmt.setString(5, pembayaran.getMetodeBayar());
                    stmt.setFloat(6, pembayaran.getUangBayar());
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Database tidak mengembalikan ID transaksi.");
                        transaksiId = keys.getInt(1);
                    }
                }

                String sqlDetail = "INSERT INTO transaksi_detail "
                        + "(transaksi_id, kode_obat, nama_obat, jenis, keterangan, harga, qty) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                String sqlStok = "UPDATE obat SET stok = stok - ? WHERE kode = ? AND stok >= ?";
                try (PreparedStatement detail = conn.prepareStatement(sqlDetail);
                     PreparedStatement stok = conn.prepareStatement(sqlStok)) {
                    for (ItemKeranjang item : pembayaran.getItemKeranjang()) {
                        Obat obat = item.getObat();
                        detail.setInt(1, transaksiId);
                        detail.setString(2, obat.getKode());
                        detail.setString(3, obat.getNama());
                        detail.setString(4, ObatDAO.jenisDb(obat));
                        detail.setString(5, obat instanceof ObatResep
                                ? ((ObatResep) obat).getNomorResep()
                                : ((ObatBebas) obat).getKategori());
                        detail.setFloat(6, obat.getHarga());
                        detail.setInt(7, item.getQty());
                        detail.executeUpdate();

                        stok.setInt(1, item.getQty());
                        stok.setString(2, obat.getKode());
                        stok.setInt(3, item.getQty());
                        if (stok.executeUpdate() == 0) {
                            throw new SQLException("Stok " + obat.getNama() + " tidak cukup.");
                        }
                    }
                }

                conn.commit();
                pembayaran.setBillNumber(bill);
                pembayaran.setWaktu(waktu);
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Format BILL# seperti contoh struk apotek: 257 + yyMM + nomor urut 5 digit per bulan. */
    private String buatBillNumber(Connection conn, LocalDateTime waktu) throws SQLException {
        String prefix = "257" + waktu.format(DateTimeFormatter.ofPattern("yyMM"));
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(bill_no FROM 8) AS INTEGER)), 0) "
                + "FROM transaksi WHERE bill_no LIKE ? AND LENGTH(bill_no) = 12";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return prefix + String.format("%05d", rs.getInt(1) + 1);
            }
        }
    }

    public List<Transaksi> getAllTransaksi() throws SQLException {
        String sql = "SELECT t.id, t.bill_no, t.tgl_waktu, t.metode_bayar, t.uang_bayar, "
                + "d.kode_obat, d.nama_obat, d.jenis, d.keterangan, d.harga, d.qty "
                + "FROM transaksi t JOIN transaksi_detail d ON d.transaksi_id = t.id "
                + "ORDER BY t.tgl_waktu DESC, t.id DESC, d.id";
        Map<Integer, Pembayaran> data = new LinkedHashMap<>();

        try (Connection conn = PostgreSQLConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                Pembayaran p = data.get(id);
                if (p == null) {
                    p = new Pembayaran();
                    p.setBillNumber(rs.getString("bill_no"));
                    p.setWaktu(rs.getTimestamp("tgl_waktu").toLocalDateTime());
                    p.setMetodeBayar(rs.getString("metode_bayar"));
                    p.setUangBayar(rs.getFloat("uang_bayar"));
                    data.put(id, p);
                }
                String kode = rs.getString("kode_obat");
                String nama = rs.getString("nama_obat");
                float harga = rs.getFloat("harga");
                String ket = rs.getString("keterangan");
                Obat obat = "RESEP".equals(rs.getString("jenis"))
                        ? new ObatResep(kode, nama, harga, 0, ket)
                        : new ObatBebas(kode, nama, harga, 0, ket);
                p.tambahObat(obat, rs.getInt("qty"));
            }
        }

        List<Transaksi> list = new ArrayList<>();
        for (Map.Entry<Integer, Pembayaran> e : data.entrySet()) {
            list.add(new Transaksi(e.getKey(), e.getValue()));
        }
        return list;
    }
}
