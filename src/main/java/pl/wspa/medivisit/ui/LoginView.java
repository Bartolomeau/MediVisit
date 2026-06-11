package pl.wspa.medivisit.ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import pl.wspa.medivisit.App;
import pl.wspa.medivisit.dao.UserDao;
import pl.wspa.medivisit.model.User;
import pl.wspa.medivisit.util.PasswordUtil;
import pl.wspa.medivisit.util.Session;
import pl.wspa.medivisit.util.Validators;

import java.util.Optional;

public class LoginView {

    private final UserDao userDao = new UserDao();

    public Parent getRoot() {
        TextField emailField = new TextField();
        emailField.setPromptText("adres e-mail");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("hasło");

        Label errorLabel = Ui.fieldError();

        Button loginButton = new Button("Zaloguj się");
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> login(emailField.getText(), passwordField.getText(), errorLabel));

        Button registerLink = new Button("Nie masz konta? Zarejestruj się");
        registerLink.getStyleClass().add("btn-link");
        registerLink.setOnAction(e -> App.showRegister());

        Label subtitle = new Label("System rejestracji wizyt w przychodni");
        subtitle.getStyleClass().add("subtitle");

        Label demo = new Label("Konta demonstracyjne:  admin@medivisit.pl / Admin123!   "
                + "•   j.kowalski@medivisit.pl / Lekarz123!   •   pacjent@medivisit.pl / Pacjent123!");
        demo.getStyleClass().add("subtitle");
        demo.setStyle("-fx-font-size: 10.5px;");
        demo.setWrapText(true);

        VBox card = new VBox(14,
                Ui.logo(46, true),
                subtitle,
                Ui.heading("Logowanie"),
                emailField,
                passwordField,
                errorLabel,
                loginButton,
                registerLink,
                demo);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);

        StackPane root = new StackPane(card);
        root.setAlignment(Pos.CENTER);
        return root;
    }

    private void login(String email, String password, Label errorLabel) {
        Ui.hideFieldError(errorLabel);
        if (!Validators.isNotBlank(email) || !Validators.isNotBlank(password)) {
            Ui.showFieldError(errorLabel, "Podaj adres e-mail i hasło.");
            return;
        }
        Optional<User> user = userDao.findByEmail(email);
        if (user.isEmpty() || !PasswordUtil.verify(password, user.get().getPasswordHash())) {
            Ui.showFieldError(errorLabel, "Nieprawidłowy adres e-mail lub hasło.");
            return;
        }
        Session.setUser(user.get());
        App.showDashboard();
    }
}
