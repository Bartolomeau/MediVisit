package pl.wspa.medivisit.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import pl.wspa.medivisit.App;
import pl.wspa.medivisit.util.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Wspolny uklad paneli: menu boczne + obszar tresci. */
public class DashboardLayout {

    private final BorderPane root = new BorderPane();
    private final VBox navBox = new VBox(4);
    private final List<Button> navButtons = new ArrayList<>();

    public DashboardLayout(String roleTitle) {
        Label appName = new Label("MediVisit");
        appName.getStyleClass().add("sidebar-title");
        Label role = new Label(roleTitle);

        VBox header = new VBox(2, appName, role);
        header.setPadding(new Insets(0, 0, 18, 8));

        Label userName = new Label(Session.getUser().getFullName());
        userName.setStyle("-fx-font-weight: bold;");
        Label userEmail = new Label(Session.getUser().getEmail());
        userEmail.setStyle("-fx-font-size: 11px;");

        Button logout = new Button("Wyloguj się");
        logout.getStyleClass().add("btn-danger");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> App.showLogin());

        VBox footer = new VBox(6, userName, userEmail, logout);
        footer.setPadding(new Insets(18, 8, 0, 8));

        VBox sidebar = new VBox(header, navBox, Ui.spacer(), footer);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        root.setLeft(sidebar);
    }

    /** Dodaje pozycje menu; pierwsza dodana jest otwierana od razu. */
    public void addItem(String label, Supplier<Node> contentSupplier) {
        Button button = new Button(label);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> {
            navButtons.forEach(b -> b.getStyleClass().remove("nav-button-active"));
            button.getStyleClass().add("nav-button-active");
            setContent(contentSupplier.get());
        });
        navButtons.add(button);
        navBox.getChildren().add(button);
        if (navButtons.size() == 1) {
            button.fire();
        }
    }

    private void setContent(Node content) {
        BorderPane wrapper = new BorderPane(content);
        wrapper.setPadding(new Insets(26));
        BorderPane.setAlignment(content, Pos.TOP_LEFT);
        root.setCenter(wrapper);
    }

    public BorderPane getRoot() {
        return root;
    }
}
