package com.studiomusik.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.studiomusik.model.Booking;
import com.studiomusik.model.BookingInstrument;
import com.studiomusik.model.Customer;
import com.studiomusik.model.Instrument;
import com.studiomusik.model.Room;
import com.studiomusik.model.enums.BookingStatus;
import com.studiomusik.model.enums.InstrumentStatus;
import com.studiomusik.model.enums.RoomStatus;
import com.studiomusik.service.BookingService;
import com.studiomusik.service.CustomerService;
import com.studiomusik.service.InstrumentService;
import com.studiomusik.service.RoomService;
import com.studiomusik.util.CurrencyUtil;
import com.studiomusik.util.DateTimeUtil;
import com.studiomusik.util.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

/**
 * Dashboard Admin: Ringkasan, Booking (status + detail alat), CRUD Customer, Ruangan, Instrumen.
 * Hanya memanggil Service/Util; aturan bisnis (BR-14, BR-17/18/19, BR-22, validasi input)
 * ditegakkan Service. Tombol yang dinonaktifkan di sini hanya UX.
 */
public class AdminDashboardController {

    private static final String ALL_STATUSES = "Semua";
    private static final int MAX_QUANTITY = 99; // batas UX; validasi sebenarnya di Service

    @FXML private BorderPane rootPane;
    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;
    @FXML private TabPane mainTabPane;

    // Ringkasan
    @FXML private Tab summaryTab;
    @FXML private Button refreshSummaryButton;
    @FXML private Label summaryMessageLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label confirmedCountLabel;
    @FXML private Label completedCountLabel;
    @FXML private Label cancelledCountLabel;
    @FXML private Label customerCountLabel;
    @FXML private Label roomCountLabel;
    @FXML private Label instrumentCountLabel;

    // Booking
    @FXML private Tab bookingTab;
    @FXML private ComboBox<String> bookingStatusFilterCombo;
    @FXML private Button refreshBookingsButton;
    @FXML private Label bookingMessageLabel;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, String> bookingIdColumn;
    @FXML private TableColumn<Booking, String> bookingCustomerColumn;
    @FXML private TableColumn<Booking, String> bookingRoomColumn;
    @FXML private TableColumn<Booking, String> bookingDateColumn;
    @FXML private TableColumn<Booking, String> bookingStartColumn;
    @FXML private TableColumn<Booking, String> bookingEndColumn;
    @FXML private TableColumn<Booking, String> bookingStatusColumn;
    @FXML private TableColumn<Booking, String> bookingTotalColumn;
    @FXML private Label bookingDetailTitleLabel;
    @FXML private Label bookingDetailInfoLabel;
    @FXML private ComboBox<BookingStatus> newStatusCombo;
    @FXML private Button updateStatusButton;
    @FXML private TableView<BookingInstrument> detailInstrumentTable;
    @FXML private TableColumn<BookingInstrument, String> detailInstrumentNameColumn;
    @FXML private TableColumn<BookingInstrument, String> detailPriceColumn;
    @FXML private TableColumn<BookingInstrument, String> detailQuantityColumn;
    @FXML private TableColumn<BookingInstrument, String> detailSubtotalColumn;
    @FXML private ComboBox<Instrument> detailInstrumentCombo;
    @FXML private Spinner<Integer> detailQuantitySpinner;
    @FXML private Button addDetailButton;
    @FXML private Button updateDetailButton;
    @FXML private Button removeDetailButton;
    @FXML private Label detailMessageLabel;

    // Customer
    @FXML private Tab customerTab;
    @FXML private Button refreshCustomerButton;
    @FXML private Label customerMessageLabel;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, String> customerIdColumn;
    @FXML private TableColumn<Customer, String> customerNameColumn;
    @FXML private TableColumn<Customer, String> customerPhoneColumn;
    @FXML private TableColumn<Customer, String> customerEmailColumn;
    @FXML private Label customerFormTitleLabel;
    @FXML private TextField customerNameField;
    @FXML private TextField customerPhoneField;
    @FXML private TextField customerEmailField;
    @FXML private PasswordField customerPasswordField;
    @FXML private Button saveCustomerButton;
    @FXML private Button newCustomerButton;
    @FXML private Button deleteCustomerButton;

