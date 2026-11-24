package main.java.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Pembayaran implements Cetak {
    // lebar kertas struk (karakter)
    private static final int LEBAR = 42;

    private float totalHarga;
    private MetodeBayar metodeBayar;
    private ArrayList<Obat> listObat;

    // data tambahan utk struk
    private String billNumber = "-";
    private LocalDateTime waktu = LocalDateTime.now();
    private float uangBayar;

    //constructor sesuai diagram
    public Pembayaran(float totalHarga, String namaMetode, float uangBayar) {
        this.totalHarga = totalHarga;
        this.metodeBayar = null;
        this.uangBayar = uangBayar;
        listObat = new ArrayList<Obat>();
    }

    public Pembayaran() {
        totalHarga = 0;
        metodeBayar = null;
        listObat = new ArrayList<Obat>();
    }

    //setter dan getter
    public void setTotalHarga(float totalHarga) { this.totalHarga = totalHarga; }
    public float getTotalHarga() { return totalHarga; }

    public void setMetodeBayar(String namaMetode) {
        if ("Tunai".equalsIgnoreCase(namaMetode)) {
            metodeBayar = new Tunai();
        } else if ("QRIS".equalsIgnoreCase(namaMetode)) {
            metodeBayar = new QRIS();
        } else if (metodeBayar != null) {
            metodeBayar.setNamaMetode(namaMetode);
        } else {
            metodeBayar = new MetodeBayar(namaMetode);
        }
    }

    //setter utk object metode bayar (polymorphism: Tunai / QRIS)
    public void setMetodeBayar(MetodeBayar metodeBayar) { this.metodeBayar = metodeBayar; }

    public String getMetodeBayar() {
        if (metodeBayar == null) {
            return "-";
        }
        return metodeBayar.getNamaMetode();
    }

    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }
    public String getBillNumber() { return billNumber; }
    public void setWaktu(LocalDateTime waktu) { this.waktu = waktu; }
    public LocalDateTime getWaktu() { return waktu; }
    public void setUangBayar(float uangBayar) { this.uangBayar = uangBayar; }
    public float getUangBayar() { return uangBayar; }
    public float getKembalian() { return Math.max(0, uangBayar - totalHarga); }

    //tambah obat ke list sebanyak jumlah
    public void tambahObat(Obat obat, int jumlah) {
        for (int i = 0; i < jumlah; i++) {
            tambahObat(obat);
        }
    }

    //tambah 1 obat ke list
    public void tambahObat(Obat obat) {
        listObat.add(obat);
        totalHarga = totalHarga + obat.getHarga();
    }

    //hapus semua obat dengan kode tertentu dari list
    public void hapusObat(String kode) {
        for (int i = listObat.size() - 1; i >= 0; i--) {
            if (listObat.get(i).getKode().equals(kode)) {
                totalHarga = totalHarga - listObat.get(i).getHarga();
                listObat.remove(i);
            }
        }
    }

    public void kosongkan() {
        listObat.clear();
        totalHarga = 0;
        uangBayar = 0;
    }

    public ArrayList<Obat> getListObat() { return listObat; }

    //jumlah obat yg sudah ada di keranjang utk kode tertentu
    public int cariJumlah(String kode) {
        int jumlah = 0;
        for (Obat obat : listObat) {
            if (obat.getKode().equals(kode)) {
                jumlah++;
            }
        }
        return jumlah;
    }

    //gabungkan obat yg sama -> 1 baris + qty
    public ArrayList<ItemKeranjang> getItemKeranjang() {
        Map<String, Obat> obatPerKode = new LinkedHashMap<>();
        Map<String, Integer> qtyPerKode = new LinkedHashMap<>();
        for (Obat obat : listObat) {
            obatPerKode.putIfAbsent(obat.getKode(), obat);
            qtyPerKode.merge(obat.getKode(), 1, Integer::sum);
        }
        ArrayList<ItemKeranjang> items = new ArrayList<>();
        for (String kode : obatPerKode.keySet()) {
            items.add(new ItemKeranjang(obatPerKode.get(kode), qtyPerKode.get(kode)));
        }
        return items;
    }

    //panggil fungsi bayar milik metode bayar
    public void bayar() {
        if (metodeBayar != null) {
            metodeBayar.bayar(totalHarga);
        }
    }

    //ringkasan pembayaran
    public String getInfo() {
        return "Metode bayar : " + getMetodeBayar() + "\n"
                + "Jumlah obat  : " + listObat.size() + "\n"
                + "Total harga  : Rp." + Format.rupiah(totalHarga);
    }

    //cetak info pembayaran ke console
    @Override
    public void cetakInfo() {
        System.out.println(getInfo());
    }

    //cetak struk ke console
    @Override
    public void cetakStruk() {
        System.out.println(getStruk());
    }

    //isi struk, format mengikuti contoh struk apotek
    public String getStruk() {
        String garis = "-".repeat(LEBAR);
        StringBuilder sb = new StringBuilder();
        sb.append(tengah("APOTEK KITA")).append('\n');
        sb.append(tengah("JL S PARMAN KAV 28")).append('\n');
        sb.append(tengah("Telp : 56985086")).append('\n');
        sb.append(garis).append('\n');
        sb.append(String.format("%-12s: %s%n", "BILL#", billNumber));
        sb.append(String.format("%-12s: %s%n", "OUTLET", "Apotek Kita"));
        sb.append(String.format("%-12s: %s%n", "PRINT DATE",
                waktu.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.US))));
        sb.append(garis).append('\n');

        for (ItemKeranjang item : getItemKeranjang()) {
            Obat obat = item.getObat();
            if (obat instanceof ObatResep) {
                sb.append("Recipe: (").append(((ObatResep) obat).getNomorResep()).append(")\n");
            }
            sb.append(obat.getNama().toUpperCase()).append('\n');
            sb.append("Procode: ").append(obat.getKode()).append('\n');
            sb.append("Qty: ").append(item.getQty()).append('\n');
            sb.append(kiriKanan("Price: " + Format.rupiah(obat.getHarga()),
                    Format.rupiah(item.getSubtotal()))).append('\n');
        }

        // harga sudah termasuk PPN 11%
        long dpp = Math.round(totalHarga / 1.11);
        long ppn = Math.round(totalHarga) - dpp;

        sb.append(garis).append('\n');
        sb.append(kiriKanan("Grand Total    :", Format.rupiah(totalHarga))).append('\n');
        sb.append(kiriKanan("Payment        :", Format.rupiah(uangBayar))).append('\n');
        sb.append(kiriKanan("Change         :", Format.rupiah(getKembalian()))).append('\n');
        sb.append(kiriKanan("Disc Total     :", "0")).append('\n');
        sb.append(kiriKanan("Payment Type   :", getMetodeBayar().toUpperCase())).append('\n');
        sb.append(kiriKanan("            DPP:", Format.rupiah(dpp))).append('\n');
        sb.append(kiriKanan("            PPN:", Format.rupiah(ppn))).append('\n');
        sb.append(garis).append('\n');
        sb.append("Customer Care: 0815 1915 3333\n");
        sb.append("*Syarat dan ketentuan berlaku\n");
        sb.append(tengah("May You Recover Soon !"));
        return sb.toString();
    }

    private static String tengah(String teks) {
        int spasi = Math.max(0, (LEBAR - teks.length()) / 2);
        return " ".repeat(spasi) + teks;
    }

    private static String kiriKanan(String kiri, String kanan) {
        int spasi = Math.max(1, LEBAR - kiri.length() - kanan.length());
        return kiri + " ".repeat(spasi) + kanan;
    }
}
