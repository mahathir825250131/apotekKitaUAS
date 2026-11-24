package main.java.model;

// Satu baris di keranjang / struk: obat + jumlah yang dibeli
public class ItemKeranjang {
    private final Obat obat;
    private final int qty;

    public ItemKeranjang(Obat obat, int qty) {
        this.obat = obat;
        this.qty = qty;
    }

    public Obat getObat() { return obat; }
    public int getQty() { return qty; }

    // getter di bawah dipakai PropertyValueFactory pada TableView
    public String getKode() { return obat.getKode(); }
    public String getNama() { return obat.getNama(); }
    public String getHargaText() { return "Rp." + Format.rupiah(obat.getHarga()); }
    public float getSubtotal() { return obat.getHarga() * qty; }
    public String getSubtotalText() { return "Rp." + Format.rupiah(getSubtotal()); }
}
