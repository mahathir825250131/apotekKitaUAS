package main.java.model;

// Class abstract Obat (sesuai diagram sistem)
public abstract class Obat {
    private String kode;
    private String nama;
    private float harga;
    private int stok;

    //constructor
    public Obat(String kode, String nama, float harga, int stok) {
        this.kode = kode;
        this.nama = nama;
        this.harga = harga;
        this.stok = stok;
    }

    //setter dan getter
    public void setKode(String kode) { this.kode = kode; }
    public String getKode() { return kode; }
    public void setNama(String nama) { this.nama = nama; }
    public String getNama() { return nama; }
    public void setHarga(float harga) { this.harga = harga; }
    public float getHarga() { return harga; }
    public void setStok(int stok) { this.stok = stok; }
    public int getStok() { return stok; }

    //method abstract utk deskripsi obat (di-override oleh ObatResep & ObatBebas)
    public abstract String deskripsiObat();

    //jenis obat utk ditampilkan di tabel
    public abstract String getJenis();

    //dipakai ComboBox / ListView
    @Override
    public String toString() {
        return kode + " - " + nama;
    }
}
