package pl.wspa.medivisit.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.wspa.medivisit.dao.AppointmentDao;
import pl.wspa.medivisit.dao.DoctorDao;
import pl.wspa.medivisit.dao.SpecializationDao;
import pl.wspa.medivisit.model.Appointment;
import pl.wspa.medivisit.model.Doctor;
import pl.wspa.medivisit.model.Specialization;
import pl.wspa.medivisit.util.Session;

import java.time.LocalDate;
import java.util.List;

/** Panel pacjenta: umawianie wizyt, przeglad wizyt, profil. */
public class PatientView {

    private final SpecializationDao specializationDao = new SpecializationDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    public Parent getRoot() {
        DashboardLayout layout = new DashboardLayout("Panel pacjenta");
        layout.addItem("Umów wizytę", this::buildBooking);
        layout.addItem("Moje wizyty", this::buildMyVisits);
        layout.addItem("Mój profil", () -> new ProfilePane().build());
        return layout.getRoot();
    }

    // ===================== umawianie wizyty =====================

    private Node buildBooking() {
        ComboBox<Specialization> specBox = new ComboBox<>(
                FXCollections.observableArrayList(specializationDao.findAll()));
        specBox.setPromptText("wybierz specjalizację");
        specBox.setPrefWidth(260);

        ComboBox<Doctor> doctorBox = new ComboBox<>();
        doctorBox.setPromptText("wybierz lekarza");
        doctorBox.setPrefWidth(260);

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("wybierz datę");
        datePicker.setPrefWidth(260);
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now())
                        || date.getDayOfWeek().getValue() > 5); // przychodnia czynna pn-pt
            }
        });

        Label doctorInfo = new Label();
        doctorInfo.getStyleClass().add("subtitle");

        FlowPane slotsPane = new FlowPane(8, 8);
        slotsPane.setPrefWrapLength(560);
        ToggleGroup slotGroup = new ToggleGroup();
        Label slotsHint = new Label("Wybierz lekarza i datę, aby zobaczyć wolne godziny.");
        slotsHint.getStyleClass().add("subtitle");

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("powód wizyty (opcjonalnie)");
        reasonArea.setPrefRowCount(2);
        reasonArea.setWrapText(true);

        Label errorLabel = Ui.fieldError();

        Runnable refreshSlots = () -> {
            slotsPane.getChildren().clear();
            slotGroup.getToggles().clear();
            Doctor doctor = doctorBox.getValue();
            LocalDate date = datePicker.getValue();
            if (doctor == null || date == null) {
                slotsHint.setText("Wybierz lekarza i datę, aby zobaczyć wolne godziny.");
                return;
            }
            List<String> slots = appointmentDao.findFreeSlots(doctor, date);
            if (slots.isEmpty()) {
                slotsHint.setText("Brak wolnych terminów w wybranym dniu – wybierz inną datę.");
                return;
            }
            slotsHint.setText("Wolne godziny (" + doctor.getWorkStart() + "–" + doctor.getWorkEnd() + "):");
            for (String slot : slots) {
                ToggleButton button = new ToggleButton(slot);
                button.getStyleClass().add("slot-button");
                button.setToggleGroup(slotGroup);
                slotsPane.getChildren().add(button);
            }
        };

        specBox.setOnAction(e -> {
            Specialization spec = specBox.getValue();
            doctorBox.getItems().clear();
            if (spec != null) {
                doctorBox.getItems().addAll(doctorDao.findBySpecialization(spec.getId()));
            }
            doctorInfo.setText("");
            refreshSlots.run();
        });
        doctorBox.setOnAction(e -> {
            Doctor doctor = doctorBox.getValue();
            doctorInfo.setText(doctor == null ? "" : "Gabinet " + doctor.getRoom()
                    + ", przyjmuje " + doctor.getWorkStart() + "–" + doctor.getWorkEnd());
            refreshSlots.run();
        });
        datePicker.setOnAction(e -> refreshSlots.run());

        Button bookButton = new Button("Zarezerwuj wizytę");
        bookButton.getStyleClass().add("btn-primary");
        bookButton.setOnAction(e -> {
            Ui.hideFieldError(errorLabel);
            Doctor doctor = doctorBox.getValue();
            LocalDate date = datePicker.getValue();
            Toggle selected = slotGroup.getSelectedToggle();
            if (doctor == null || date == null || selected == null) {
                Ui.showFieldError(errorLabel, "Wybierz lekarza, datę oraz godzinę wizyty.");
                return;
            }
            String time = ((ToggleButton) selected).getText();
            boolean ok = appointmentDao.book(Session.getUser().getId(), doctor.getId(),
                    date.toString(), time, reasonArea.getText().trim());
            if (!ok) {
                Ui.showFieldError(errorLabel, "Ten termin został właśnie zajęty – wybierz inną godzinę.");
                refreshSlots.run();
                return;
            }
            Ui.info("Wizyta zarezerwowana",
                    "Termin: " + date + " godz. " + time + "\n"
                            + doctor.getFullName() + " (" + doctor.getSpecializationName() + ")\n"
                            + "Gabinet: " + doctor.getRoom());
            reasonArea.clear();
            refreshSlots.run();
        });

        HBox selectors = new HBox(14,
                labeled("Specjalizacja", specBox),
                labeled("Lekarz", doctorBox),
                labeled("Data wizyty", datePicker));
        selectors.setAlignment(Pos.BOTTOM_LEFT);

        VBox card = new VBox(16,
                Ui.heading("Umów wizytę"),
                selectors,
                doctorInfo,
                slotsHint,
                slotsPane,
                labeled("Powód wizyty", reasonArea),
                errorLabel,
                bookButton);
        card.getStyleClass().add("card");
        card.setMaxWidth(720);

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    // ===================== moje wizyty =====================

    private Node buildMyVisits() {
        TableView<Appointment> table = Tables.appointmentTable();
        table.getColumns().addAll(List.of(
                Tables.column("Data", Appointment::getDate, 95),
                Tables.column("Godzina", Appointment::getTime, 70),
                Tables.column("Lekarz", Appointment::getDoctorName, 160),
                Tables.column("Specjalizacja", Appointment::getSpecializationName, 130),
                Tables.column("Gabinet", Appointment::getRoom, 70),
                Tables.statusColumn(),
                Tables.column("Powód wizyty", Appointment::getReason, 180)));

        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(
                appointmentDao.findByPatient(Session.getUser().getId())));
        refresh.run();

        Button cancelButton = new Button("Anuluj wizytę");
        cancelButton.getStyleClass().add("btn-danger");
        cancelButton.setOnAction(e -> {
            Appointment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Brak wyboru", "Zaznacz wizytę, którą chcesz anulować.");
                return;
            }
            if (!Appointment.STATUS_PLANNED.equals(selected.getStatus()) || !selected.isUpcoming()) {
                Ui.error("Nie można anulować", "Anulować można tylko nadchodzące, zaplanowane wizyty.");
                return;
            }
            if (Ui.confirm("Anulowanie wizyty",
                    "Czy na pewno anulować wizytę " + selected.getDate() + " godz. " + selected.getTime() + "?")) {
                appointmentDao.updateStatus(selected.getId(), Appointment.STATUS_CANCELLED);
                refresh.run();
            }
        });

        Button notesButton = new Button("Pokaż zalecenia lekarza");
        notesButton.getStyleClass().add("btn-accent");
        notesButton.setOnAction(e -> {
            Appointment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || !Appointment.STATUS_DONE.equals(selected.getStatus())) {
                Ui.error("Brak zaleceń", "Zalecenia są dostępne tylko dla odbytych wizyt.");
                return;
            }
            String notes = selected.getNotes();
            Ui.info("Zalecenia z wizyty " + selected.getDate(),
                    (notes == null || notes.isBlank()) ? "Lekarz nie zapisał zaleceń." : notes);
        });

        HBox actions = new HBox(10, cancelButton, notesButton);

        VBox box = new VBox(14, Ui.heading("Moje wizyty"), table, actions);
        box.setPadding(new Insets(0));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private VBox labeled(String label, Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("subtitle");
        return new VBox(4, l, field);
    }
}
