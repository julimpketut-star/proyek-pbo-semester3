package com.studiomusik.controller;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.studiomusik.service.BusinessRuleException;
import com.studiomusik.service.ValidationException;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;

/**
 * Pembantu tampilan khusus Controller (package-private). Tidak berisi SQL maupun aturan bisnis:
 * hanya penampil pesan, pemetaan exception ke teks pengguna, pemasang kolom tabel, dan
 * pembaca input UX.
 */
final class UiSupport {

    static final String GENERIC_ERROR = "Terjadi kesalahan pada sistem. Silakan coba lagi.";

    private static final Logger LOG = Logger.getLogger(UiSupport.class.getName());
    private static final String[] MESSAGE_CLASSES = {"message-error", "message-success", "message-info", "message-warning"};
    private static final Pattern MONEY = Pattern.compile("\\d{1,8}([.,]\\d{1,2})?");

    enum MessageType {
        ERROR("message-error"), SUCCESS("message-success"), INFO("message-info"), WARNING("message-warning");

        private final String styleClass;

        MessageType(String styleClass) {
            this.styleClass = styleClass;
        }
    }

    private UiSupport() {
    }

    // ---- Pesan ----

    static void show(Label label, String text, MessageType type) {
        if (label == null) {
            return;
        }
        label.getStyleClass().removeAll(MESSAGE_CLASSES);
        label.getStyleClass().add(type.styleClass);
        label.setText(text);
        label.setManaged(true);
        label.setVisible(true);
    }

    static void error(Label label, String text) {
        show(label, text, MessageType.ERROR);
    }

    static void success(Label label, String text) {
        show(label, text, MessageType.SUCCESS);
    }

    static void info(Label label, String text) {
        show(label, text, MessageType.INFO);
    }

    static void warning(Label label, String text) {
        show(label, text, MessageType.WARNING);
    }

    static void hide(Label label) {
        if (label == null) {
            return;
        }
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    /**
     * Teks aman untuk pengguna dari exception Service: {@link ValidationException} dan
     * {@link BusinessRuleException} memakai {@code getMessage()}; selain itu pesan umum (AC-G04).
     */
    static String messageOf(RuntimeException e) {
        if (e instanceof ValidationException || e instanceof BusinessRuleException) {
            String msg = e.getMessage();
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
        } else {
            LOG.log(Level.WARNING, "Kesalahan tak terduga di lapisan UI/Service", e);
        }
        return GENERIC_ERROR;
    }

    static boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(title);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ---- Tabel ----

    static <S> void textColumn(TableColumn<S, String> column, Function<S, String> extractor) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(extractor.apply(cell.getValue())));
    }

    /** Kolom teks yang ditampilkan sebagai lencana status ({@code badge status-xxx}). */
    static <S> void badgeColumn(TableColumn<S, String> column, Function<S, String> extractor) {
        textColumn(column, extractor);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll("badge", "status-" + item.toLowerCase());
                setText(null);
                setGraphic(badge);
            }
        });
    }

    // ---- Input ----

    static <T> void setConverter(ComboBox<T> combo, Function<T, String> toText) {
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : toText.apply(object);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        });
    }

    /** Membaca Spinner yang dapat diketik; {@code null} bila teks bukan bilangan bulat. */
    static Integer readSpinner(Spinner<Integer> spinner) {
        String text = spinner.getEditor().getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static void setSpinner(Spinner<Integer> spinner, int value) {
        spinner.getValueFactory().setValue(value);
        spinner.getEditor().setText(String.valueOf(value));
    }

    /**
     * Mengubah teks harga menjadi {@link BigDecimal} (hanya bentuk UX). Hanya angka dengan
     * desimal opsional 1-2 digit; pola seperti "150.000" ditolak agar tidak terbaca 150.
     *
     * @throws IllegalArgumentException berpesan Indonesia bila format tidak valid
     */
    static BigDecimal parseMoney(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Harga wajib diisi.");
        }
        if (!MONEY.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Format harga tidak valid. Tulis angka tanpa pemisah ribuan, mis. 150000 atau 150000,50.");
        }
        return new BigDecimal(value.replace(',', '.'));
    }

    static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