    // Ruangan
    @FXML private Tab roomTab;
    @FXML private Button refreshRoomButton;
    @FXML private Label roomMessageLabel;
    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, String> roomIdColumn;
    @FXML private TableColumn<Room, String> roomNameColumn;
    @FXML private TableColumn<Room, String> roomTypeColumn;
    @FXML private TableColumn<Room, String> roomPriceColumn;
    @FXML private TableColumn<Room, String> roomStatusColumn;
    @FXML private Label roomFormTitleLabel;
    @FXML private TextField roomNameField;
    @FXML private TextField roomTypeField;
    @FXML private TextField roomPriceField;
    @FXML private ComboBox<RoomStatus> roomStatusCombo;
    @FXML private Button saveRoomButton;
    @FXML private Button newRoomButton;
    @FXML private Button deleteRoomButton;

    // Instrumen
    @FXML private Tab instrumentTab;
    @FXML private Button refreshInstrumentButton;
    @FXML private Label instrumentMessageLabel;
    @FXML private TableView<Instrument> instrumentTable;
    @FXML private TableColumn<Instrument, String> instrumentIdColumn;
    @FXML private TableColumn<Instrument, String> instrumentNameColumn;
    @FXML private TableColumn<Instrument, String> instrumentTypeColumn;
    @FXML private TableColumn<Instrument, String> instrumentPriceColumn;
    @FXML private TableColumn<Instrument, String> instrumentStatusColumn;
    @FXML private Label instrumentFormTitleLabel;
    @FXML private TextField instrumentNameField;
    @FXML private TextField instrumentTypeField;
    @FXML private TextField instrumentPriceField;
    @FXML private ComboBox<InstrumentStatus> instrumentStatusCombo;
    @FXML private Button saveInstrumentButton;
    @FXML private Button newInstrumentButton;
    @FXML private Button deleteInstrumentButton;

    private final BookingService bookingService = new BookingService();
    private final CustomerService customerService = new CustomerService();
    private final RoomService roomService = new RoomService();
    private final InstrumentService instrumentService = new InstrumentService();
    private final SessionManager session = SessionManager.getInstance();

    private final ObservableList<Booking> allBookings = FXCollections.observableArrayList();
    private final FilteredList<Booking> filteredBookings = new FilteredList<>(allBookings, b -> true);

    private String defaultBookingTitle;
    private String defaultBookingInfo;

    // ---- Inisialisasi ----

    @FXML
    private void initialize() {
        if (!session.isAdmin()) { // AC-G03: Admin saja
            rootPane.setDisable(true);
            Platform.runLater(Navigator::showLogin);
            return;
        }
        welcomeLabel.setText("Halo, " + session.getCurrentUser().getName());
        defaultBookingTitle = bookingDetailTitleLabel.getText();
        defaultBookingInfo = bookingDetailInfoLabel.getText();

        for (Label l : new Label[] {summaryMessageLabel, bookingMessageLabel, detailMessageLabel,
                customerMessageLabel, roomMessageLabel, instrumentMessageLabel}) {
            UiSupport.hide(l);
        }

        setupBookingTab();
        setupCustomerTab();
        setupRoomTab();
        setupInstrumentTab();

        refreshSummary();
        reloadBookings(null);
        reloadDetailInstrumentChoices();
        reloadCustomers();
        reloadRooms();
        reloadInstruments();
    }

