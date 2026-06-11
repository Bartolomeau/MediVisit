package pl.wspa.medivisit.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.wspa.medivisit.dao.AppointmentDao;
import pl.wspa.medivisit.dao.DoctorDao;
import pl.wspa.medivisit.dao.SpecializationDao;
import pl.wspa.medivisit.dao.UserDao;
import pl.wspa.medivisit.model.Appointment;
import pl.wspa.medivisit.model.Doctor;
import pl.wspa.medivisit.model.Specialization;
import pl.wspa.medivisit.model.User;
import pl.wspa.medivisit.util.PasswordUtil;
import pl.wspa.medivisit.util.Session;
import pl.wspa.medivisit.util.Validators;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Panel administratora: statystyki, lekarze, specjalizacje, uzytkownicy, wizyty. */
public class AdminView {

    private final UserDao userDao = new UserDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final SpecializationDao specializationDao = new SpecializationDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    public Parent getRoot() {
        DashboardLayout layout = new DashboardLayout("Panel administratora");
        layout.addItem("Statystyki", this::buildStats);
        layout.addItem("Lekarze", this::buildDoctors);
        layout.addItem("Specjalizacje", this::buildSpecializations);
        layout.addItem("Użytkownicy", this::buildUsers);
        layout.addItem("Wizyty", this::buildAppointments);
        return layout.getRoot();
    }

    // ===================== statystyki =====================

    private Node buildStats() {
        int patients = (int) userDao.findAll().stream()
                .filter(u -> User.ROLE_PATIENT.equals(u.getRole())).count();
        int doctors = doctorDao.findAll().size();
        int planned = appointmentDao.countByStatus(Appointment.STATUS_PLANNED);
        int done = appointmentDao.countByStatus(Appointment.STATUS_DONE);
        int cancelled = appointmentDao.countByStatus(Appointment.STATUS_CANCELLED);

        HBox cards = new HBox(16,
                statCard(String.valueOf(patients), "pacjentów"),
                statCard(String.valueOf(doctors), "lekarzy"),
                statCard(String.valueOf(planned), "wizyt zaplanowanych"),
                statCard(String.valueOf(done), "wizyt odbytych"),
                statCard(String.valueOf(cancelled), "wizyt anulowanych"));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Specjalizacja");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Liczba wizyt");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Wizyty według specjalizacji");
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : appointmentDao.countBySpecialization().entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chart.getData().add(series);

        VBox box = new VBox(20, Ui.heading("Statystyki przychodni"), cards, chart);
        VBox.setVgrow(chart, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private VBox statCard(String value, String label) {
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        VBox card = new VBox(2, v, l);
        card.getStyleClass().add("stat-card");
        return card;
    }

    // ===================== lekarze =====================

    private Node buildDoctors() {
        TableView<Doctor> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Brak lekarzy"));
        table.getColumns().addAll(List.of(
                doctorColumn("Imię i nazwisko", d -> d.getFirstName() + " " + d.getLastName(), 160),
                doctorColumn("Specjalizacja", Doctor::getSpecializationName, 130),
                doctorColumn("E-mail", Doctor::getEmail, 180),
                doctorColumn("Gabinet", Doctor::getRoom, 60),
                doctorColumn("Godziny przyjęć", d -> d.getWorkStart() + "–" + d.getWorkEnd(), 110),
                doctorColumn("Długość wizyty", d -> d.getSlotMinutes() + " min", 90)));

        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(doctorDao.findAll()));
        refresh.run();

        Button addButton = new Button("Dodaj lekarza");
        addButton.getStyleClass().add("btn-primary");
        addButton.setOnAction(e -> {
            if (specializationDao.findAll().isEmpty()) {
                Ui.error("Brak specjalizacji", "Najpierw dodaj co najmniej jedną specjalizację.");
                return;
            }
            showDoctorDialog(null).ifPresent(unused -> refresh.run());
        });

        Button editButton = new Button("Edytuj");
        editButton.getStyleClass().add("btn-accent");
        editButton.setOnAction(e -> {
            Doctor selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Brak wyboru", "Zaznacz lekarza do edycji.");
                return;
            }
            showDoctorDialog(selected).ifPresent(unused -> refresh.run());
        });

