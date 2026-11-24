package main.java.model;

import java.util.Locale;

// Helper format angka, misal 10000.0 jadi 10.000
public final class Format {
    private Format() { }

    public static String rupiah(double harga) {
        return String.format(Locale.US, "%,.0f", harga).replace(",", ".");
    }
}
