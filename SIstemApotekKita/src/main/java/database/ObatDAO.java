package main.java.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import main.java.model.Obat;
import main.java.model.ObatBebas;
import main.java.model.ObatResep;

/** DAO dengan strategi joined-table: data umum di tabel obat,
 * data khusus di tabel obat_resep atau obat_bebas. */
public class ObatDAO {

    public List<Obat> getAllObat() throws SQLException {
        List<Obat> list = new ArrayList<>();
        String sql = "SELECT o.kode, o.nama, o.harga, o.stok, o.jenis, "
                + "r.nomor_resep, b.kategori FROM obat o "
                + "LEFT JOIN obat_resep r ON o.kode = r.kode "
                + "LEFT JOIN obat_bebas b ON o.kode = b.kode ORDER BY o.jenis DESC, o.kode";

        try (Connection conn = PostgreSQLConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String kode = rs.getString("kode");
                String nama = rs.getString("nama");
                float harga = rs.getFloat("harga");
                int stok = rs.getInt("stok");

                if ("RESEP".equals(rs.getString("jenis"))) {
                    list.add(new ObatResep(kode, nama, harga, stok, nullToEmpty(rs.getString("nomor_resep"))));
                } else {
                    list.add(new ObatBebas(kode, nama, harga, stok, nullToEmpty(rs.getString("kategori"))));
                }
            }
        }
        return list;
    }

    public void saveObat(Obat obat) throws SQLException {
        String sql = "INSERT INTO obat (kode, nama, harga, stok, jenis) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = PostgreSQLConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, obat.getKode());
                stmt.setString(2, obat.getNama());
                stmt.setFloat(3, obat.getHarga());
                stmt.setInt(4, obat.getStok());
                stmt.setString(5, jenisDb(obat));
                stmt.executeUpdate();
                saveSubtype(conn, obat);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** kodeLama dipakai sebagai acuan karena kode obat juga boleh diubah. */
    public void updateObat(Obat obat, String kodeLama) throws SQLException {
        String sql = "UPDATE obat SET kode = ?, nama = ?, harga = ?, stok = ?, jenis = ? WHERE kode = ?";
        try (Connection conn = PostgreSQLConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // hapus subtype dulu, supaya jenis obat boleh berganti
                deleteSubtype(conn, kodeLama);
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, obat.getKode());
                    stmt.setString(2, obat.getNama());
                    stmt.setFloat(3, obat.getHarga());
                    stmt.setInt(4, obat.getStok());
                    stmt.setString(5, jenisDb(obat));
                    stmt.setString(6, kodeLama);
                    if (stmt.executeUpdate() == 0) {
                        throw new SQLException("Data obat tidak ditemukan.");
                    }
                }
                saveSubtype(conn, obat);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    public void deleteObat(String kode) throws SQLException {
        // obat_resep / obat_bebas ikut terhapus (ON DELETE CASCADE)
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM obat WHERE kode = ?")) {
            stmt.setString(1, kode);
            stmt.executeUpdate();
        }
    }

    private void saveSubtype(Connection conn, Obat obat) throws SQLException {
        String sql;
        String nilai;
        if (obat instanceof ObatResep) {
            sql = "INSERT INTO obat_resep (kode, nomor_resep) VALUES (?, ?)";
            nilai = ((ObatResep) obat).getNomorResep();
        } else {
            sql = "INSERT INTO obat_bebas (kode, kategori) VALUES (?, ?)";
            nilai = ((ObatBebas) obat).getKategori();
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, obat.getKode());
            stmt.setString(2, nilai);
            stmt.executeUpdate();
        }
    }

    private void deleteSubtype(Connection conn, String kode) throws SQLException {
        for (String table : new String[] {"obat_resep", "obat_bebas"}) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + table + " WHERE kode = ?")) {
                stmt.setString(1, kode);
                stmt.executeUpdate();
            }
        }
    }

    static String jenisDb(Obat obat) {
        return obat instanceof ObatResep ? "RESEP" : "BEBAS";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