        Button deleteButton = new Button("Usuń");
        deleteButton.getStyleClass().add("btn-danger");
        deleteButton.setOnAction(e -> {
            Doctor selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Brak wyboru", "Zaznacz lekarza do usunięcia.");
                return;
            }
            if (Ui.confirm("Usuwanie lekarza",
                    "Usunąć konto " + selected.getFullName() + "?\n"
                            + "Usunięte zostaną również wszystkie wizyty tego lekarza.")) {
                doctorDao.deleteWithUser(selected);
                refresh.run();
            }
        });

        HBox actions = new HBox(10, addButton, editButton, deleteButton);
        VBox box = new VBox(14, Ui.heading("Lekarze"), table, actions);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private TableColumn<Doctor, String> doctorColumn(String title,
                                                     java.util.function.Function<Doctor, String> getter,
                                                     int width) {
        TableColumn<Doctor, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                getter.apply(data.getValue()) == null ? "" : getter.apply(data.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    /** Dialog dodawania (doctor == null) lub edycji lekarza. */
    private Optional<Boolean> showDoctorDialog(Doctor doctor) {
        boolean isNew = doctor == null;

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nowy lekarz" : "Edycja lekarza");
        dialog.setHeaderText(isNew
                ? "Utworzone zostanie konto lekarza (rola DOCTOR)."
                : "Edycja: " + doctor.getFullName());

        TextField firstNameField = new TextField(isNew ? "" : doctor.getFirstName());
        TextField lastNameField = new TextField(isNew ? "" : doctor.getLastName());
        TextField emailField = new TextField(isNew ? "" : doctor.getEmail());
        emailField.setDisable(!isNew);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(isNew ? "hasło startowe" : "(bez zmian)");

        ComboBox<Specialization> specBox = new ComboBox<>(
                FXCollections.observableArrayList(specializationDao.findAll()));
        if (!isNew) {
            specBox.getItems().stream()
                    .filter(s -> s.getId() == doctor.getSpecializationId())
                    .findFirst().ifPresent(specBox::setValue);
        } else {
            specBox.getSelectionModel().selectFirst();
        }

        TextField roomField = new TextField(isNew ? "" : doctor.getRoom());
        ComboBox<String> startBox = new ComboBox<>(FXCollections.observableArrayList(hours()));
        startBox.setValue(isNew ? "08:00" : doctor.getWorkStart());
        ComboBox<String> endBox = new ComboBox<>(FXCollections.observableArrayList(hours()));
        endBox.setValue(isNew ? "16:00" : doctor.getWorkEnd());
        Spinner<Integer> slotSpinner = new Spinner<>(10, 60, isNew ? 30 : doctor.getSlotMinutes(), 5);

        Label errorLabel = Ui.fieldError();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        int row = 0;
        grid.addRow(row++, new Label("Imię:"), firstNameField);
        grid.addRow(row++, new Label("Nazwisko:"), lastNameField);
        grid.addRow(row++, new Label("E-mail (login):"), emailField);
        if (isNew) {
            grid.addRow(row++, new Label("Hasło:"), passwordField);
        }
        grid.addRow(row++, new Label("Specjalizacja:"), specBox);
        grid.addRow(row++, new Label("Gabinet:"), roomField);
        grid.addRow(row++, new Label("Przyjmuje od:"), startBox);
        grid.addRow(row++, new Label("Przyjmuje do:"), endBox);
        grid.addRow(row++, new Label("Wizyta (min):"), slotSpinner);
        grid.add(errorLabel, 0, row, 2, 1);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // walidacja bez zamykania okna przy bledzie
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Ui.hideFieldError(errorLabel);
            if (!Validators.isNotBlank(firstNameField.getText())
                    || !Validators.isNotBlank(lastNameField.getText())) {
                Ui.showFieldError(errorLabel, "Podaj imię i nazwisko.");
                event.consume();
                return;
            }
            if (specBox.getValue() == null) {
                Ui.showFieldError(errorLabel, "Wybierz specjalizację.");
                event.consume();
                return;
            }
            if (startBox.getValue().compareTo(endBox.getValue()) >= 0) {
                Ui.showFieldError(errorLabel, "Godzina rozpoczęcia musi być wcześniejsza niż zakończenia.");
                event.consume();
                return;
            }
            if (isNew) {
                if (!Validators.isValidEmail(emailField.getText())) {
                    Ui.showFieldError(errorLabel, "Podaj poprawny adres e-mail.");
                    event.consume();
                    return;
                }
                if (userDao.findByEmail(emailField.getText()).isPresent()) {
                    Ui.showFieldError(errorLabel, "Konto z tym adresem e-mail już istnieje.");
                    event.consume();
                    return;
                }
                if (!Validators.isValidPassword(passwordField.getText())) {
                    Ui.showFieldError(errorLabel, "Hasło: min. 8 znaków, litera i cyfra.");
                    event.consume();
                }
            }
        });

        dialog.setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }
            if (isNew) {
                User user = new User();
                user.setFirstName(firstNameField.getText());
                user.setLastName(lastNameField.getText());
                user.setEmail(emailField.getText());
                user.setPhone(null);
                user.setPasswordHash(PasswordUtil.hash(passwordField.getText()));
                user.setRole(User.ROLE_DOCTOR);
                int userId = userDao.insert(user);
                doctorDao.insert(userId, specBox.getValue().getId(), roomField.getText().trim(),
                        startBox.getValue(), endBox.getValue(), slotSpinner.getValue());
            } else {
                userDao.updateProfile(doctor.getUserId(), firstNameField.getText(),
                        lastNameField.getText(), null);
                doctorDao.update(doctor.getId(), specBox.getValue().getId(), roomField.getText().trim(),
                        startBox.getValue(), endBox.getValue(), slotSpinner.getValue());
            }
            return Boolean.TRUE;
        });

        return dialog.showAndWait();
    }

    private List<String> hours() {
        List<String> result = new java.util.ArrayList<>();
        for (int h = 6; h <= 20; h++) {
            result.add(String.format("%02d:00", h));
            if (h < 20) {
                result.add(String.format("%02d:30", h));
            }
        }
        return result;
    }

    // ===================== specjalizacje =====================

    private Node buildSpecializations() {
        ListView<Specialization> list = new ListView<>();
        list.setPrefWidth(360);
        Runnable refresh = () -> list.setItems(
                FXCollections.observableArrayList(specializationDao.findAll()));
        refresh.run();

        Button addButton = new Button("Dodaj");
        addButton.getStyleClass().add("btn-primary");
        addButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Nowa specjalizacja");
            dialog.setHeaderText("Podaj nazwę specjalizacji:");
            dialog.showAndWait().ifPresent(name -> {
                if (!Validators.isNotBlank(name)) {
                    Ui.error("Błąd", "Nazwa nie może być pusta.");
                    return;
                }
                try {
                    specializationDao.insert(name);
                    refresh.run();
                } catch (IllegalStateException ex) {
                    Ui.error("Błąd", ex.getMessage());
                }
            });
        });

        Button renameButton = new Button("Zmień nazwę");
        renameButton.getStyleClass().add("btn-accent");
        renameButton.setOnAction(e -> {
            Specialization selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Brak wyboru", "Zaznacz specjalizację.");
                return;
            }
            TextInputDialog dialog = new TextInputDialog(selected.getName());
            dialog.setTitle("Zmiana nazwy");
            dialog.setHeaderText("Nowa nazwa specjalizacji:");
            dialog.showAndWait().ifPresent(name -> {
                if (Validators.isNotBlank(name)) {
                    specializationDao.update(selected.getId(), name);
                    refresh.run();
                }
            });
        });

        Button deleteButton = new Button("Usuń");
        deleteButton.getStyleClass().add("btn-danger");
        deleteButton.setOnAction(e -> {
            Specialization selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Brak wyboru", "Zaznacz specjalizację.");
                return;
            }
            if (!Ui.confirm("Usuwanie", "Usunąć specjalizację \"" + selected.getName() + "\"?")) {
                return;
            }
            if (!specializationDao.delete(selected.getId())) {
                Ui.error("Nie można usunąć",
                        "Do tej specjalizacji przypisani są lekarze. Najpierw zmień ich specjalizację.");
                return;
            }
            refresh.run();
        });

        HBox actions = new HBox(10, addButton, renameButton, deleteButton);
        VBox box = new VBox(14, Ui.heading("Specjalizacje"), list, actions);
        VBox.setVgrow(list, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    // ===================== uzytkownicy =====================

    private Node buildUsers() {
        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Brak użytkowników"));

        TableColumn<User, String> nameCol = new TableColumn<>("Imię i nazwisko");
        nameCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getFullName()));
        nameCol.setPrefWidth(170);
        TableColumn<User, String> emailCol = new TableColumn<>("E-mail");
        emailCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmail()));
        emailCol.setPrefWidth(200);
        TableColumn<User, String> phoneCol = new TableColumn<>("Telefon");
        phoneCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getPhone() == null ? "" : d.getValue().getPhone()));
        phoneCol.setPrefWidth(100);
        TableColumn<User, String> roleCol = new TableColumn<>("Rola");
        roleCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getRoleDisplay()));
        roleCol.setPrefWidth(110);
        TableColumn<User, String> createdCol = new TableColumn<>("Utworzono");
        createdCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getCreatedAt()));
        createdCol.setPrefWidth(140);
        table.getColumns().addAll(List.of(nameCol, emailCol, phoneCol, roleCol, createdCol));

        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(userDao.findAll()));
        refresh.run();

        Button deleteButton = new Button("Usuń konto");
        deleteButton.getStyleClass().add("btn-danger");
        deleteButton.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Brak wyboru", "Zaznacz użytkownika.");
                return;
            }
            if (selected.getId() == Session.getUser().getId()) {
                Ui.error("Nie można usunąć", "Nie możesz usunąć własnego konta.");
                return;
            }
            if (Ui.confirm("Usuwanie konta",
                    "Usunąć konto " + selected.getFullName() + " (" + selected.getEmail() + ")?\n"
                            + "Usunięte zostaną również powiązane wizyty.")) {
                userDao.delete(selected.getId());
                refresh.run();
            }
        });

        VBox box = new VBox(14, Ui.heading("Użytkownicy systemu"), table, new HBox(deleteButton));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    // ===================== wizyty =====================

    private Node buildAppointments() {
        TableView<Appointment> table = Tables.appointmentTable();
        table.getColumns().addAll(List.of(
                Tables.column("Data", Appointment::getDate, 90),
                Tables.column("Godzina", Appointment::getTime, 65),
                Tables.column("Pacjent", Appointment::getPatientName, 150),
                Tables.column("Lekarz", Appointment::getDoctorName, 150),
                Tables.column("Specjalizacja", Appointment::getSpecializationName, 120),
                Tables.statusColumn(),
                Tables.column("Powód wizyty", Appointment::getReason, 170)));

        ComboBox<String> statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Wszystkie", Appointment.STATUS_PLANNED, Appointment.STATUS_DONE, Appointment.STATUS_CANCELLED));
        statusFilter.setValue("Wszystkie");

        Runnable refresh = () -> {
            List<Appointment> all = appointmentDao.findAll();
            String filter = statusFilter.getValue();
            if (!"Wszystkie".equals(filter)) {
                all = all.stream().filter(a -> filter.equals(a.getStatus())).toList();
            }
            table.setItems(FXCollections.observableArrayList(all));
        };
        statusFilter.setOnAction(e -> refresh.run());
        refresh.run();

        HBox top = new HBox(10, new Label("Filtruj po statusie:"), statusFilter);
        VBox box = new VBox(14, Ui.heading("Wszystkie wizyty"), top, table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }
}
