package pl.wspa.medivisit.dao;

import pl.wspa.medivisit.db.Database;
import pl.wspa.medivisit.model.Specialization;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SpecializationDao {

    public List<Specialization> findAll() {
        String sql = "SELECT * FROM specializations ORDER BY name";
        List<Specialization> result = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Specialization(rs.getInt("id"), rs.getString("name")));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Blad odczytu specjalizacji", e);
        }
    }

    public void insert(String name) {
        String sql = "INSERT INTO specializations (name) VALUES (?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Specjalizacja o tej nazwie juz istnieje", e);
        }
    }

    public void update(int id, String name) {
        String sql = "UPDATE specializations SET name = ? WHERE id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Blad aktualizacji specjalizacji", e);
        }
    }

    public boolean delete(int id) {
        try {
            try (PreparedStatement check = Database.get().prepareStatement(
                    "SELECT COUNT(*) FROM doctors WHERE specialization_id = ?")) {
                check.setInt(1, id);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false;
                    }
                }
            }
            try (PreparedStatement ps = Database.get().prepareStatement(
                    "DELETE FROM specializations WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Blad usuwania specjalizacji", e);
        }
    }
}
