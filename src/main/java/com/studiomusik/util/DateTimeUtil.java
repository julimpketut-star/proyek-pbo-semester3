package com.studiomusik.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Fungsi umum waktu dan format tampilan tanggal/jam. Tidak berisi aturan bisnis. */
public final class DateTimeUtil {

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", ID);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeUtil() {
    }

    /** Selisih dalam jam penuh (bagian menit dibuang). */
    public static int hoursBetween(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Waktu mulai dan selesai tidak boleh null.");
        }
        return (int) Duration.between(start, end).toHours();
    }

    /** {@code true} bila menit, detik, dan nano bernilai nol. */
    public static boolean isWholeHour(LocalTime t) {
        return t != null && t.getMinute() == 0 && t.getSecond() == 0 && t.getNano() == 0;
    }

    /** Dua rentang waktu bertumpuk bila {@code s1 < e2 && e1 > s2}. */
    public static boolean overlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        if (s1 == null || e1 == null || s2 == null || e2 == null) {
            throw new IllegalArgumentException("Waktu tidak boleh null.");
        }
        return s1.isBefore(e2) && e1.isAfter(s2);
    }

    public static String formatDate(LocalDate d) {
        return d == null ? "-" : DATE_FORMAT.format(d);
    }

    public static String formatTime(LocalTime t) {
        return t == null ? "-" : TIME_FORMAT.format(t);
    }
}
