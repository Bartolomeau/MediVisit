package pl.wspa.medivisit.dao;

import pl.wspa.medivisit.db.Database;
import pl.wspa.medivisit.model.Appointment;
import pl.wspa.medivisit.model.Doctor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppointmentDao {

    private static final String BASE_SELECT =
            "SELECT a.*, "
                    + "pu.first_name || ' ' || pu.last_name AS patient_name, "
                    + "du.first_name || ' ' || du.last_name AS doctor_name, "
                    + "s.name AS spec_name, d.room AS room "
                    + "FROM appointments a "
                    + "JOIN users pu ON pu.id = a.patient_id "
                    + "JOIN doctors d ON d.id = a.doctor_id "
                    + "JOIN users du ON du.id = d.user_id "
                    + "JOIN specializations s ON s.id = d.specialization_id ";

    public java.util.Optional<Appointment> findById(int id) {
        String sql = BASE_SELECT + "WHERE a.id = ?";
        List<Appointment> list = query(sql, ps -> ps.setInt(1, id));
        return list.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(list.get(0));
    }

    public List<Appointment> findByPatient(int patientId) {
        String sql = BASE_SELECT + "WHERE a.patient_id = ? ORDER BY a.date DESC, a.time DESC";
        return query(sql, ps -> ps.setInt(1, patientId));
    }

    public List<Appointment> findByDoctor(int doctorId) {
        String sql = BASE_SELECT + "WHERE a.doctor_id = ? ORDER BY a.date DESC, a.time DESC";
        return query(sql, ps -> ps.setInt(1, doctorId));
    }

    public List<Appointment> findByDoctorAndDate(int doctorId, String date) {
        String sql = BASE_SELECT + "WHERE a.doctor_id = ? AND a.date = ? ORDER BY a.time";
        return query(sql, ps -> {
            ps.setInt(1, doctorId);
            ps.setString(2, date);
        });
    }

    public List<Appointment> findAll() {
        String sql = BASE_SELECT + "ORDER BY a.date DESC, a.time DESC";
        return query(sql, ps -> { });
    }

    /**
     * Wolne godziny lekarza danego dnia: sloty z grafiku pomniejszone
     * o terminy juz zarezerwowane oraz godziny, ktore juz minely.
     */
    public List<String> findFreeSlots(Doctor doctor, LocalDate date) {
        List<String> taken = new ArrayList<>();
        String sql = "SELECT time FROM appointments WHERE doctor_id = ? AND date = ? AND status != 'ANULOWANA'";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, doctor.getId());
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    taken.add(rs.getString("time"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Blad odczytu terminow", e);
        }

        List<String> free = new ArrayList<>();
        LocalTime slot = LocalTime.parse(doctor.getWorkStart());
        LocalTime end = LocalTime.parse(doctor.getWorkEnd());
        boolean today = date.isEqual(LocalDate.now());
        while (slot.isBefore(end)) {
            String label = slot.toString();
            boolean inPast = today && !slot.isAfter(LocalTime.now());
            if (!taken.contains(label) && !inPast) {
                free.add(label);
            }
            slot = slot.plusMinutes(doctor.getSlotMinutes());
        }
        return free;
    }

    /** Zwraca false, jezeli termin zostal w miedzyczasie zajety. */
    public boolean book(int patientId, int doctorId, String date, String time, String reason) {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, date, time, reason) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setString(3, date);
            ps.setString(4, time);
            ps.setString(5, reason);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                return false;
            }
            throw new IllegalStateException("Blad rezerwacji wizyty", e);
        }
    }

    public void updateStatus(int id, String status) {
        String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Blad aktualizacji wizyty", e);
        }
    }

    public void complete(int id, String notes) {
        String sql = "UPDATE appointments SET status = 'ODBYTA', notes = ? WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, notes);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Blad zapisu zalecen", e);
        }
    }

    // ---- statystyki dla panelu administratora ----

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE status = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Blad statystyk", e);
        }
    }

    public Map<String, Integer> countBySpecialization() {
        String sql = "SELECT s.name, COUNT(a.id) AS cnt "
                + "FROM specializations s "
                + "LEFT JOIN doctors d ON d.specialization_id = s.id "
                + "LEFT JOIN appointments a ON a.doctor_id = d.id "
                + "GROUP BY s.id ORDER BY s.name";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getInt(2));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Blad statystyk", e);
        }
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Appointment> query(String sql, Binder binder) {
        List<Appointment> result = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Blad odczytu wizyt", e);
        }
    }

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getInt("id"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setDoctorId(rs.getInt("doctor_id"));
        a.setDate(rs.getString("date"));
        a.setTime(rs.getString("time"));
        a.setStatus(rs.getString("status"));
        a.setReason(rs.getString("reason"));
        a.setNotes(rs.getString("notes"));
        a.setCreatedAt(rs.getString("created_at"));
        a.setPatientName(rs.getString("patient_name"));
        a.setDoctorName(rs.getString("doctor_name"));
        a.setSpecializationName(rs.getString("spec_name"));
        a.setRoom(rs.getString("room"));
        return a;
    }
}
