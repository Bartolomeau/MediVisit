package pl.wspa.medivisit.dao;

import pl.wspa.medivisit.db.Database;
import pl.wspa.medivisit.model.Doctor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorDao {

    private static final String BASE_SELECT =
            "SELECT d.id, d.user_id, d.specialization_id, d.room, d.work_start, d.work_end, d.slot_minutes, "
                    + "u.first_name, u.last_name, u.email, s.name AS spec_name "
                    + "FROM doctors d "
                    + "JOIN users u ON u.id = d.user_id "
                    + "JOIN specializations s ON s.id = d.specialization_id ";

    public List<Doctor> findAll() {
        String sql = BASE_SELECT + "ORDER BY u.last_name, u.first_name";
        return query(sql, ps -> { });
    }

    public List<Doctor> findBySpecialization(int specializationId) {
        String sql = BASE_SELECT + "WHERE d.specialization_id = ? ORDER BY u.last_name";
        return query(sql, ps -> ps.setInt(1, specializationId));
    }

    public Optional<Doctor> findByUserId(int userId) {
        String sql = BASE_SELECT + "WHERE d.user_id = ?";
        List<Doctor> list = query(sql, ps -> ps.setInt(1, userId));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void insert(int userId, int specializationId, String room,
                       String workStart, String workEnd, int slotMinutes) {
        String sql = "INSERT INTO doctors (user_id, specialization_id, room, work_start, work_end, slot_minutes) "
                + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, specializationId);
            ps.setString(3, room);
            ps.setString(4, workStart);
            ps.setString(5, workEnd);
            ps.setInt(6, slotMinutes);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Blad zapisu lekarza", e);
        }
    }

    public void update(int id, int specializationId, String room,
                       String workStart, String workEnd, int slotMinutes) {
        String sql = "UPDATE doctors SET specialization_id = ?, room = ?, work_start = ?, work_end = ?, "
                + "slot_minutes = ? WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, specializationId);
            ps.setString(2, room);
            ps.setString(3, workStart);
            ps.setString(4, workEnd);
            ps.setInt(5, slotMinutes);
            ps.setInt(6, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Blad aktualizacji lekarza", e);
        }
    }

    /** Usuniecie konta uzytkownika kaskadowo usuwa wpis lekarza i jego wizyty. */
    public void deleteWithUser(Doctor doctor) {
        new UserDao().delete(doctor.getUserId());
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Doctor> query(String sql, Binder binder) {
        List<Doctor> result = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Blad odczytu lekarzy", e);
        }
    }

    private Doctor map(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getInt("id"));
        d.setUserId(rs.getInt("user_id"));
        d.setSpecializationId(rs.getInt("specialization_id"));
        d.setRoom(rs.getString("room"));
        d.setWorkStart(rs.getString("work_start"));
        d.setWorkEnd(rs.getString("work_end"));
        d.setSlotMinutes(rs.getInt("slot_minutes"));
        d.setFirstName(rs.getString("first_name"));
        d.setLastName(rs.getString("last_name"));
        d.setEmail(rs.getString("email"));
        d.setSpecializationName(rs.getString("spec_name"));
        return d;
    }
}
