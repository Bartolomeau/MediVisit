package pl.wspa.medivisit.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import pl.wspa.medivisit.model.Appointment;

import java.util.function.Function;

/** Pomocnicze metody do budowania tabel wizyt. */
public final class Tables {

    private Tables() {
    }

    public static TableColumn<Appointment, String> column(String title, Function<Appointment, String> getter,
                                                          int prefWidth) {
        TableColumn<Appointment, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                getter.apply(data.getValue()) == null ? "" : getter.apply(data.getValue())));
        col.setPrefWidth(prefWidth);
        return col;
    }

    /** Kolumna statusu z kolorowaniem wartosci. */
    public static TableColumn<Appointment, String> statusColumn() {
        TableColumn<Appointment, String> col = column("Status", Appointment::getStatus, 110);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-planned", "status-done", "status-cancelled");
                if (empty || status == null) {
                    setText(null);
                    return;
                }
                setText(status);
                switch (status) {
                    case Appointment.STATUS_PLANNED -> getStyleClass().add("status-planned");
                    case Appointment.STATUS_DONE -> getStyleClass().add("status-done");
                    case Appointment.STATUS_CANCELLED -> getStyleClass().add("status-cancelled");
                }
            }
        });
        return col;
    }

    public static TableView<Appointment> appointmentTable() {
        TableView<Appointment> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new javafx.scene.control.Label("Brak wizyt do wyświetlenia"));
        return table;
    }
}
