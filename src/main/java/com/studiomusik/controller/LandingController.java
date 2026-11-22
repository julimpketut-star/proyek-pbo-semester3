package com.studiomusik.controller;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.TreeSet;

import com.studiomusik.model.Instrument;
import com.studiomusik.model.Room;
import com.studiomusik.service.BusinessRuleException;
import com.studiomusik.service.InstrumentService;
import com.studiomusik.service.RoomService;
import com.studiomusik.service.BookingService;
import com.studiomusik.util.CurrencyUtil;
import com.studiomusik.util.DateTimeUtil;
import com.studiomusik.util.SessionManager;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Landing Page publik. Browsing (katalog, cari jadwal) tanpa login; memilih slot jadwal tanpa
 * login mengarahkan ke Login/Register (D-GUI-01) karena booking butuh customerId.
 */
public class LandingController {

    /** Rentang jam "Cari Jadwal" (D-GUI-04, belum diputuskan tim): semua jam bulat, akhir sesi maks. 23:00. */
    private static final int FIRST_START_HOUR = 0;
    private static final int LAST_END_HOUR = 23;
    private static final int MAX_DURATION_HOURS = 8;
    private static final int DEFAULT_DURATION_HOURS = 2;
    private static final String HERO_IMAGE_PATH = "/com/studiomusik/images/hero.jpg";

    @FXML private BorderPane rootPane;
    @FXML private Button navHomeButton;
    @FXML private Button navStudioButton;
    @FXML private Button navFacilityButton;
    @FXML private Button navHowToButton;
    @FXML private Button navContactButton;
    @FXML private Button loginButton;
    @FXML private ScrollPane landingScroll;
    @FXML private VBox contentBox;
    @FXML private StackPane heroSection;
    @FXML private ImageView heroImageView;
    @FXML private Button startButton;
    @FXML private DatePicker bookingDatePicker;
    @FXML private ComboBox<Integer> durationComboBox;
    @FXML private ComboBox<String> studioTypeComboBox;
    @FXML private Button searchScheduleButton;
    @FXML private Label quickBookingMessageLabel;
    @FXML private VBox scheduleResultBox;
    @FXML private Label scheduleResultTitleLabel;
    @FXML private FlowPane scheduleResultPane;
    @FXML private VBox studioSection;
    @FXML private FlowPane studioCardsPane;
    @FXML private Label studioEmptyLabel;
    @FXML private VBox facilitySection;
    @FXML private FlowPane facilityCardsPane;
    @FXML private Label facilityEmptyLabel;
    @FXML private VBox howToSection;
    @FXML private Button registerLinkButton;
    @FXML private VBox contactSection;
    @FXML private Label contactAddressLabel;
    @FXML private Label contactPhoneLabel;
    @FXML private Label contactEmailLabel;
    @FXML private Label contactHoursLabel;

    private final RoomService roomService = new RoomService();
    private final InstrumentService instrumentService = new InstrumentService();
    private final BookingService bookingService = new BookingService();
    private final SessionManager session = SessionManager.getInstance();

    @FXML
    private void initialize() {
        UiSupport.hide(quickBookingMessageLabel);
        setupHeroImage();
        setupLoginButtonText();
        setupQuickBookingControls();
        loadStudios();
        loadFacilities();
    }

    // ---- Persiapan tampilan ----

    private void setupHeroImage() {
        try (InputStream in = LandingController.class.getResourceAsStream(HERO_IMAGE_PATH)) {
            if (in == null) {
                return; // tidak ada aset foto: hero tetap gradient (D-GUI-09)
            }
            heroImageView.setImage(new Image(in));
            heroImageView.fitWidthProperty().bind(heroSection.widthProperty());
            heroImageView.fitHeightProperty().bind(heroSection.heightProperty());
            heroImageView.setManaged(false);
            heroImageView.setVisible(true);
        } catch (java.io.IOException | RuntimeException e) {
            heroImageView.setVisible(false);
        }
    }

