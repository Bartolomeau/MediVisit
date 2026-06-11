package pl.wspa.medivisit.db;

import pl.wspa.medivisit.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Zarzadza polaczeniem z baza SQLite. Przy pierwszym uruchomieniu
 * tworzy strukture tabel (schema.sql) i wypelnia baze danymi startowymi.
 */
public final class Database {

    private static final String DB_URL = "jdbc:sqlite:medivisit.db";
    private static Connection connection;

    private Database() {
    }

    public static synchronized Connection get() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON");
                }
            }
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Nie mozna polaczyc sie z baza danych", e);
        }
    }

    public static void init() {
        runSchema();
        seedIfEmpty();
    }

    private static void runSchema() {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("Brak pliku schema.sql w zasobach");
            }
            String sql = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            try (Statement st = get().createStatement()) {
                for (String part : sql.split(";")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        st.execute(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Blad inicjalizacji struktury bazy danych", e);
        }
    }

    /** Dane startowe: konto administratora, specjalizacje i przykladowi lekarze. */
    private static void seedIfEmpty() {
        try {
            try (Statement st = get().createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }

            insertUser("Anna", "Nowak", "admin@medivisit.pl", "510100200", "Admin123!", "ADMIN");

            String[] specs = {"Lekarz rodzinny", "Kardiolog", "Dermatolog", "Ortopeda", "Laryngolog"};
            try (PreparedStatement ps = get().prepareStatement(
                    "INSERT INTO specializations (name) VALUES (?)")) {
                for (String s : specs) {
                    ps.setString(1, s);
                    ps.executeUpdate();
                }
            }

            int d1 = insertUser("Jan", "Kowalski", "j.kowalski@medivisit.pl", "511200300", "Lekarz123!", "DOCTOR");
            int d2 = insertUser("Maria", "Wisniewska", "m.wisniewska@medivisit.pl", "512300400", "Lekarz123!", "DOCTOR");
            int d3 = insertUser("Piotr", "Zielinski", "p.zielinski@medivisit.pl", "513400500", "Lekarz123!", "DOCTOR");

            insertDoctor(d1, 1, "101", "08:00", "16:00", 30);
            insertDoctor(d2, 2, "205", "09:00", "15:00", 30);
            insertDoctor(d3, 3, "112", "10:00", "18:00", 20);

            insertUser("Tomasz", "Mazur", "pacjent@medivisit.pl", "514500600", "Pacjent123!", "PATIENT");
        } catch (SQLException e) {
            throw new IllegalStateException("Blad podczas wypelniania bazy danymi startowymi", e);
        }
    }

    private static int insertUser(String firstName, String lastName, String email,
                                  String phone, String plainPassword, String role) throws SQLException {
        try (PreparedStatement ps = get().prepareStatement(
                "INSERT INTO users (first_name, last_name, email, phone, password_hash, role) VALUES (?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setString(5, PasswordUtil.hash(plainPassword));
            ps.setString(6, role);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static void insertDoctor(int userId, int specId, String room,
                                     String workStart, String workEnd, int slotMinutes) throws SQLException {
        try (PreparedStatement ps = get().prepareStatement(
                "INSERT INTO doctors (user_id, specialization_id, room, work_start, work_end, slot_minutes) "
                        + "VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, userId);
            ps.setInt(2, specId);
            ps.setString(3, room);
            ps.setString(4, workStart);
            ps.setString(5, workEnd);
            ps.setInt(6, slotMinutes);
            ps.executeUpdate();
        }
    }
}
