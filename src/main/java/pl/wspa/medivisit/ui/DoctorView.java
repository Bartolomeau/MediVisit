package pl.wspa.medivisit.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.wspa.medivisit.dao.AppointmentDao;
import pl.wspa.medivisit.dao.DoctorDao;
import pl.wspa.medivisit.model.Appointment;
import pl.wspa.medivisit.model.Doctor;
import pl.wspa.medivisit.util.Session;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Panel lekarza: grafik dnia, historia wizyt, profil. */
public class DoctorView {

    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final Doctor doctor;

    public DoctorView() {
        this.doctor = new DoctorDao().findByUserId(Session.getUser().getId())
                .orElseThrow(() -> new IllegalStateException("Brak profilu lekarza dla zalogowanego konta"));
    }

    public Parent getRoot() {
        DashboardLayout layout = new DashboardLayout("Panel lekarza – " + doctor.getSpecializationName());
        layout.addItem("Grafik dnia", this::buildDaySchedule);
        layout.addItem("Wszystkie wizyty", this::buildAllVisits);
        layout.addItem("Mój profil", () -> new ProfilePane().build());
        return layout.getRoot();
    }

    // ===================== grafik dnia =====================

    private Node buildDaySchedule() {
        DatePicker datePicker = new DatePicker(LocalDate.now());

        TableView<Appointment> table = Tables.appointmentTable();
        table.getColumns().addAll(List.of(
                Tables.column("Godzina", Appointment::getTime, 70),
                Tables.column("Pacjent", Appointment::getPatientName, 160),
                Tables.statusColumn(),
                Tables.column("Powód wizyty", Appointment::getReason, 220),
                Tables.column("Zalecenia", Appointment::getNotes, 220)));

        Runnable refresh = () -> {
            LocalDate date = datePicker.getValue();
            table.setItems(FXCollections.observableArrayList(
                    date == null ? List.of() : appointmentDao.findByDoctorAndDate(doctor.getId(), date.toString())));
        };
        datePicker.setOnAction(e -> refresh.run());
        refresh.run();

        Button completeButton = new Button("Oznacz jako odbytą + zalecenia");
        completeButton.getStyleClass().add("btn-accent");
        completeButton.setOnAction(e -> {
            Appointment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Brak wyboru", "Zaznacz wizytę na liście.");
                return;
            }
            if (!Appointment.STATUS_PLANNED.equals(selected.getStatus())) {
                Ui.error("Nieprawidłowy status", "Tylko zaplanowane wizyty można oznaczyć jako odbyte.");
                return;
            }
            askForNotes(selected).ifPresent(notes -> {
                appointmentDao.complete(selected.getId(), notes);
                refresh.run();
            });
        });

        Button cancelButton = new Button("Anuluj wizytę");
        cancelButton.getStyleClass().add("btn-danger");
        cancelButton.setOnAction(e -> {
            Appointment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || !Appointment.STATUS_PLANNED.equals(selected.getStatus())) {
                Ui.error("Nie można anulować", "Zaznacz zaplanowaną wizytę.");
                return;
            }
            if (Ui.confirm("Anulowanie wizyty",
                    "Anulować wizytę pacjenta " + selected.getPatientName()
                            + " (" + selected.getDate() + " " + selected.getTime() + ")?")) {
                appointmentDao.updateStatus(selected.getId(), Appointment.STATUS_CANCELLED);
                refresh.run();
            }
        });

        Label info = new Label("Gabinet " + doctor.getRoom() + "  •  godziny przyjęć "
                + doctor.getWorkStart() + "–" + doctor.getWorkEnd()
                + "  •  wizyta co " + doctor.getSlotMinutes() + " min");
        info.getStyleClass().add("subtitle");

        HBox top = new HBox(14, new Label("Dzień:"), datePicker);
        top.setPadding(new Insets(0, 0, 4, 0));
        HBox actions = new HBox(10, completeButton, cancelButton);

        VBox box = new VBox(12, Ui.heading("Grafik dnia"), info, top, table, actions);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private Optional<String> askForNotes(Appointment appointment) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Zalecenia po wizycie");
        dialog.setHeaderText("Pacjent: " + appointment.getPatientName()
                + " (" + appointment.getDate() + " " + appointment.getTime() + ")");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("zalecenia dla pacjenta, przepisane leki itd.");
        notesArea.setPrefRowCount(6);
        notesArea.setWrapText(true);
        dialog.getDialogPane().setContent(notesArea);

        ButtonType saveType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == saveType ? notesArea.getText().trim() : null);
        return dialog.showAndWait();
    }

    // ===================== wszystkie wizyty =====================

    private Node buildAllVisits() {
        TableView<Appointment> table = Tables.appointmentTable();
        table.getColumns().addAll(List.of(
                Tables.column("Data", Appointment::getDate, 95),
                Tables.column("Godzina", Appointment::getTime, 70),
                Tables.column("Pacjent", Appointment::getPatientName, 160),
                Tables.statusColumn(),
                Tables.column("Powód wizyty", Appointment::getReason, 200),
                Tables.column("Zalecenia", Appointment::getNotes, 200)));
        table.setItems(FXCollections.observableArrayList(appointmentDao.findByDoctor(doctor.getId())));

        VBox box = new VBox(14, Ui.heading("Wszystkie moje wizyty"), table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }
}
