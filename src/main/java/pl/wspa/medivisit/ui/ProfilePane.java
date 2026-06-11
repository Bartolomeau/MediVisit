package pl.wspa.medivisit.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.wspa.medivisit.dao.UserDao;
import pl.wspa.medivisit.model.User;
import pl.wspa.medivisit.util.PasswordUtil;
import pl.wspa.medivisit.util.Session;
import pl.wspa.medivisit.util.Validators;

/** Edycja danych profilu i zmiana hasla - wspolne dla wszystkich rol. */
public class ProfilePane {

    private final UserDao userDao = new UserDao();

    public Node build() {
        User user = Session.getUser();

        // --- dane osobowe ---
        TextField firstNameField = new TextField(user.getFirstName());
        TextField lastNameField = new TextField(user.getLastName());
        TextField phoneField = new TextField(user.getPhone() == null ? "" : user.getPhone());
        TextField emailField = new TextField(user.getEmail());
        emailField.setDisable(true);

        Label dataError = Ui.fieldError();

        Button saveButton = new Button("Zapisz zmiany");
        saveButton.getStyleClass().add("btn-primary");
        saveButton.setOnAction(e -> {
            Ui.hideFieldError(dataError);
            if (!Validators.isNotBlank(firstNameField.getText())
                    || !Validators.isNotBlank(lastNameField.getText())) {
                Ui.showFieldError(dataError, "Imię i nazwisko nie mogą być puste.");
                return;
            }
            if (!Validators.isValidPhone(phoneField.getText())) {
                Ui.showFieldError(dataError, "Telefon musi składać się z dokładnie 9 cyfr.");
                return;
            }
            userDao.updateProfile(user.getId(), firstNameField.getText(),
                    lastNameField.getText(), phoneField.getText().trim());
            user.setFirstName(firstNameField.getText().trim());
            user.setLastName(lastNameField.getText().trim());
            user.setPhone(phoneField.getText().trim());
            Ui.info("Profil zaktualizowany", "Twoje dane zostały zapisane.");
        });

        VBox dataCard = new VBox(12,
                Ui.heading("Dane profilu"),
                labeled("Imię", firstNameField),
                labeled("Nazwisko", lastNameField),
                labeled("Telefon", phoneField),
                labeled("E-mail (login)", emailField),
                dataError,
                saveButton);
        dataCard.getStyleClass().add("card");
        dataCard.setPrefWidth(380);

        // --- zmiana hasla ---
        PasswordField currentField = new PasswordField();
        PasswordField newField = new PasswordField();
        PasswordField confirmField = new PasswordField();

        Label passError = Ui.fieldError();

        Button changeButton = new Button("Zmień hasło");
        changeButton.getStyleClass().add("btn-accent");
        changeButton.setOnAction(e -> {
            Ui.hideFieldError(passError);
            if (!PasswordUtil.verify(currentField.getText(), user.getPasswordHash())) {
                Ui.showFieldError(passError, "Obecne hasło jest nieprawidłowe.");
                return;
            }
            if (!Validators.isValidPassword(newField.getText())) {
                Ui.showFieldError(passError, "Nowe hasło: min. 8 znaków, litera i cyfra.");
                return;
            }
            if (!newField.getText().equals(confirmField.getText())) {
                Ui.showFieldError(passError, "Nowe hasła nie są identyczne.");
                return;
            }
            String hash = PasswordUtil.hash(newField.getText());
            userDao.updatePassword(user.getId(), hash);
            user.setPasswordHash(hash);
            currentField.clear();
            newField.clear();
            confirmField.clear();
            Ui.info("Hasło zmienione", "Twoje hasło zostało zaktualizowane.");
        });

        VBox passCard = new VBox(12,
                Ui.heading("Zmiana hasła"),
                labeled("Obecne hasło", currentField),
                labeled("Nowe hasło", newField),
                labeled("Powtórz nowe hasło", confirmField),
                passError,
                changeButton);
        passCard.getStyleClass().add("card");
        passCard.setPrefWidth(380);

        HBox row = new HBox(24, dataCard, passCard);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private VBox labeled(String label, Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("subtitle");
        return new VBox(4, l, field);
    }
}
