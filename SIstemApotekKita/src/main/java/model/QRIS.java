package main.java.model;

public class QRIS extends MetodeBayar {

    //constructor, super memanggil constructor MetodeBayar dengan parameter "QRIS"
    public QRIS() { super("QRIS"); }

    //overriding method bayar
    @Override
    public void bayar(float jumlah) {
        System.out.println("Bayar QRIS: Rp." + Format.rupiah(jumlah));
    }
}
