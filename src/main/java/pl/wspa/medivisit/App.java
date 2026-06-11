package pl.wspa.medivisit;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.wspa.medivisit.db.Database;
import pl.wspa.medivisit.model.User;
import pl.wspa.medivisit.ui.AdminView;
import pl.wspa.medivisit.ui.DoctorView;
import pl.wspa.medivisit.ui.LoginView;
import pl.wspa.medivisit.ui.PatientView;
import pl.wspa.medivisit.ui.RegisterView;
import pl.wspa.medivisit.util.Session;

public class App extends Application {

    private static Stage primaryStage;
    private static Scene scene;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        Database.init();

        scene = new Scene(new LoginView().getRoot(), 1100, 720);
        scene.getStylesheets().add(App.class.getResource("/styles.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("MediVisit – system rejestracji wizyt");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.show();
    }

    private static void setRoot(Parent root) {
        scene.setRoot(root);
    }

    public static void showLogin() {
        Session.logout();
        setRoot(new LoginView().getRoot());
    }

    public static void showRegister() {
        setRoot(new RegisterView().getRoot());
    }

    /** Po zalogowaniu kieruje do panelu odpowiedniego dla roli. */
    public static void showDashboard() {
        User user = Session.getUser();
        switch (user.getRole()) {
            case User.ROLE_ADMIN -> setRoot(new AdminView().getRoot());
            case User.ROLE_DOCTOR -> setRoot(new DoctorView().getRoot());
            default -> setRoot(new PatientView().getRoot());
        }
    }

    public static Stage getStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
