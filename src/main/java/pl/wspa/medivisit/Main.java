package pl.wspa.medivisit;

/**
 * Klasa startowa. Nie dziedziczy po javafx.application.Application,
 * dzieki czemu aplikacje mozna uruchomic takze z pliku JAR
 * (java -jar medivisit-1.0.0.jar) bez konfigurowania modulow JavaFX.
 */
public final class Main {

    public static void main(String[] args) {
        App.main(args);
    }
}
