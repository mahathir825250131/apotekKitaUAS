-- Database Apotek Kita (Sistem Pembelian Obat)
-- 1. Buat database khusus dulu (di pgAdmin: klik kanan Databases > Create, atau):
--      CREATE DATABASE apotek_kita;
-- 2. Buka Query Tool di database apotek_kita, lalu jalankan file ini.
-- Strategi inheritance: joined table -> obat (data umum),
-- obat_resep & obat_bebas (data khusus), memakai obat.kode sebagai PK/FK.

CREATE TABLE IF NOT EXISTS obat (
    kode  VARCHAR(10)  PRIMARY KEY,
    nama  VARCHAR(100) NOT NULL,
    harga REAL         NOT NULL CHECK (harga >= 0),
    stok  INTEGER      NOT NULL CHECK (stok >= 0),
    jenis VARCHAR(5)   NOT NULL CHECK (jenis IN ('RESEP', 'BEBAS'))
);

CREATE TABLE IF NOT EXISTS obat_resep (
    kode        VARCHAR(10) PRIMARY KEY
        REFERENCES obat(kode) ON DELETE CASCADE ON UPDATE CASCADE,
    nomor_resep VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS obat_bebas (
    kode     VARCHAR(10) PRIMARY KEY
        REFERENCES obat(kode) ON DELETE CASCADE ON UPDATE CASCADE,
    kategori VARCHAR(50) NOT NULL
);

-- Pembayaran (transaksi) 1 --- 1..* Obat
--   list_obat : array kode obat yang dibeli, misal {R001,B001}
CREATE TABLE IF NOT EXISTS transaksi (
    id           INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bill_no      VARCHAR(20) NOT NULL UNIQUE,
    tgl_waktu    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    list_obat    VARCHAR(10)[] NOT NULL DEFAULT '{}',
    total_harga  REAL        NOT NULL,
    metode_bayar VARCHAR(20) NOT NULL,
    uang_bayar   REAL        NOT NULL
);

-- detail menyimpan salinan nama/harga/no resep supaya struk lama tetap benar
-- walaupun data obat nanti diubah atau dihapus
CREATE TABLE IF NOT EXISTS transaksi_detail (
    id           INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaksi_id INTEGER      NOT NULL REFERENCES transaksi(id) ON DELETE CASCADE,
    kode_obat    VARCHAR(10)  NOT NULL,
    nama_obat    VARCHAR(100) NOT NULL,
    jenis        VARCHAR(5)   NOT NULL,
    keterangan   VARCHAR(50)  NOT NULL, -- nomor resep / kategori
    harga        REAL         NOT NULL,
    qty          INTEGER      NOT NULL CHECK (qty > 0)
);

-- Data awal (dari obat.txt versi console)
INSERT INTO obat (kode, nama, harga, stok, jenis) VALUES
    ('R001', 'Paracetamol 500mg', 5000, 20, 'RESEP'),
    ('R002', 'Amoxicillin 500mg', 12000, 20, 'RESEP'),
    ('R003', 'Ibuprofen 400mg', 7000, 20, 'RESEP'),
    ('R004', 'Omeprazole 20mg', 10000, 15, 'RESEP'),
    ('R005', 'Cetirizine 10mg', 6000, 25, 'RESEP'),
    ('B001', 'Vitamin C', 8000, 25, 'BEBAS'),
    ('B002', 'Antasida', 6000, 15, 'BEBAS'),
    ('B003', 'Obat Batuk Sirup', 12000, 20, 'BEBAS'),
    ('B004', 'Minyak Kayu Putih', 15000, 20, 'BEBAS'),
    ('B005', 'Salep Luka', 10000, 25, 'BEBAS')
ON CONFLICT (kode) DO NOTHING;

INSERT INTO obat_resep (kode, nomor_resep) VALUES
    ('R001', 'RSP001'), ('R002', 'RSP002'), ('R003', 'RSP003'),
    ('R004', 'RSP004'), ('R005', 'RSP005')
ON CONFLICT (kode) DO NOTHING;

INSERT INTO obat_bebas (kode, kategori) VALUES
    ('B001', 'Vitamin'), ('B002', 'Lambung'), ('B003', 'Batuk'),
    ('B004', 'Masuk Angin'), ('B005', 'Luka')
ON CONFLICT (kode) DO NOTHING;
