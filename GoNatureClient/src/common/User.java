package common;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String role; // 'PARK_EMPLOYEE', 'PARK_MANAGER', 'DEPARTMENT_MANAGER', 'SERVICE_REPRESENTATIVE', 'GUIDE'
    private String email;
    private Integer assignedParkId; // Nullable
    private boolean isLoggedIn;

    public User() {}

    public User(String username, String password, String firstName, String lastName, String role, String email, Integer assignedParkId) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.email = email;
        this.assignedParkId = assignedParkId;
        this.isLoggedIn = false;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getAssignedParkId() { return assignedParkId; }
    public void setAssignedParkId(Integer assignedParkId) { this.assignedParkId = assignedParkId; }

    public boolean isLoggedIn() { return isLoggedIn; }
    public void setLoggedIn(boolean loggedIn) { isLoggedIn = loggedIn; }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role='" + role + '\'' +
                ", assignedParkId=" + assignedParkId +
                '}';
    }
}