    private void setupBookingTab() {
        bookingStatusFilterCombo.getItems().add(ALL_STATUSES);
        for (BookingStatus s : BookingStatus.values()) {
            bookingStatusFilterCombo.getItems().add(s.name());
        }
        bookingStatusFilterCombo.setValue(ALL_STATUSES);
        bookingStatusFilterCombo.valueProperty().addListener((o, a, b) -> applyBookingFilter());

        bookingTable.setItems(filteredBookings);
        UiSupport.textColumn(bookingIdColumn, b -> String.valueOf(b.getBookingId()));
        UiSupport.textColumn(bookingCustomerColumn, b -> b.getCustomer() == null ? "-" : b.getCustomer().getName());
        UiSupport.textColumn(bookingRoomColumn, b -> b.getRoom() == null ? "-" : b.getRoom().getRoomName());
        UiSupport.textColumn(bookingDateColumn, b -> DateTimeUtil.formatDate(b.getBookingDate()));
        UiSupport.textColumn(bookingStartColumn, b -> DateTimeUtil.formatTime(b.getStartTime()));
        UiSupport.textColumn(bookingEndColumn, b -> DateTimeUtil.formatTime(b.getEndTime()));
        UiSupport.badgeColumn(bookingStatusColumn, b -> b.getStatus().name());
        UiSupport.textColumn(bookingTotalColumn, b -> CurrencyUtil.formatRupiah(b.getTotalCost()));

        UiSupport.textColumn(detailInstrumentNameColumn, d -> d.getInstrument().getName());
        UiSupport.textColumn(detailPriceColumn, d -> CurrencyUtil.formatRupiah(d.getRentalPrice()));
        UiSupport.textColumn(detailQuantityColumn, d -> String.valueOf(d.getQuantity()));
        UiSupport.textColumn(detailSubtotalColumn, d -> CurrencyUtil.formatRupiah(d.calculateSubtotal()));

        UiSupport.setConverter(newStatusCombo, BookingStatus::name);
        UiSupport.setConverter(detailInstrumentCombo, i -> i.getName() + " — " + i.getType()
                + " (" + CurrencyUtil.formatRupiah(i.getRentalPrice()) + "/unit)");

        bookingTable.getSelectionModel().selectedItemProperty()
                .addListener((o, old, selected) -> showBookingDetail(selected));
        detailInstrumentTable.getSelectionModel().selectedItemProperty().addListener((o, old, selected) -> {
            if (selected != null) {
                UiSupport.setSpinner(detailQuantitySpinner, selected.getQuantity());
            }
        });
        showBookingDetail(null);
    }

    private void setupCustomerTab() {
        UiSupport.textColumn(customerIdColumn, c -> String.valueOf(c.getId()));
        UiSupport.textColumn(customerNameColumn, Customer::getName);
        UiSupport.textColumn(customerPhoneColumn, Customer::getPhone);
        UiSupport.textColumn(customerEmailColumn, Customer::getEmail);
        customerTable.getSelectionModel().selectedItemProperty().addListener((o, old, c) -> fillCustomerForm(c));
        clearCustomerForm();
    }

    private void setupRoomTab() {
        UiSupport.textColumn(roomIdColumn, r -> String.valueOf(r.getRoomId()));
        UiSupport.textColumn(roomNameColumn, Room::getRoomName);
        UiSupport.textColumn(roomTypeColumn, Room::getRoomType);
        UiSupport.textColumn(roomPriceColumn, r -> CurrencyUtil.formatRupiah(r.getPricePerHour()));
        UiSupport.badgeColumn(roomStatusColumn, r -> r.getStatus().name());
        roomStatusCombo.getItems().setAll(RoomStatus.values());
        UiSupport.setConverter(roomStatusCombo, RoomStatus::name);
        roomTable.getSelectionModel().selectedItemProperty().addListener((o, old, r) -> fillRoomForm(r));
        clearRoomForm();
    }

    private void setupInstrumentTab() {
        UiSupport.textColumn(instrumentIdColumn, i -> String.valueOf(i.getInstrumentId()));
        UiSupport.textColumn(instrumentNameColumn, Instrument::getName);
        UiSupport.textColumn(instrumentTypeColumn, Instrument::getType);
        UiSupport.textColumn(instrumentPriceColumn, i -> CurrencyUtil.formatRupiah(i.getRentalPrice()));
        UiSupport.badgeColumn(instrumentStatusColumn, i -> i.getStatus().name());
        instrumentStatusCombo.getItems().setAll(InstrumentStatus.values());
        UiSupport.setConverter(instrumentStatusCombo, InstrumentStatus::name);
        instrumentTable.getSelectionModel().selectedItemProperty().addListener((o, old, i) -> fillInstrumentForm(i));
        clearInstrumentForm();
    }

