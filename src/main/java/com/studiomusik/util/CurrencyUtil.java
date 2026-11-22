package com.studiomusik.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Format mata uang Rupiah untuk tampilan, mis. {@code Rp 150.000}. */
public final class CurrencyUtil {

    private CurrencyUtil() {
    }

    /**
     * Memformat nilai sebagai Rupiah dengan pemisah ribuan titik. Pecahan sen hanya ditampilkan
     * bila bukan nol (mis. {@code Rp 150.000,50}). {@code null} ditampilkan sebagai {@code Rp 0}.
     */
    public static String formatRupiah(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        boolean hasFraction = value.stripTrailingZeros().scale() > 0;
        DecimalFormat format = new DecimalFormat(hasFraction ? "#,##0.00" : "#,##0", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return "Rp " + format.format(value);
    }
}
