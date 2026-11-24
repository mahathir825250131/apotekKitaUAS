# Apotek KITA - Sistem Pembelian Obat

Aplikasi kasir Apotek Kita berbasis **JavaFX + FXML** dengan pola **MVC**
dan database **PostgreSQL**.

Anggota kelompok:
1. Nicholas Andersen (825250071)
2. Gisella Aureliu (825250078)
3. Mahathir Muhammad Idrus (825250131)

## Fitur

- **Pembelian Obat**: cari obat, masukkan ke keranjang (stok dicek), pilih metode bayar
  (Tunai / QRIS), hitung kembalian, lalu cetak struk bergaya struk kasir apotek.
- **Data Obat**: tambah / ubah / hapus obat resep dan obat bebas.
- **Riwayat Transaksi**: daftar transaksi + lihat / print ulang struk.

## Struktur (MVC)

```
src/
  App.java                      -> titik masuk program
  main/java/model/              -> Obat (abstract), ObatResep, ObatBebas,
                                   MetodeBayar, Tunai, QRIS, Cetak (interface),
                                   Pembayaran, ItemKeranjang, Transaksi, Format
  main/java/view/               -> Main (JavaFX Application), StrukWindow
  main/java/controller/         -> DashboardController, PembelianController,
                                   ObatTableController, RiwayatController
  main/java/database/           -> PostgreSQLConnection, ObatDAO, TransaksiDAO
  main/resources/               -> file .fxml dan css/style.css
sql/apotek.sql                  -> pembuatan tabel + data awal obat
```

Class model mengikuti diagram sistem:
`Obat` (abstract) <- `ObatResep`, `ObatBebas`; `MetodeBayar` <- `Tunai`, `QRIS`;
`Pembayaran` implements `Cetak` dan memiliki `MetodeBayar` serta `ArrayList<Obat>`.

## Cara Menjalankan

1. Buat database `apotek_kita` (owner = user yang dipakai aplikasi), lalu jalankan
   `sql/apotek.sql` di database tersebut.
2. Sesuaikan URL, user, dan password di `src/main/java/database/PostgreSQLConnection.java`.
3. Pastikan JavaFX SDK dan driver PostgreSQL ada di `c:\javafx-sdk\javafx-sdk-27\lib`
   (lihat `.vscode/settings.json`).
4. Jalankan `App` dari VS Code.
