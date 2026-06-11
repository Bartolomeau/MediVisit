package pl.wspa.medivisit.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {

    public static final String STATUS_PLANNED = "ZAPLANOWANA";
    public static final String STATUS_DONE = "ODBYTA";
    public static final String STATUS_CANCELLED = "ANULOWANA";

    private int id;
    private int patientId;
    private int doctorId;
    private String date;   // YYYY-MM-DD
    private String time;   // HH:MM
    private String status;
    private String reason;
    private String notes;
    private String createdAt;

    // pola pomocnicze z JOIN-ow
    private String patientName;
    private String doctorName;
    private String specializationName;
    private String room;

    public boolean isUpcoming() {
        LocalDate d = LocalDate.parse(date);
        LocalDate today = LocalDate.now();
        if (d.isAfter(today)) return true;
        if (d.isEqual(today)) return LocalTime.parse(time).isAfter(LocalTime.now());
        return false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getSpecializationName() { return specializationName; }
    public void setSpecializationName(String specializationName) { this.specializationName = specializationName; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
}