    // ---- Header ----

    @FXML
    private void onLogoutClick() {
        session.logout();
        Navigator.showLanding();
    }

    // ---- Ringkasan ----

    @FXML
    private void onRefreshSummary() {
        refreshSummary();
    }

    private void refreshSummary() {
        try {
            Map<BookingStatus, Long> summary = bookingService.getBookingStatusSummary();
            pendingCountLabel.setText(String.valueOf(summary.getOrDefault(BookingStatus.PENDING, 0L)));
            confirmedCountLabel.setText(String.valueOf(summary.getOrDefault(BookingStatus.CONFIRMED, 0L)));
            completedCountLabel.setText(String.valueOf(summary.getOrDefault(BookingStatus.COMPLETED, 0L)));
            cancelledCountLabel.setText(String.valueOf(summary.getOrDefault(BookingStatus.CANCELLED, 0L)));
            customerCountLabel.setText(String.valueOf(customerService.countCustomers()));
            roomCountLabel.setText(String.valueOf(roomService.countRooms()));
            instrumentCountLabel.setText(String.valueOf(instrumentService.countInstruments()));
            UiSupport.hide(summaryMessageLabel);
        } catch (RuntimeException e) {
            UiSupport.error(summaryMessageLabel, UiSupport.messageOf(e));
        }
    }

    // ---- Booking ----

