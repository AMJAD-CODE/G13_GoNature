package common;
import java.io.Serializable;

/**
 * Represents a system user in the GoNature application.
 *
 * <p>A user is an employee, manager, guide, or service representative
 * who can access the system according to their assigned role. The class
 * stores authentication details, personal information, role information,
 * assigned park details, and login status.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
public class User implements Serializable {
	private static final long serialVersionUID = 1L;
	private String username;
	private String password;
	private String firstName;
	private String lastName;
	private String role; // 'PARK_EMPLOYEE', 'PARK_MANAGER', 'DEPARTMENT_MANAGER', 'SERVICE_REPRESENTATIVE', 'GUIDE'
	private String email;
	private Integer assignedParkId; // Nullable => meaning this field is optinal 
	private boolean isLoggedIn;
	/**
	 * Creates an empty user object.
	 */
	public User() {}

	/**
	 * Creates a user with the specified details.
	 *
	 * @param username the user's username
	 * @param password the user's password
	 * @param firstName the user's first name
	 * @param lastName the user's last name
	 * @param role the user's role in the system
	 * @param email the user's email address
	 * @param assignedParkId the identifier of the park assigned to the user,
	 *                       or null if no park is assigned
	 */
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

	/**
	 * Creates a user with the specified details, including login status.
	 *
	 * @param username the user's username
	 * @param password the user's password
	 * @param firstName the user's first name
	 * @param lastName the user's last name
	 * @param role the user's role in the system
	 * @param email the user's email address
	 * @param assignedParkId the identifier of the park assigned to the user,
	 *                       or null if no park is assigned
	 * @param isLoggedIn the login status
	 */
	public User(String username, String password, String firstName, String lastName, String role, String email, Integer assignedParkId, boolean isLoggedIn) {
		this.username = username;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;
		this.email = email;
		this.assignedParkId = assignedParkId;
		this.isLoggedIn = isLoggedIn;
	}



	/**
	 * Returns the username.
	 *
	 * @return the username
	 */
	public String getUsername() { return username; }

	/**
	 * Sets the username.
	 *
	 * @param username the username to set
	 */
	public void setUsername(String username) { this.username = username; }

	/**
	 * Returns the password.
	 *
	 * @return the password
	 */
	public String getPassword() { return password; }

	/**
	 * Sets the password.
	 *
	 * @param password the password to set
	 */
	public void setPassword(String password) { this.password = password; }

	/**
	 * Returns the user's first name.
	 *
	 * @return the first name
	 */
	public String getFirstName() { return firstName; }

	/**
	 * Sets the user's first name.
	 *
	 * @param firstName the first name to set
	 */
	public void setFirstName(String firstName) { this.firstName = firstName; }

	/**
	 * Returns the user's last name.
	 *
	 * @return the last name
	 */
	public String getLastName() { return lastName; }

	/**
	 * Sets the user's last name.
	 *
	 * @param lastName the last name to set
	 */
	public void setLastName(String lastName) { this.lastName = lastName; }

	/**
	 * Returns the user's role.
	 *
	 * @return the role
	 */
	public String getRole() { return role; }

	/**
	 * Sets the user's role.
	 *
	 * @param role the role to set
	 */
	public void setRole(String role) { this.role = role; }

	/**
	 * Returns the user's email address.
	 *
	 * @return the email address
	 */
	public String getEmail() { return email; }

	/**
	 * Sets the user's email address.
	 *
	 * @param email the email address to set
	 */
	public void setEmail(String email) { this.email = email; }

	/**
	 * Returns the identifier of the assigned park.
	 *
	 * @return the assigned park identifier, or null if none exists
	 */
	public Integer getAssignedParkId() { return assignedParkId; }

	/**
	 * Sets the assigned park identifier.
	 *
	 * @param assignedParkId the assigned park identifier
	 */
	public void setAssignedParkId(Integer assignedParkId) {
		this.assignedParkId = assignedParkId;
	}

	/**
	 * Returns whether the user is currently logged in.
	 *
	 * @return true if the user is logged in; false otherwise
	 */
	public boolean isLoggedIn() { return isLoggedIn; }

	/**
	 * Sets the user's login status.
	 *
	 * @param loggedIn the login status to set
	 */
	public void setLoggedIn(boolean loggedIn) { isLoggedIn = loggedIn; }

	/**
	 * Returns a string representation of the user.
	 *
	 * @return a formatted user description
	 */
	@Override
	public String toString() {
		return "User{...}";
	}
}