    private void setupLoginButtonText() {
        if (session.isAdmin()) {
            loginButton.setText("Dashboard Admin");
        } else if (session.isCustomer()) {
            loginButton.setText("Dashboard");
        }
    }

    private void setupQuickBookingControls() {
        bookingDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now())); // hanya UX; BR-21 tetap di Service
            }
        });

        for (int h = 1; h <= MAX_DURATION_HOURS; h++) {
            durationComboBox.getItems().add(h);
        }
        UiSupport.setConverter(durationComboBox, h -> h + " jam");
        durationComboBox.setValue(DEFAULT_DURATION_HOURS);

        try {
            TreeSet<String> types = new TreeSet<>();
            for (Room room : roomService.getBookableRooms()) {
                types.add(room.getRoomType());
            }
            studioTypeComboBox.getItems().setAll(types);
        } catch (RuntimeException e) {
            UiSupport.error(quickBookingMessageLabel, UiSupport.messageOf(e));
        }
    }

    private void loadStudios() {
        studioCardsPane.getChildren().clear();
        try {
            List<Room> rooms = roomService.getAllRooms();
            for (Room room : rooms) {
                studioCardsPane.getChildren().add(buildCard(room.getRoomName(), room.getRoomType(),
                        CurrencyUtil.formatRupiah(room.getPricePerHour()) + " / jam", room.getStatus().name()));
            }
            setEmptyState(studioEmptyLabel, rooms.isEmpty());
        } catch (RuntimeException e) {
            studioEmptyLabel.setText(UiSupport.messageOf(e));
            setEmptyState(studioEmptyLabel, true);
        }
    }

    private void loadFacilities() {
        facilityCardsPane.getChildren().clear();
        try {
            List<Instrument> instruments = instrumentService.getAllInstruments();
            for (Instrument instrument : instruments) {
                facilityCardsPane.getChildren().add(buildCard(instrument.getName(), instrument.getType(),
                        CurrencyUtil.formatRupiah(instrument.getRentalPrice()) + " / unit", instrument.getStatus().name()));
            }
            setEmptyState(facilityEmptyLabel, instruments.isEmpty());
        } catch (RuntimeException e) {
            facilityEmptyLabel.setText(UiSupport.messageOf(e));
            setEmptyState(facilityEmptyLabel, true);
        }
    }

    private static void setEmptyState(Label label, boolean empty) {
        label.setVisible(empty);
        label.setManaged(empty);
    }

    private static VBox buildCard(String title, String subtitle, String price, String status) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-heading");
        titleLabel.setWrapText(true);
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("card-text");
        Label priceLabel = new Label(price);
        priceLabel.getStyleClass().add("card-heading");
        Label badge = new Label(status);
        badge.getStyleClass().addAll("badge", "status-" + status.toLowerCase());

        VBox card = new VBox(8, titleLabel, subtitleLabel, priceLabel, badge);
        card.getStyleClass().add("card");
        card.setPrefWidth(240);
        card.setPadding(new Insets(16));
        return card;
    }

    // ---- Navigasi gulir ----

    private void scrollTo(Node target) {
        contentBox.applyCss();
        contentBox.layout();
        double scrollable = contentBox.getBoundsInLocal().getHeight() - landingScroll.getViewportBounds().getHeight();
        if (target == null || scrollable <= 0) {
            landingScroll.setVvalue(0);
            return;
        }
        double ratio = target.getBoundsInParent().getMinY() / scrollable;
        landingScroll.setVvalue(Math.max(0, Math.min(1, ratio)));
    }

    @FXML
    private void onNavHome() {
        landingScroll.setVvalue(0);
    }

    @FXML
    private void onNavStudio() {
        scrollTo(studioSection);
    }

    @FXML
    private void onNavFacility() {
        scrollTo(facilitySection);
    }

    @FXML
    private void onNavHowTo() {
        scrollTo(howToSection);
    }

    @FXML
    private void onNavContact() {
        scrollTo(contactSection);
    }

    @FXML
    private void onLoginClick() {
        if (session.isLoggedIn()) {
            Navigator.showDashboardForCurrentUser();
        } else {
            Navigator.showLogin();
        }
    }

    @FXML
    private void onRegisterClick() {
        Navigator.showRegister();
    }

    @FXML
    private void onStartClick() {
        landingScroll.setVvalue(0);
        bookingDatePicker.requestFocus();
    }

    // ---- Quick Booking ----

    @FXML
    private void onSearchSchedule() {
        UiSupport.hide(quickBookingMessageLabel);
        scheduleResultBox.setVisible(false);
        scheduleResultBox.setManaged(false);
        scheduleResultPane.getChildren().clear();

        LocalDate date = bookingDatePicker.getValue();
        Integer duration = durationComboBox.getValue();
        String type = studioTypeComboBox.getValue();
        if (date == null || duration == null || type == null) {
            UiSupport.error(quickBookingMessageLabel, "Tanggal, durasi, dan jenis studio wajib dipilih.");
            return;
        }

        List<Room> rooms;
        try {
            rooms = roomService.getBookableRooms().stream()
                    .filter(r -> type.equals(r.getRoomType()))
                    .toList();
        } catch (RuntimeException e) {
            UiSupport.error(quickBookingMessageLabel, UiSupport.messageOf(e));
            return;
        }

        int slots = 0;
        for (int startHour = FIRST_START_HOUR; startHour + duration <= LAST_END_HOUR; startHour++) {
            LocalTime start = LocalTime.of(startHour, 0);
            LocalTime end = start.plusHours(duration);
            for (Room room : rooms) {
                try {
                    if (bookingService.isRoomAvailable(room.getRoomId(), date, start, end)) {
                        scheduleResultPane.getChildren().add(buildSlotButton(room, date, start, end));
                        slots++;
                    }
                } catch (BusinessRuleException e) {
                    // Ruangan berubah menjadi MAINTENANCE di sela pencarian: lewati ruangan ini.
                } catch (RuntimeException e) {
                    scheduleResultPane.getChildren().clear();
                    UiSupport.error(quickBookingMessageLabel, UiSupport.messageOf(e));
                    return;
                }
            }
        }

        if (slots == 0) {
            UiSupport.info(quickBookingMessageLabel,
                    "Tidak ada jadwal tersedia untuk pilihan tersebut. Coba tanggal, durasi, atau jenis studio lain.");
            return;
        }
        scheduleResultTitleLabel.setText("Jadwal tersedia — " + DateTimeUtil.formatDate(date) + " · "
                + duration + " jam · " + type);
        scheduleResultBox.setManaged(true);
        scheduleResultBox.setVisible(true);
    }

    private Button buildSlotButton(Room room, LocalDate date, LocalTime start, LocalTime end) {
        Button button = new Button(DateTimeUtil.formatTime(start) + " – " + DateTimeUtil.formatTime(end)
                + "  ·  " + room.getRoomName());
        button.getStyleClass().addAll("outline-button", "compact-button");
        button.setOnAction(e -> onSlotSelected(new Navigator.BookingPrefill(date, start, end, room.getRoomId())));
        return button;
    }

    private void onSlotSelected(Navigator.BookingPrefill prefill) {
        if (session.isCustomer()) {
            Navigator.setPendingPrefill(prefill);
            Navigator.showCustomerDashboard();
        } else if (session.isAdmin()) {
            UiSupport.warning(quickBookingMessageLabel,
                    "Booking dibuat atas nama Customer. Login sebagai Customer untuk memesan jadwal.");
        } else {
            Navigator.setPendingPrefill(prefill); // D-GUI-01: booking wajib login/registrasi Customer
            Navigator.showLogin();
        }
    }
}
