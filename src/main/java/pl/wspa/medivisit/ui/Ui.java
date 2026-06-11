package pl.wspa.medivisit.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.Optional;

/** Wspolne elementy interfejsu. */
public final class Ui {

    public static final Color PRIMARY = Color.web("#1976d2");
    public static final Color ACCENT = Color.web("#26a69a");

    private Ui() {
    }

    /** Logo aplikacji: krzyz medyczny w kole + nazwa. */
    public static Node logo(double size, boolean withText) {
        double r = size / 2;
        Circle circle = new Circle(r, r, r, PRIMARY);
        Circle inner = new Circle(r, r, r * 0.82, Color.WHITE);
        double arm = size * 0.5;
        double thick = size * 0.18;
        Rectangle vertical = new Rectangle(r - thick / 2, r - arm / 2, thick, arm);
        Rectangle horizontal = new Rectangle(r - arm / 2, r - thick / 2, arm, thick);
        vertical.setFill(ACCENT);
        horizontal.setFill(ACCENT);
        vertical.setArcWidth(thick * 0.6);
        vertical.setArcHeight(thick * 0.6);
        horizontal.setArcWidth(thick * 0.6);
        horizontal.setArcHeight(thick * 0.6);
        Group mark = new Group(circle, inner, vertical, horizontal);

        if (!withText) {
            return mark;
        }
        Text medi = new Text("Medi");
        medi.setFont(Font.font("Segoe UI", FontWeight.BOLD, size * 0.55));
        medi.setFill(PRIMARY);
        Text visit = new Text("Visit");
        visit.setFont(Font.font("Segoe UI", FontWeight.BOLD, size * 0.55));
        visit.setFill(ACCENT);
        HBox text = new HBox(medi, visit);
        text.setAlignment(Pos.CENTER);

        HBox box = new HBox(size * 0.25, mark, text);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("heading");
        return label;
    }

    public static Label fieldError() {
        Label label = new Label();
        label.getStyleClass().add("field-error");
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    public static void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void hideFieldError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    public static void info(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MediVisit");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("MediVisit");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirm(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("MediVisit");
        alert.setHeaderText(header);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static Region spacer() {
        Region region = new Region();
        VBox.setVgrow(region, javafx.scene.layout.Priority.ALWAYS);
        return region;
    }
}
