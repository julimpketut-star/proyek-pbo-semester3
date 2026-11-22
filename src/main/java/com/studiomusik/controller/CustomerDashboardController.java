package com.studiomusik.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.studiomusik.model.Booking;
import com.studiomusik.model.BookingInstrument;
import com.studiomusik.model.Customer;
import com.studiomusik.model.Instrument;
import com.studiomusik.model.Room;
import com.studiomusik.service.BookingService;
import com.studiomusik.service.InstrumentService;
import com.studiomusik.service.RoomService;
import com.studiomusik.util.CurrencyUtil;
import com.studiomusik.util.DateTimeUtil;
import com.studiomusik.util.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/**
 * Dashboard Customer: Katalog, Booking, Riwayat. Hanya memanggil Service/Util; seluruh validasi
 * aturan bisnis (BR-xx), perhitungan total, dan kepemilikan data (BR-16) ada di Service.
 */
public class CustomerDashboardController {

    private static final int MAX_QUANTITY = 99; // batas UX; validasi sebenarnya di Service
    private static final int FIRST_START_HOUR = 0;
    private static final int LAST_END_HOUR = 23; // D-GUI-04

    /** Baris lokal pilihan instrumen (belum tersimpan sampai Buat Booking). */
    private static final class SelectedInstrument {
        private final Instrument instrument;
        private final int quantity;

        SelectedInstrument(Instrument instrument, int quantity) {
            this.instrument = instrument;
            this.quantity = quantity;
        }
    }

    @FXML private BorderPane rootPane;
    @FXML private Label welcomeLabel;
    @FXML private Button homeButton;
    @FXML private Button logoutButton;
    @FXML private TabPane mainTabPane;

    // Katalog
    @FXML private Tab catalogTab;
    @FXML private Button refreshCatalogButton;
    @FXML private TableView<Room> roomCatalogTable;
    @FXML private TableColumn<Room, String> roomNameColumn;
    @FXML private TableColumn<Room, String> roomTypeColumn;
    @FXML private TableColumn<Room, String> roomPriceColumn;
    @FXML private TableColumn<Room, String> roomStatusColumn;
    @FXML private TableView<Instrument> instrumentCatalogTable;
    @FXML private TableColumn<Instrument, String> instrumentNameColumn;
    @FXML private TableColumn<Instrument, String> instrumentTypeColumn;
    @FXML private TableColumn<Instrument, String> instrumentPriceColumn;
    @FXML private TableColumn<Instrument, String> instrumentStatusColumn;
    @FXML private Label catalogMessageLabel;

    // Booking
    @FXML private Tab bookingTab;
    @FXML private Label bookingMessageLabel;
    @FXML private DatePicker bookingDatePicker;
    @FXML private ComboBox<LocalTime> startTimeCombo;
    @FXML private ComboBox<LocalTime> endTimeCombo;
    @FXML private ComboBox<Room> roomComboBox;
    @FXML private Button checkAvailabilityButton;
    @FXML private Label availabilityLabel;
    @FXML private ComboBox<Instrument> instrumentComboBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Button addInstrumentButton;
    @FXML private TableView<SelectedInstrument> selectedInstrumentsTable;
    @FXML private TableColumn<SelectedInstrument, String> selInstrumentNameColumn;
    @FXML private TableColumn<SelectedInstrument, String> selInstrumentPriceColumn;
    @FXML private TableColumn<SelectedInstrument, String> selInstrumentQtyColumn;
    @FXML private TableColumn<SelectedInstrument, String> selInstrumentSubtotalColumn;
    @FXML private Button removeInstrumentButton;
    @FXML private Button clearInstrumentsButton;
    @FXML private Label estimatedCostLabel;
    @FXML private Label estimateInfoLabel;
    @FXML private Button estimateButton;
    @FXML private Button resetBookingButton;
    @FXML private Button createBookingButton;