    @FXML
    private void onRefreshBookings() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        reloadBookings(selected == null ? null : selected.getBookingId());
        reloadDetailInstrumentChoices();
        refreshSummary();
    }

    /** Memuat ulang tabel booking; bila {@code keepId} ada di hasil, barisnya dipilih kembali. */
    private void reloadBookings(Integer keepId) {
        try {
            allBookings.setAll(bookingService.getAllBookings());
            UiSupport.hide(bookingMessageLabel);
        } catch (RuntimeException e) {
            UiSupport.error(bookingMessageLabel, UiSupport.messageOf(e));
            return;
        }
        selectBooking(keepId);
    }

    private void selectBooking(Integer id) {
        if (id == null) {
            return;
        }
        for (Booking b : filteredBookings) {
            if (b.getBookingId() == id) {
                bookingTable.getSelectionModel().select(b);
                return;
            }
        }
    }

    private void applyBookingFilter() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        String filter = bookingStatusFilterCombo.getValue();
        filteredBookings.setPredicate(b -> filter == null || ALL_STATUSES.equals(filter)
                || b.getStatus().name().equals(filter));
        if (selected != null) {
            selectBooking(selected.getBookingId());
        }
    }

    private void reloadDetailInstrumentChoices() {
        Instrument keep = detailInstrumentCombo.getValue();
        try {
            List<Instrument> items = instrumentService.getSelectableInstruments();
            detailInstrumentCombo.getItems().setAll(items);
            if (keep != null) {
                items.stream().filter(i -> i.getInstrumentId() == keep.getInstrumentId()).findFirst()
                        .ifPresent(detailInstrumentCombo::setValue);
            }
        } catch (RuntimeException e) {
            UiSupport.error(detailMessageLabel, UiSupport.messageOf(e));
        }
    }

    /** Menampilkan detail booking terpilih (memuat detail alat lewat Service) dan mengatur tombol (UX). */
    private void showBookingDetail(Booking selected) {
        UiSupport.hide(detailMessageLabel);
        if (selected == null) {
            bookingDetailTitleLabel.setText(defaultBookingTitle);
            bookingDetailInfoLabel.setText(defaultBookingInfo);
            detailInstrumentTable.getItems().clear();
            newStatusCombo.getItems().clear();
            setBookingActionsDisabled(true, true);
            return;
        }
        try {
            Booking detail = bookingService.getBookingDetail(selected.getBookingId());
            bookingDetailTitleLabel.setText("Booking #" + detail.getBookingId());
            bookingDetailInfoLabel.setText(
                    (detail.getCustomer() == null ? "-" : detail.getCustomer().getName())
                    + " · " + (detail.getRoom() == null ? "-" : detail.getRoom().getRoomName())
                    + " · " + DateTimeUtil.formatDate(detail.getBookingDate())
                    + " · " + DateTimeUtil.formatTime(detail.getStartTime()) + "–" + DateTimeUtil.formatTime(detail.getEndTime())
                    + " · " + detail.getStatus().name()
                    + " · Total " + CurrencyUtil.formatRupiah(detail.getTotalCost()));
            detailInstrumentTable.getItems().setAll(detail.getInstruments());

            newStatusCombo.getItems().clear();
            for (BookingStatus s : BookingStatus.values()) {
                if (detail.getStatus().canTransitionTo(s)) {
                    newStatusCombo.getItems().add(s);
                }
            }
            boolean noTransition = newStatusCombo.getItems().isEmpty();
            boolean detailLocked = detail.getStatus() != BookingStatus.PENDING
                    && detail.getStatus() != BookingStatus.CONFIRMED; // BR-19 (UX; ditegakkan Service)
            setBookingActionsDisabled(noTransition, detailLocked);
            if (detailLocked) {
                UiSupport.info(detailMessageLabel,
                        "Booking berstatus " + detail.getStatus().name() + " tidak dapat diubah detail alatnya.");
            }
        } catch (RuntimeException e) {
            UiSupport.error(bookingMessageLabel, UiSupport.messageOf(e));
            detailInstrumentTable.getItems().clear();
            setBookingActionsDisabled(true, true);
        }
    }

    private void setBookingActionsDisabled(boolean statusDisabled, boolean detailDisabled) {
        newStatusCombo.setDisable(statusDisabled);
        updateStatusButton.setDisable(statusDisabled);
        detailInstrumentCombo.setDisable(detailDisabled);
        detailQuantitySpinner.setDisable(detailDisabled);
        addDetailButton.setDisable(detailDisabled);
        updateDetailButton.setDisable(detailDisabled);
        removeDetailButton.setDisable(detailDisabled);
    }

    @FXML
    private void onUpdateBookingStatus() {
        UiSupport.hide(bookingMessageLabel);
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        BookingStatus target = newStatusCombo.getValue();
        if (selected == null || target == null) {
            UiSupport.error(bookingMessageLabel, "Pilih booking dan status tujuan terlebih dahulu.");
            return;
        }
        if (!UiSupport.confirm("Ubah status booking",
                "Ubah status booking #" + selected.getBookingId() + " menjadi " + target.name() + "?")) {
            return;
        }
        try {
            bookingService.updateBookingStatus(selected.getBookingId(), target);
            reloadBookings(selected.getBookingId());
            refreshSummary();
            UiSupport.success(bookingMessageLabel,
                    "Status booking #" + selected.getBookingId() + " diubah menjadi " + target.name() + ".");
        } catch (RuntimeException e) {
            UiSupport.error(bookingMessageLabel, UiSupport.messageOf(e));
        }
    }

    @FXML
    private void onAddDetail() {
        UiSupport.hide(detailMessageLabel);
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        Instrument instrument = detailInstrumentCombo.getValue();
        if (selected == null || instrument == null) {
            UiSupport.error(detailMessageLabel, "Pilih booking dan instrumen terlebih dahulu.");
            return;
        }
        Integer qty = readQuantity();
        if (qty == null) {
            return;
        }
        try {
            bookingService.addInstrumentToBooking(selected.getBookingId(), instrument.getInstrumentId(), qty);
            afterDetailChange(selected.getBookingId(), "Instrumen ditambahkan ke booking.");
        } catch (RuntimeException e) {
            UiSupport.error(detailMessageLabel, UiSupport.messageOf(e));
        }
    }

    @FXML
    private void onUpdateDetailQuantity() {
        UiSupport.hide(detailMessageLabel);
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        BookingInstrument detail = detailInstrumentTable.getSelectionModel().getSelectedItem();
        if (selected == null || detail == null) {
            UiSupport.error(detailMessageLabel, "Pilih baris instrumen yang akan diubah.");
            return;
        }
        Integer qty = readQuantity();
        if (qty == null) {
            return;
        }
        try {
            bookingService.updateBookingInstrumentQuantity(detail.getBookingInstrumentId(), qty);
            afterDetailChange(selected.getBookingId(), "Jumlah instrumen diperbarui.");
        } catch (RuntimeException e) {
            UiSupport.error(detailMessageLabel, UiSupport.messageOf(e));
        }
    }

    @FXML
    private void onRemoveDetail() {
        UiSupport.hide(detailMessageLabel);
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        BookingInstrument detail = detailInstrumentTable.getSelectionModel().getSelectedItem();
        if (selected == null || detail == null) {
            UiSupport.error(detailMessageLabel, "Pilih baris instrumen yang akan dihapus.");
            return;
        }
        if (!UiSupport.confirm("Hapus instrumen dari booking",
                "Hapus " + detail.getInstrument().getName() + " dari booking #" + selected.getBookingId() + "?")) {
            return;
        }
        try {
            bookingService.removeInstrumentFromBooking(detail.getBookingInstrumentId());
            afterDetailChange(selected.getBookingId(), "Instrumen dihapus dari booking.");
        } catch (RuntimeException e) {
            UiSupport.error(detailMessageLabel, UiSupport.messageOf(e));
        }
    }

    private Integer readQuantity() {
        Integer qty = UiSupport.readSpinner(detailQuantitySpinner);
        if (qty == null || qty < 1 || qty > MAX_QUANTITY) {
            UiSupport.error(detailMessageLabel, "Jumlah harus bilangan bulat 1–" + MAX_QUANTITY + ".");
            return null;
        }
        return qty;
    }

    /** Total booking berubah setelah CRUD detail: muat ulang tabel dan pilih kembali booking yang sama. */
    private void afterDetailChange(int bookingId, String successText) {
        reloadBookings(bookingId);
        UiSupport.success(detailMessageLabel, successText);
    }

    // ---- Customer ----

    @FXML
    private void onRefreshCustomer() {
        reloadCustomers();
        refreshSummary();
    }

    private void reloadCustomers() {
        try {
            customerTable.getItems().setAll(customerService.getAllCustomers());
            UiSupport.hide(customerMessageLabel);
        } catch (RuntimeException e) {
            UiSupport.error(customerMessageLabel, UiSupport.messageOf(e));
        }
        clearCustomerForm();
    }

    private void fillCustomerForm(Customer c) {
        if (c == null) {
            return;
        }
        customerFormTitleLabel.setText("Ubah Customer");
        customerNameField.setText(c.getName());
        customerPhoneField.setText(c.getPhone());
        customerEmailField.setText(c.getEmail());
        customerPasswordField.clear();
        customerPasswordField.setDisable(true); // updateCustomer tidak mengubah password
    }

    private void clearCustomerForm() {
        customerTable.getSelectionModel().clearSelection();
        customerFormTitleLabel.setText("Tambah Customer");
        customerNameField.clear();
        customerPhoneField.clear();
        customerEmailField.clear();
        customerPasswordField.clear();
        customerPasswordField.setDisable(false);
    }

    @FXML
    private void onNewCustomer() {
        clearCustomerForm();
        UiSupport.hide(customerMessageLabel);
    }

    @FXML
    private void onSaveCustomer() {
        UiSupport.hide(customerMessageLabel);
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        String name = UiSupport.trim(customerNameField.getText());
        String phone = UiSupport.trim(customerPhoneField.getText());
        String email = UiSupport.trim(customerEmailField.getText());
        try {
            if (selected == null) {
                String password = customerPasswordField.getText() == null ? "" : customerPasswordField.getText();
                customerService.addCustomer(name, phone, email, password);
                reloadCustomers();
                UiSupport.success(customerMessageLabel, "Customer berhasil ditambahkan.");
            } else {
                customerService.updateCustomer(selected.getId(), name, phone, email);
                reloadCustomers();
                UiSupport.success(customerMessageLabel, "Data Customer berhasil diperbarui.");
            }
            refreshSummary();
        } catch (RuntimeException e) {
            UiSupport.error(customerMessageLabel, UiSupport.messageOf(e));
        }
    }

    @FXML
    private void onDeleteCustomer() {
        UiSupport.hide(customerMessageLabel);
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiSupport.error(customerMessageLabel, "Pilih Customer yang akan dihapus.");
            return;
        }
        if (!UiSupport.confirm("Hapus Customer", "Hapus Customer \"" + selected.getName() + "\"?")) {
            return;
        }
        try {
            customerService.deleteCustomer(selected.getId());
            reloadCustomers();
            refreshSummary();
            UiSupport.success(customerMessageLabel, "Customer berhasil dihapus.");
        } catch (RuntimeException e) {
            UiSupport.error(customerMessageLabel, UiSupport.messageOf(e)); // BR-22 dari exception
        }
    }

    // ---- Ruangan ----

    @FXML
    private void onRefreshRoom() {
        reloadRooms();
        refreshSummary();
    }

    private void reloadRooms() {
        try {
            roomTable.getItems().setAll(roomService.getAllRooms());
            UiSupport.hide(roomMessageLabel);
        } catch (RuntimeException e) {
            UiSupport.error(roomMessageLabel, UiSupport.messageOf(e));
        }
        clearRoomForm();
    }

    private void fillRoomForm(Room r) {
        if (r == null) {
            return;
        }
        roomFormTitleLabel.setText("Ubah Ruangan");
        roomNameField.setText(r.getRoomName());
        roomTypeField.setText(r.getRoomType());
        roomPriceField.setText(r.getPricePerHour().toPlainString());
        roomStatusCombo.setValue(r.getStatus());
    }

    private void clearRoomForm() {
        roomTable.getSelectionModel().clearSelection();
        roomFormTitleLabel.setText("Tambah Ruangan");
        roomNameField.clear();
        roomTypeField.clear();
        roomPriceField.clear();
        roomStatusCombo.setValue(RoomStatus.AVAILABLE);
    }

    @FXML
    private void onNewRoom() {
        clearRoomForm();
        UiSupport.hide(roomMessageLabel);
    }

    @FXML
    private void onSaveRoom() {
        UiSupport.hide(roomMessageLabel);
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        BigDecimal price;
        try {
            price = UiSupport.parseMoney(roomPriceField.getText());
        } catch (IllegalArgumentException e) {
            UiSupport.error(roomMessageLabel, e.getMessage());
            return;
        }
        RoomStatus status = roomStatusCombo.getValue();
        if (status == null) {
            UiSupport.error(roomMessageLabel, "Status ruangan wajib dipilih.");
            return;
        }
        String name = UiSupport.trim(roomNameField.getText());
        String type = UiSupport.trim(roomTypeField.getText());
        try {
            if (selected == null) {
                roomService.addRoom(new Room(0, name, type, price, status));
                reloadRooms();
                UiSupport.success(roomMessageLabel, "Ruangan berhasil ditambahkan.");
            } else {
                roomService.updateRoom(new Room(selected.getRoomId(), name, type, price, status));
                reloadRooms();
                UiSupport.success(roomMessageLabel, "Ruangan berhasil diperbarui.");
            }
            refreshSummary();
        } catch (RuntimeException e) {
            UiSupport.error(roomMessageLabel, UiSupport.messageOf(e));
        }
    }

    @FXML
    private void onDeleteRoom() {
        UiSupport.hide(roomMessageLabel);
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiSupport.error(roomMessageLabel, "Pilih ruangan yang akan dihapus.");
            return;
        }
        if (!UiSupport.confirm("Hapus Ruangan", "Hapus ruangan \"" + selected.getRoomName() + "\"?")) {
            return;
        }
        try {
            roomService.deleteRoom(selected.getRoomId());
            reloadRooms();
            refreshSummary();
            UiSupport.success(roomMessageLabel, "Ruangan berhasil dihapus.");
        } catch (RuntimeException e) {
            UiSupport.error(roomMessageLabel, UiSupport.messageOf(e));
        }
    }

    // ---- Instrumen ----

    @FXML
    private void onRefreshInstrument() {
        reloadInstruments();
        reloadDetailInstrumentChoices();
        refreshSummary();
    }

    private void reloadInstruments() {
        try {
            instrumentTable.getItems().setAll(instrumentService.getAllInstruments());
            UiSupport.hide(instrumentMessageLabel);
        } catch (RuntimeException e) {
            UiSupport.error(instrumentMessageLabel, UiSupport.messageOf(e));
        }
        clearInstrumentForm();
    }

    private void fillInstrumentForm(Instrument i) {
        if (i == null) {
            return;
        }
        instrumentFormTitleLabel.setText("Ubah Instrumen");
        instrumentNameField.setText(i.getName());
        instrumentTypeField.setText(i.getType());
        instrumentPriceField.setText(i.getRentalPrice().toPlainString());
        instrumentStatusCombo.setValue(i.getStatus());
    }

    private void clearInstrumentForm() {
        instrumentTable.getSelectionModel().clearSelection();
        instrumentFormTitleLabel.setText("Tambah Instrumen");
        instrumentNameField.clear();
        instrumentTypeField.clear();
        instrumentPriceField.clear();
        instrumentStatusCombo.setValue(InstrumentStatus.AVAILABLE);
    }

    @FXML
    private void onNewInstrument() {
        clearInstrumentForm();
        UiSupport.hide(instrumentMessageLabel);
    }

    @FXML
    private void onSaveInstrument() {
        UiSupport.hide(instrumentMessageLabel);
        Instrument selected = instrumentTable.getSelectionModel().getSelectedItem();
        BigDecimal price;
        try {
            price = UiSupport.parseMoney(instrumentPriceField.getText());
        } catch (IllegalArgumentException e) {
            UiSupport.error(instrumentMessageLabel, e.getMessage());
            return;
        }
        InstrumentStatus status = instrumentStatusCombo.getValue();
        if (status == null) {
            UiSupport.error(instrumentMessageLabel, "Status instrumen wajib dipilih.");
            return;
        }
        String name = UiSupport.trim(instrumentNameField.getText());
        String type = UiSupport.trim(instrumentTypeField.getText());
        try {
            if (selected == null) {
                instrumentService.addInstrument(new Instrument(0, name, type, price, status));
                reloadInstruments();
                UiSupport.success(instrumentMessageLabel, "Instrumen berhasil ditambahkan.");
            } else {
                instrumentService.updateInstrument(new Instrument(selected.getInstrumentId(), name, type, price, status));
                reloadInstruments();
                UiSupport.success(instrumentMessageLabel, "Instrumen berhasil diperbarui.");
            }
            reloadDetailInstrumentChoices();
            refreshSummary();
        } catch (RuntimeException e) {
            UiSupport.error(instrumentMessageLabel, UiSupport.messageOf(e));
        }
    }

    @FXML
    private void onDeleteInstrument() {
        UiSupport.hide(instrumentMessageLabel);
        Instrument selected = instrumentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiSupport.error(instrumentMessageLabel, "Pilih instrumen yang akan dihapus.");
            return;
        }
        if (!UiSupport.confirm("Hapus Instrumen", "Hapus instrumen \"" + selected.getName() + "\"?")) {
            return;
        }
        try {
            instrumentService.deleteInstrument(selected.getInstrumentId());
            reloadInstruments();
            reloadDetailInstrumentChoices();
            refreshSummary();
            UiSupport.success(instrumentMessageLabel, "Instrumen berhasil dihapus.");
        } catch (RuntimeException e) {
            UiSupport.error(instrumentMessageLabel, UiSupport.messageOf(e));
        }
    }
}
