package pl.wspa.medivisit.model;

public class Doctor {

    private int id;
    private int userId;
    private String firstName;
    private String lastName;
    private String email;
    private int specializationId;
    private String specializationName;
    private String room;
    private String workStart;
    private String workEnd;
    private int slotMinutes;

    public String getFullName() {
        return "lek. " + firstName + " " + lastName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getSpecializationId() { return specializationId; }
    public void setSpecializationId(int specializationId) { this.specializationId = specializationId; }
    public String getSpecializationName() { return specializationName; }
    public void setSpecializationName(String specializationName) { this.specializationName = specializationName; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getWorkStart() { return workStart; }
    public void setWorkStart(String workStart) { this.workStart = workStart; }
    public String getWorkEnd() { return workEnd; }
    public void setWorkEnd(String workEnd) { this.workEnd = workEnd; }
    public int getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(int slotMinutes) { this.slotMinutes = slotMinutes; }

    @Override
    public String toString() {
        return getFullName() + " (gab. " + (room == null ? "-" : room) + ")";
    }
}