    // Riwayat
    @FXML private Tab historyTab;
    @FXML private Button refreshHistoryButton;
    @FXML private Label historyMessageLabel;
    @FXML private TableView<Booking> historyTable;
    @FXML private TableColumn<Booking, String> historyIdColumn;
    @FXML private TableColumn<Booking, String> historyDateColumn;
    @FXML private TableColumn<Booking, String> historyStartColumn;
    @FXML private TableColumn<Booking, String> historyEndColumn;
    @FXML private TableColumn<Booking, String> historyRoomColumn;
    @FXML private TableColumn<Booking, String> historyStatusColumn;
    @FXML private TableColumn<Booking, String> historyTotalColumn;
    @FXML private Label historyDetailTitleLabel;
    @FXML private Label historyDetailInfoLabel;
    @FXML private TableView<BookingInstrument> historyInstrumentsTable;
    @FXML private TableColumn<BookingInstrument, String> histInstrumentNameColumn;
    @FXML private TableColumn<BookingInstrument, String> histInstrumentPriceColumn;
    @FXML private TableColumn<BookingInstrument, String> histInstrumentQtyColumn;
    @FXML private TableColumn<BookingInstrument, String> histInstrumentSubtotalColumn;

    private final RoomService roomService = new RoomService();
    private final InstrumentService instrumentService = new InstrumentService();
    private final BookingService bookingService = new BookingService();
    private final SessionManager session = SessionManager.getInstance();

    private final ObservableList<SelectedInstrument> selectedInstruments = FXCollections.observableArrayList();
    private String defaultDetailTitle;
    private String defaultDetailInfo;

    // ---- Inisialisasi ----

    @FXML
    private void initialize() {
        if (!session.isCustomer()) { // AC-G03: Customer saja
            rootPane.setDisable(true);
            Platform.runLater(Navigator::showLogin);
            return;
        }
        Customer customer = session.getCurrentCustomer();
        welcomeLabel.setText("Halo, " + customer.getName());

        defaultDetailTitle = historyDetailTitleLabel.getText();
        defaultDetailInfo = historyDetailInfoLabel.getText();

        for (Label l : new Label[] {catalogMessageLabel, bookingMessageLabel, availabilityLabel, historyMessageLabel}) {
            UiSupport.hide(l);
        }

        setupCatalogTables();
        setupBookingForm();
        setupHistoryTables();

        loadCatalog();
        reloadChoices();
        loadHistory();

        Optional<Navigator.BookingPrefill> prefill = Navigator.consumePrefill();
        prefill.ifPresent(this::applyPrefill);

        // Pilihan ruangan/instrumen disegarkan setiap kali tab Booking dibuka.
        bookingTab.setOnSelectionChanged(e -> {
            if (bookingTab.isSelected()) {
                reloadChoices();
            }
        });
    }

    private void setupCatalogTables() {
        UiSupport.textColumn(roomNameColumn, Room::getRoomName);
        UiSupport.textColumn(roomTypeColumn, Room::getRoomType);
        UiSupport.textColumn(roomPriceColumn, r -> CurrencyUtil.formatRupiah(r.getPricePerHour()));
        UiSupport.badgeColumn(roomStatusColumn, r -> r.getStatus().name());

        UiSupport.textColumn(instrumentNameColumn, Instrument::getName);
        UiSupport.textColumn(instrumentTypeColumn, Instrument::getType);
        UiSupport.textColumn(instrumentPriceColumn, i -> CurrencyUtil.formatRupiah(i.getRentalPrice()));
        UiSupport.badgeColumn(instrumentStatusColumn, i -> i.getStatus().name());
    }

