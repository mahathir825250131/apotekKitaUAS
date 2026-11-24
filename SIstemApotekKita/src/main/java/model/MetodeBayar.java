package main.java.model;

public class MetodeBayar {
    private String namaMetode;

    //constructor
    public MetodeBayar(String namaMetode) { this.namaMetode = namaMetode; }
    public MetodeBayar() { namaMetode = ""; }

    //setter dan getter
    public void setNamaMetode(String namaMetode) { this.namaMetode = namaMetode; }
    public String getNamaMetode() { return namaMetode; }

    //method pembayaran (di-override Tunai & QRIS)
    public void bayar(float jumlah) {
        System.out.println("Pembayaran sebesar Rp." + Format.rupiah(jumlah));
    }

    @Override
    public String toString() { return namaMetode; }
}
