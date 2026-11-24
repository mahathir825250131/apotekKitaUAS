package main.java.model;

public class Tunai extends MetodeBayar {

    //constructor
    public Tunai() { super("Tunai"); }

    //overriding method bayar
    @Override
    public void bayar(float jumlah) {
        System.out.println("Bayar tunai: Rp." + Format.rupiah(jumlah));
    }
}