    private void setupBookingForm() {
        bookingDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now())); // hanya UX; BR-21 tetap di Service
            }
        });

        for (int h = FIRST_START_HOUR; h <= LAST_END_HOUR; h++) {
            LocalTime t = LocalTime.of(h, 0);
            if (h < LAST_END_HOUR) {
                startTimeCombo.getItems().add(t);
            }
            if (h > FIRST_START_HOUR) {
                endTimeCombo.getItems().add(t);
            }
        }
        UiSupport.setConverter(startTimeCombo, DateTimeUtil::formatTime);
        UiSupport.setConverter(endTimeCombo, DateTimeUtil::formatTime);
        UiSupport.setConverter(roomComboBox, r -> r.getRoomName() + " — " + r.getRoomType()
                + " (" + CurrencyUtil.formatRupiah(r.getPricePerHour()) + "/jam)");
        UiSupport.setConverter(instrumentComboBox, i -> i.getName() + " — " + i.getType()
                + " (" + CurrencyUtil.formatRupiah(i.getRentalPrice()) + "/unit)");

        // Hasil cek ketersediaan lama tidak berlaku lagi bila jadwal/ruangan diubah.
        bookingDatePicker.valueProperty().addListener((o, a, b) -> UiSupport.hide(availabilityLabel));
        startTimeCombo.valueProperty().addListener((o, a, b) -> UiSupport.hide(availabilityLabel));
        endTimeCombo.valueProperty().addListener((o, a, b) -> UiSupport.hide(availabilityLabel));
        roomComboBox.valueProperty().addListener((o, a, b) -> UiSupport.hide(availabilityLabel));

        selectedInstrumentsTable.setItems(selectedInstruments);
        UiSupport.textColumn(selInstrumentNameColumn, s -> s.instrument.getName());
        UiSupport.textColumn(selInstrumentPriceColumn, s -> CurrencyUtil.formatRupiah(s.instrument.getRentalPrice()));
        UiSupport.textColumn(selInstrumentQtyColumn, s -> String.valueOf(s.quantity));
        UiSupport.textColumn(selInstrumentSubtotalColumn,
                s -> CurrencyUtil.formatRupiah(s.instrument.calculateCost(s.quantity)));

        estimatedCostLabel.setText(CurrencyUtil.formatRupiah(BigDecimal.ZERO));
    }

    private void setupHistoryTables() {
        UiSupport.textColumn(historyIdColumn, b -> String.valueOf(b.getBookingId()));
        UiSupport.textColumn(historyDateColumn, b -> DateTimeUtil.formatDate(b.getBookingDate()));
        UiSupport.textColumn(historyStartColumn, b -> DateTimeUtil.formatTime(b.getStartTime()));
        UiSupport.textColumn(historyEndColumn, b -> DateTimeUtil.formatTime(b.getEndTime()));
        UiSupport.textColumn(historyRoomColumn, b -> b.getRoom() == null ? "-" : b.getRoom().getRoomName());
        UiSupport.badgeColumn(historyStatusColumn, b -> b.getStatus().name());
        UiSupport.textColumn(historyTotalColumn, b -> CurrencyUtil.formatRupiah(b.getTotalCost()));

        UiSupport.textColumn(histInstrumentNameColumn, d -> d.getInstrument().getName());
        UiSupport.textColumn(histInstrumentPriceColumn, d -> CurrencyUtil.formatRupiah(d.getRentalPrice()));
        UiSupport.textColumn(histInstrumentQtyColumn, d -> String.valueOf(d.getQuantity()));
        UiSupport.textColumn(histInstrumentSubtotalColumn, d -> CurrencyUtil.formatRupiah(d.calculateSubtotal()));

        historyTable.getSelectionModel().selectedItemProperty()
                .addListener((o, old, selected) -> showHistoryDetail(selected));
        resetHistoryDetail();
    }

    // ---- Pemuatan data ----

    private void loadCatalog() {
        try {
            roomCatalogTable.getItems().setAll(roomService.getAllRooms());
            instrumentCatalogTable.getItems().setAll(instrumentService.getAllInstruments());
            UiSupport.hide(catalogMessageLabel);
        } catch (RuntimeException e) {
            UiSupport.error(catalogMessageLabel, UiSupport.messageOf(e));
        }
    }

    /** Memuat ulang pilihan ruangan/instrumen sambil mempertahankan pilihan (berdasarkan id). */
    private void reloadChoices() {
        Room keepRoom = roomComboBox.getValue();
        Instrument keepInstrument = instrumentComboBox.getValue();
        try {
            List<Room> rooms = roomService.getBookableRooms();
            roomComboBox.getItems().setAll(rooms);
            if (keepRoom != null) {
                rooms.stream().filter(r -> r.getRoomId() == keepRoom.getRoomId()).findFirst()
                        .ifPresent(roomComboBox::setValue);
            }
            List<Instrument> instruments = instrumentService.getSelectableInstruments();
            instrumentComboBox.getItems().setAll(instruments);
            if (keepInstrument != null) {
                instruments.stream().filter(i -> i.getInstrumentId() == keepInstrument.getInstrumentId()).findFirst()
                        .ifPresent(instrumentComboBox::setValue);
            }
        } catch (RuntimeException e) {
            UiSupport.error(bookingMessageLabel, UiSupport.messageOf(e));
        }
    }

    private void loadHistory() {
        Customer customer = session.getCurrentCustomer();
        if (customer == null) {
            return;
        }
        try {
            historyTable.getItems().setAll(bookingService.getBookingsByCustomer(customer.getId()));
            UiSupport.hide(historyMessageLabel);
        } catch (RuntimeException e) {
            UiSupport.error(historyMessageLabel, UiSupport.messageOf(e));
        }
        resetHistoryDetail();
    }

    private void showHistoryDetail(Booking selected) {
        Customer customer = session.getCurrentCustomer();
        if (selected == null || customer == null) {
            resetHistoryDetail();
            return;
        }
        try {
            Booking detail = bookingService.getBookingForCustomer(selected.getBookingId(), customer.getId());
            historyDetailTitleLabel.setText("Detail instrumen booking #" + detail.getBookingId());
            historyDetailInfoLabel.setText((detail.getRoom() == null ? "-" : detail.getRoom().getRoomName())
                    + " · " + DateTimeUtil.formatDate(detail.getBookingDate())
                    + " · " + DateTimeUtil.formatTime(detail.getStartTime()) + "–" + DateTimeUtil.formatTime(detail.getEndTime())
                    + " · " + detail.getStatus().name()
                    + " · Total " + CurrencyUtil.formatRupiah(detail.getTotalCost())
                    + (detail.getInstruments().isEmpty() ? " · Tanpa sewa instrumen." : ""));
            historyInstrumentsTable.getItems().setAll(detail.getInstruments());
            UiSupport.hide(historyMessageLabel);
        } catch (RuntimeException e) {
            historyInstrumentsTable.getItems().clear();
            UiSupport.error(historyMessageLabel, UiSupport.messageOf(e));
        }
    }

    private void resetHistoryDetail() {
        historyDetailTitleLabel.setText(defaultDetailTitle);
        historyDetailInfoLabel.setText(defaultDetailInfo);
        historyInstrumentsTable.getItems().clear();
    }

    private void applyPrefill(Navigator.BookingPrefill prefill) {
        bookingDatePicker.setValue(prefill.date());
        startTimeCombo.setValue(prefill.start());
        endTimeCombo.setValue(prefill.end());
        roomComboBox.getItems().stream().filter(r -> r.getRoomId() == prefill.roomId()).findFirst()
                .ifPresent(roomComboBox::setValue);
        mainTabPane.getSelectionModel().select(bookingTab);
        UiSupport.info(bookingMessageLabel,
                "Jadwal pilihan dari Beranda sudah terisi. Periksa kembali, lalu tambahkan instrumen bila perlu.");
    }

    // ---- Header ----

    @FXML
    private void onHomeClick() {
        Navigator.showLanding();
    }

    @FXML
    private void onLogoutClick() {
        session.logout();
        Navigator.showLanding();
    }

    // ---- Katalog ----

    @FXML
    private void onRefreshCatalog() {
        loadCatalog();
        reloadChoices();
    }

    // ---- Booking ----

    /** Input form yang sudah lengkap (bentuk saja); aturan bisnis diperiksa Service. */
    private record BookingInput(Room room, LocalDate date, LocalTime start, LocalTime end) {
    }

    private BookingInput readBookingInput(Label messageTarget) {
        LocalDate date = bookingDatePicker.getValue();
        LocalTime start = startTimeCombo.getValue();
        LocalTime end = endTimeCombo.getValue();
        Room room = roomComboBox.getValue();
        if (date == null || start == null || end == null || room == null) {
            UiSupport.error(messageTarget, "Tanggal, jam mulai, jam selesai, dan ruangan wajib dipilih.");
            return null;
        }
        return new BookingInput(room, date, start, end);
    }

    private Map<Integer, Integer> instrumentMap() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (SelectedInstrument s : selectedInstruments) {
            map.put(s.instrument.getInstrumentId(), s.quantity);
        }
        return map;
    }

    @FXML
    private void onCheckAvailability() {
        UiSupport.hide(bookingMessageLabel);
        BookingInput in = readBookingInput(availabilityLabel);
        if (in == null) {
            return;
        }
        try {
            boolean free = bookingService.isRoomAvailable(in.room().getRoomId(), in.date(), in.start(), in.end());
            if (free) {
                UiSupport.success(availabilityLabel, "Ruangan tersedia pada jadwal ini.");
            } else {
                UiSupport.warning(availabilityLabel, "Jadwal bertabrakan dengan booking lain. Pilih jam atau ruangan lain.");
            }
        } catch (RuntimeException e) {
            UiSupport.error(availabilityLabel, UiSupport.messageOf(e));
        }
    }

    @FXML
    private void onAddInstrument() {
        UiSupport.hide(bookingMessageLabel);
        Instrument instrument = instrumentComboBox.getValue();
        if (instrument == null) {
            UiSupport.error(bookingMessageLabel, "Pilih instrumen terlebih dahulu.");
            return;
        }
        Integer qty = UiSupport.readSpinner(quantitySpinner);
        if (qty == null || qty < 1 || qty > MAX_QUANTITY) {
            UiSupport.error(bookingMessageLabel, "Jumlah harus bilangan bulat 1–" + MAX_QUANTITY + ".");
            return;
        }
        // Instrumen yang sama digabung (satu baris per instrumen; BR-17 ditegakkan Service).
        for (int i = 0; i < selectedInstruments.size(); i++) {
            SelectedInstrument existing = selectedInstruments.get(i);
            if (existing.instrument.getInstrumentId() == instrument.getInstrumentId()) {
                int merged = existing.quantity + qty;
                if (merged > MAX_QUANTITY) {
                    UiSupport.error(bookingMessageLabel, "Total jumlah satu instrumen maksimal " + MAX_QUANTITY + ".");
                    return;
                }
                selectedInstruments.set(i, new SelectedInstrument(instrument, merged));
                resetQuantityInput();
                return;
            }
        }
        selectedInstruments.add(new SelectedInstrument(instrument, qty));
        resetQuantityInput();
    }

    private void resetQuantityInput() {
        instrumentComboBox.setValue(null);
        UiSupport.setSpinner(quantitySpinner, 1);
    }

    @FXML
    private void onRemoveInstrument() {
        SelectedInstrument selected = selectedInstrumentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiSupport.warning(bookingMessageLabel, "Pilih baris instrumen yang akan dihapus.");
            return;
        }
        UiSupport.hide(bookingMessageLabel);
        selectedInstruments.remove(selected);
    }

    @FXML
    private void onClearInstruments() {
        selectedInstruments.clear();
    }

    @FXML
    private void onEstimateCost() {
        UiSupport.hide(bookingMessageLabel);
        BookingInput in = readBookingInput(bookingMessageLabel);
        if (in == null) {
            return;
        }
        try {
            BigDecimal total = bookingService.estimateTotalCost(in.room().getRoomId(), in.start(), in.end(), instrumentMap());
            estimatedCostLabel.setText(CurrencyUtil.formatRupiah(total));
        } catch (RuntimeException e) {
            UiSupport.error(bookingMessageLabel, UiSupport.messageOf(e));
        }
    }

    @FXML
    private void onResetBookingForm() {
        resetBookingForm();
        UiSupport.hide(bookingMessageLabel);
    }

    private void resetBookingForm() {
        bookingDatePicker.setValue(null);
        startTimeCombo.setValue(null);
        endTimeCombo.setValue(null);
        roomComboBox.setValue(null);
        UiSupport.hide(availabilityLabel);
        resetQuantityInput();
        selectedInstruments.clear();
        estimatedCostLabel.setText(CurrencyUtil.formatRupiah(BigDecimal.ZERO));
    }

    @FXML
    private void onCreateBooking() {
        UiSupport.hide(bookingMessageLabel);
        Customer customer = session.getCurrentCustomer();
        if (customer == null) {
            Navigator.showLogin();
            return;
        }
        BookingInput in = readBookingInput(bookingMessageLabel);
        if (in == null) {
            return;
        }
        createBookingButton.setDisable(true);
        try {
            Booking created = bookingService.createBooking(customer.getId(), in.room().getRoomId(),
                    in.date(), in.start(), in.end(), instrumentMap());
            resetBookingForm();
            reloadChoices();
            loadHistory();
            UiSupport.success(bookingMessageLabel, "Booking #" + created.getBookingId()
                    + " berhasil dibuat dengan status " + created.getStatus().name()
                    + ". Total biaya: " + CurrencyUtil.formatRupiah(created.getTotalCost()) + ".");
        } catch (RuntimeException e) {
            UiSupport.error(bookingMessageLabel, UiSupport.messageOf(e));
        } finally {
            createBookingButton.setDisable(false);
        }
    }

    // ---- Riwayat ----

    @FXML
    private void onRefreshHistory() {
        loadHistory();
    }
}
