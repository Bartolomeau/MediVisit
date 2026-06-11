package pl.wspa.medivisit.model;

public class User {

    public static final String ROLE_PATIENT = "PATIENT";
    public static final String ROLE_DOCTOR = "DOCTOR";
    public static final String ROLE_ADMIN = "ADMIN";

    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String passwordHash;
    private String role;
    private String createdAt;

    public User() {
    }

    public User(int id, String firstName, String lastName, String email,
                String phone, String passwordHash, String role, String createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getRoleDisplay() {
        switch (role) {
            case ROLE_ADMIN: return "Administrator";
            case ROLE_DOCTOR: return "Lekarz";
            default: return "Pacjent";
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
