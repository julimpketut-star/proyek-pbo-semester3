/**
 * Layer Service: validasi input, aturan bisnis (BR-01…BR-22), pengaturan transaksi, dan otorisasi
 * kepemilikan data (BR-16). Tidak mengimpor {@code javafx.*} dan tidak berisi SQL (semua SQL di DAO).
 * {@link java.sql.SQLException} dari DAO dibungkus menjadi
 * {@link com.studiomusik.service.ServiceException} berpesan umum; detail teknis hanya ke log developer.
 * Kontrak: docs/API_CONTRACTS.md §4 dan §6. Diimplementasi pada STEP 5.
 */
package com.studiomusik.service;
