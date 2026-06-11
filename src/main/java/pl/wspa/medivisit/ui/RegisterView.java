package pl.wspa.medivisit.ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import pl.wspa.medivisit.App;
import pl.wspa.medivisit.dao.UserDao;
import pl.wspa.medivisit.model.User;
import pl.wspa.medivisit.util.PasswordUtil;
import pl.wspa.medivisit.util.Validators;

public class RegisterView {

    private final UserDao userDao = new UserDao();

    public Parent getRoot() {
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("imię");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("nazwisko");
        TextField emailField = new TextField();
        emailField.setPromptText("adres e-mail");
        TextField phoneField = new TextField();
        phoneField.setPromptText("telefon (9 cyfr)");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("hasło (min. 8 znaków, litera i cyfra)");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("powtórz hasło");

        Label errorLabel = Ui.fieldError();

        Button registerButton = new Button("Załóż konto");
        registerButton.getStyleClass().add("btn-accent");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setDefaultButton(true);
        registerButton.setOnAction(e -> register(
                firstNameField.getText(), lastNameField.getText(), emailField.getText(),
                phoneField.getText(), passwordField.getText(), confirmField.getText(), errorLabel));

        Button backLink = new Button("Masz już konto? Zaloguj się");
        backLink.getStyleClass().add("btn-link");
        backLink.setOnAction(e -> App.showLogin());

        HBox names = new HBox(10, firstNameField, lastNameField);
        firstNameField.setPrefWidth(180);
        lastNameField.setPrefWidth(180);

        VBox card = new VBox(14,
                Ui.logo(46, true),
                Ui.heading("Rejestracja pacjenta"),
                names,
                emailField,
                phoneField,
                passwordField,
                confirmField,
                errorLabel,
                registerButton,
                backLink);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);

        StackPane root = new StackPane(card);
        root.setAlignment(Pos.CENTER);
        return root;
    }

    private void register(String firstName, String lastName, String email,
                          String phone, String password, String confirm, Label errorLabel) {
        Ui.hideFieldError(errorLabel);

        if (!Validators.isNotBlank(firstName) || !Validators.isNotBlank(lastName)) {
            Ui.showFieldError(errorLabel, "Podaj imię i nazwisko.");
            return;
        }
        if (!Validators.isValidEmail(email)) {
            Ui.showFieldError(errorLabel, "Podaj poprawny adres e-mail.");
            return;
        }
        if (!Validators.isValidPhone(phone)) {
            Ui.showFieldError(errorLabel, "Telefon musi składać się z dokładnie 9 cyfr.");
            return;
        }
        if (!Validators.isValidPassword(password)) {
            Ui.showFieldError(errorLabel, "Hasło: min. 8 znaków, co najmniej jedna litera i cyfra.");
            return;
        }
        if (!password.equals(confirm)) {
            Ui.showFieldError(errorLabel, "Hasła nie są identyczne.");
            return;
        }
        if (userDao.findByEmail(email).isPresent()) {
            Ui.showFieldError(errorLabel, "Konto z tym adresem e-mail już istnieje.");
            return;
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone.trim());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(User.ROLE_PATIENT);
        userDao.insert(user);

        Ui.info("Konto utworzone", "Możesz się teraz zalogować na swoje konto.");
        App.showLogin();
    }
}
