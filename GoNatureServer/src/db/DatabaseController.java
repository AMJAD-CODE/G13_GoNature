package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.Park;
import common.Reservation;
import common.Subscriber;
import common.User;
import common.Promotion;
/**
 * Provides all database operations for the GoNature system.
 * This class manages database connectivity and performs CRUD
 * operations related to users, parks, subscribers, reservations,
 * promotions, occupancy tracking and reports.
 *
 * @author Rahaf
 * @version 1.0
 */
public class DatabaseController {

	private Connection conn;


	/**
	 * Establishes a connection to the MySQL database.
	 *
	 * @param host database server host
	 * @param dbName database name
	 * @param user database username
	 * @param password database password
	 * @return true if connection succeeded, false otherwise
	 */
	public boolean connect(String host, String dbName, String user, String password) {
		String url = "jdbc:mysql://" + host + "/" + dbName + "?serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
		try {
			// Load driver explicitly just in case
			Class.forName("com.mysql.cj.jdbc.Driver");//loads the MySQL driver — the code that knows how to actually speak MySQL's protocol

			conn = DriverManager.getConnection(url, user, password);//opens the socket
			System.out.println("DatabaseController: connected to " + url);

			// On successful connection, reset all user login statuses
			resetLogins();
			//we need this incase the server crashed and some users are logged in, we need to reset is_logged_in so they can log in again
			return true;
		} catch (Exception e) {
			System.out.println("ERROR in DatabaseController.connect: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Resets the login status of all users in the system.
	 */
	private void resetLogins() {
		String sql = "UPDATE users SET is_logged_in = FALSE";
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			System.out.println("Error resetting logins: " + e.getMessage());
		}
	}

	/**
	 * Closes the database connection and resets all login statuses.
	 */
	public void disconnect() {
		try {
			if (conn != null && !conn.isClosed()) {
				resetLogins(); // Reset on normal exit
				conn.close();
				System.out.println("DatabaseController: connection closed.");
			}
		} catch (SQLException e) {
			System.out.println("ERROR in DatabaseController.disconnect: " + e.getMessage());
		}
	}

	/**
	 * Checks whether the database connection is active.
	 *
	 * @return true if connected, false otherwise
	 */
	public boolean isConnected() {
		try {
			return conn != null && !conn.isClosed();
		} catch (SQLException e) {
			return false;
		}
	}

	// ==========================================
	// USER OPERATIONS
	// ==========================================

	/**
	 * Authenticates a user using username and password.
	 *
	 * @param username user's username
	 * @param password user's password
	 * @return User object if authentication succeeds, otherwise null
	 */
	public User loginUser(String username, String password) {
		String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, username);
			ps.setString(2, password);
			//The username and password are not glued into the SQL string 
			//they're sent as bound parameters via setString
			//this is how we prevent sql injection

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {//it moves the cursor to the next row, and it returns true if there was one, false if there wasn't
					boolean alreadyLoggedIn = rs.getBoolean("is_logged_in");
					if (alreadyLoggedIn) {
						return null; // Prevents simultaneous logins
					}

					// Mark as logged in
					//a single login actually touches the DB twice 
					//one SELECT to read and one UPDATE to claim in setLoginStatus
					setLoginStatus(username, true);

					Integer parkId = rs.getInt("assigned_park_id");//getInt returns 0 for a SQL NULL
					if (rs.wasNull()) parkId = null;// so we use wasNull to check if it was null

					return new User(
							rs.getString("username"),
							rs.getString("password"),
							rs.getString("first_name"),
							rs.getString("last_name"),
							rs.getString("role"),
							rs.getString("email"),
							parkId
							);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error logging in user: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Updates a user's login status.
	 *
	 * @param username username to update
	 * @param isLoggedIn new login status
	 */
	public void setLoginStatus(String username, boolean isLoggedIn){
		String sql = "UPDATE users SET is_logged_in = ? WHERE username = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setBoolean(1, isLoggedIn);
			ps.setString(2, username);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Error updating login status: " + e.getMessage());
		}
	}

	// ==========================================
	// PARK OPERATIONS
	// ==========================================

	/**
	 * Retrieves a park by its identifier.
	 *
	 * @param parkId park identifier
	 * @return matching Park object or null if not found
	 */
	public Park getPark(int parkId){
		String sql = "SELECT * FROM parks WHERE park_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					Park p = new Park(
							rs.getInt("park_id"),
							rs.getString("park_name"),
							rs.getInt("max_quota"),
							rs.getInt("current_quota"),
							rs.getInt("reserved_gap"),
							rs.getInt("stay_duration")
							);
					p.setPendingMaxQuota(rs.getInt("pending_max_quota"));
					if (rs.wasNull()) p.setPendingMaxQuota(null);

					p.setPendingReservedGap(rs.getInt("pending_reserved_gap"));
					if (rs.wasNull()) p.setPendingReservedGap(null);

					p.setPendingStayDuration(rs.getInt("pending_stay_duration"));
					if (rs.wasNull()) p.setPendingStayDuration(null);

					p.setPendingChangesStatus(rs.getString("pending_changes_status"));
					return p;
				}
			}
		} catch (SQLException e) {
			System.out.println("Error fetching park: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Retrieves all parks from the database.
	 *
	 * @return list of all parks
	 */
	public List<Park> getAllParks() {
		List<Park> list = new ArrayList<>();
		String sql = "SELECT * FROM parks";
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				Park p = new Park(
						rs.getInt("park_id"),
						rs.getString("park_name"),
						rs.getInt("max_quota"),
						rs.getInt("current_quota"),
						rs.getInt("reserved_gap"),
						rs.getInt("stay_duration")
						);
				p.setPendingMaxQuota(rs.getInt("pending_max_quota"));
				if (rs.wasNull()) p.setPendingMaxQuota(null);

				p.setPendingReservedGap(rs.getInt("pending_reserved_gap"));
				if (rs.wasNull()) p.setPendingReservedGap(null);

				p.setPendingStayDuration(rs.getInt("pending_stay_duration"));
				if (rs.wasNull()) p.setPendingStayDuration(null);

				p.setPendingChangesStatus(rs.getString("pending_changes_status"));
				list.add(p);
			}
		} catch (SQLException e) {
			System.out.println("Error fetching all parks: " + e.getMessage());
		}
		return list;
	}

	/**
	 * Submits new park parameters for approval.
	 *
	 * @param park park containing pending values
	 * @return true if update succeeded, false otherwise
	 */
	public boolean updateParkParameters(Park park) {
		String sql = "UPDATE parks SET pending_max_quota = ?, pending_reserved_gap = ?, pending_stay_duration = ?, pending_changes_status = 'PENDING_APPROVAL' WHERE park_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			if (park.getPendingMaxQuota() != null) ps.setInt(1, park.getPendingMaxQuota());
			else ps.setNull(1, java.sql.Types.INTEGER);

			if (park.getPendingReservedGap() != null) ps.setInt(2, park.getPendingReservedGap());
			else ps.setNull(2, java.sql.Types.INTEGER);

			if (park.getPendingStayDuration() != null) ps.setInt(3, park.getPendingStayDuration());
			else ps.setNull(3, java.sql.Types.INTEGER);

			ps.setInt(4, park.getParkId());
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			System.out.println("Error updating park parameters: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Approves or rejects pending park parameter changes.
	 *
	 * @param parkId park identifier
	 * @param approve true to approve, false to reject
	 * @return true if operation succeeded, false otherwise
	 */
	public boolean approveParkParameters(int parkId, boolean approve) {
		String sql;
		if (approve) {
			sql = "UPDATE parks SET max_quota = IFNULL(pending_max_quota, max_quota), " +
					"current_quota = IFNULL(pending_max_quota, current_quota), " +
					"reserved_gap = IFNULL(pending_reserved_gap, reserved_gap), " +
					"stay_duration = IFNULL(pending_stay_duration, stay_duration), " +
					"pending_max_quota = NULL, pending_reserved_gap = NULL, pending_stay_duration = NULL, " +
					"pending_changes_status = 'NONE' WHERE park_id = ?";
		} else {
			sql = "UPDATE parks SET pending_max_quota = NULL, pending_reserved_gap = NULL, pending_stay_duration = NULL, " +
					"pending_changes_status = 'NONE' WHERE park_id = ?";
		}
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			System.out.println("Error approving/rejecting park parameters: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Updates the details of an existing subscriber.
	 *
	 * @param sub the subscriber object with updated information
	 * @return true if updated successfully; false otherwise
	 */
	public boolean updateSubscriberDetails(Subscriber sub) {
		String sql = "UPDATE subscribers SET email = ?, phone_number = ?, family_size = ?, credit_card_number = ? WHERE subscriber_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, sub.getEmail());
			ps.setString(2, sub.getPhoneNumber());
			ps.setInt(3, sub.getFamilySize());
			ps.setString(4, sub.getCreditCardNumber());
			ps.setInt(5, sub.getSubscriberId());
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			System.out.println("Error updating subscriber details: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Checks if a visitor has an active reservation scheduled for today.
	 *
	 * @param visitorId the visitor's identifier
	 * @param simNow the current simulated system time
	 * @return true if an active reservation exists for today; false otherwise
	 */
	public boolean hasActiveReservationForToday(String visitorId, Timestamp simNow) {
		String sql = "SELECT COUNT(*) FROM reservations WHERE visitor_id = ? AND status IN ('CONFIRMED', 'PENDING_CONFIRMATION', 'ACTIVE') AND DATE(visit_date_time) = DATE(?)";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, visitorId);
			ps.setTimestamp(2, simNow);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1) > 0;
				}
			}
		} catch (SQLException e) {
			System.out.println("Error checking active reservations for today: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Retrieves a system user by their username.
	 *
	 * @param username the username to query
	 * @return the User object, or null if not found
	 */
	public User getUserByUsername(String username) {
		String sql = "SELECT * FROM users WHERE username = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					Integer parkId = rs.getInt("assigned_park_id");
					if (rs.wasNull()) parkId = null;
					return new User(
						rs.getString("username"),
						"", // Hide password
						rs.getString("first_name"),
						rs.getString("last_name"),
						rs.getString("role"),
						rs.getString("email"),
						parkId,
						rs.getBoolean("is_logged_in")
					);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error fetching user: " + e.getMessage());
		}
		return null;
	}

	// ==========================================
	// SUBSCRIBER & GUIDE REGISTER
	// ==========================================

	/**
	 * Registers a new subscriber.
	 *
	 * @param sub subscriber information
	 * @return registered subscriber with generated identifier, or null if failed
	 */
	public Subscriber registerSubscriber(Subscriber sub) {
		String sql = "INSERT INTO subscribers (id_number, first_name, last_name, email, phone_number, family_size, credit_card_number) VALUES (?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, sub.getIdNumber());
			ps.setString(2, sub.getFirstName());
			ps.setString(3, sub.getLastName());
			ps.setString(4, sub.getEmail());
			ps.setString(5, sub.getPhoneNumber());
			ps.setInt(6, sub.getFamilySize());
			ps.setString(7, sub.getCreditCardNumber());

			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					sub.setSubscriberId(rs.getInt(1));
					return sub;
				}
			}
		} catch (SQLException e) {
			System.out.println("Error registering subscriber: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Retrieves a subscriber by ID number or subscriber number.
	 *
	 * @param idOrSubNumber subscriber ID or subscription number
	 * @return matching Subscriber object or null if not found
	 */
	public Subscriber getSubscriberById(String idOrSubNumber) {
		String sql = "SELECT * FROM subscribers WHERE id_number = ? OR subscriber_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, idOrSubNumber);
			int subId = -1;
			try {
				subId = Integer.parseInt(idOrSubNumber);
			} catch (NumberFormatException ignored) {}
			ps.setInt(2, subId);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return new Subscriber(
							rs.getInt("subscriber_id"),
							rs.getString("id_number"),
							rs.getString("first_name"),
							rs.getString("last_name"),
							rs.getString("email"),
							rs.getString("phone_number"),
							rs.getInt("family_size"),
							rs.getString("credit_card_number")
							);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error fetching subscriber: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Registers a new guide user.
	 *
	 * @param guide guide information
	 * @return true if registration succeeded, false otherwise
	 */
	public boolean registerGuide(User guide) {
		String sql = "INSERT INTO users (username, password, first_name, last_name, role, email, assigned_park_id) VALUES (?, ?, ?, ?, 'GUIDE', ?, NULL)";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, guide.getUsername());
			ps.setString(2, guide.getPassword());
			ps.setString(3, guide.getFirstName());
			ps.setString(4, guide.getLastName());
			ps.setString(5, guide.getEmail());
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			System.out.println("Error registering guide: " + e.getMessage());
			return false;
		}
	}

	// ==========================================
	// RESERVATION LOGIC
	// ==========================================

	/**
	 * Checks whether sufficient capacity is available for a reservation.
	 *
	 * @param parkId park identifier
	 * @param visitTime requested visit time
	 * @param numberOfVisitors number of visitors
	 * @return true if capacity is available, false otherwise
	 */
	public boolean checkCapacityAvailable(int parkId, Timestamp visitTime, int numberOfVisitors) {
		Park park = getPark(parkId);
		if (park == null) return false;

		int durationHours = park.getStayDuration();
		long visitStart = visitTime.getTime();
		long visitEnd = visitStart + (durationHours * 60 * 60 * 1000);

		// Fetch reservations for the same park on the same calendar day
		String sql = "SELECT number_of_visitors, visit_date_time, status, stay_duration FROM reservations r " +
				"JOIN parks p ON r.park_id = p.park_id " +
				"WHERE r.park_id = ? AND DATE(visit_date_time) = DATE(?) AND r.status IN ('CONFIRMED', 'ACTIVE', 'PENDING_CONFIRMATION')";

		int maxAllowed = park.getCurrentQuota() - park.getReservedGap();

		// We will test occupancy hour-by-hour within the stay window.
		// Let's divide the stay duration into 30-minute steps
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			ps.setTimestamp(2, visitTime);

			List<OverlapInfo> existing = new ArrayList<>();
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Timestamp startTs = rs.getTimestamp("visit_date_time");
					int num = rs.getInt("number_of_visitors");
					int stayHrs = rs.getInt("stay_duration");
					existing.add(new OverlapInfo(startTs.getTime(), startTs.getTime() + (stayHrs * 60L * 60 * 1000), num));
				}
			}

			// Check every 15 minutes of the proposed slot
			for (long t = visitStart; t < visitEnd; t += 15L * 60 * 1000) {
				int currentLoad = 0;
				for (OverlapInfo r : existing) {
					if (t >= r.start && t < r.end) {
						currentLoad += r.count;
					}
				}
				if (currentLoad + numberOfVisitors > maxAllowed) {
					return false; // Exceeds quota in at least one window
				}
			}
			return true;
		} catch (SQLException e) {
			System.out.println("Error checking capacity availability: " + e.getMessage());
			return false;
		}
	}
	/**
	 * Represents an existing reservation time interval and the
	 * number of visitors associated with it.
	 * <p>
	 * Used during capacity calculations to determine overlapping
	 * reservations and occupancy levels within a park.
	 * </p>
	 */
	private static class OverlapInfo {
		long start;
		long end;
		int count;
		OverlapInfo(long start, long end, int count) {
			this.start = start;
			this.end = end;
			this.count = count;
		}
	}

	/**
	 * Calculates hourly occupancy levels for a specific day.
	 *
	 * @param parkId park identifier
	 * @param date requested date
	 * @return map containing occupancy by hour
	 */
	public Map<Integer, Integer> getOccupancyForDay(int parkId, Date date) {
		Map<Integer, Integer> loads = new HashMap<>();
		Park park = getPark(parkId);
		if (park == null) return loads;

		// Initialize 08:00 to 20:00 with 0
		for (int h = 8; h <= 20; h++) {
			loads.put(h, 0);
		}

		String sql = "SELECT number_of_visitors, visit_date_time, stay_duration FROM reservations r " +
				"JOIN parks p ON r.park_id = p.park_id " +
				"WHERE r.park_id = ? AND DATE(visit_date_time) = ? AND r.status IN ('CONFIRMED', 'ACTIVE', 'PENDING_CONFIRMATION')";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			ps.setDate(2, date);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Timestamp start = rs.getTimestamp("visit_date_time");
					int visitors = rs.getInt("number_of_visitors");
					int duration = rs.getInt("stay_duration");

					java.util.Calendar cal = java.util.Calendar.getInstance();
					cal.setTime(start);
					int startHour = cal.get(java.util.Calendar.HOUR_OF_DAY);

					for (int h = startHour; h < startHour + duration; h++) {
						if (h >= 8 && h <= 20) {
							loads.put(h, loads.get(h) + visitors);
						}
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Error fetching occupancy for day: " + e.getMessage());
		}
		return loads;
	}

	/**
	 * Creates a new reservation.
	 *
	 * @param res reservation details
	 * @return created reservation with generated ID, or null if creation failed
	 */
	public Reservation createReservation(Reservation res) {
		String sql = "INSERT INTO reservations (visitor_id, park_id, visit_date_time, number_of_visitors, email, phone_number, reservation_type, status, payment_status, price, created_at) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, res.getVisitorId());
			ps.setInt(2, res.getParkId());
			ps.setTimestamp(3, res.getVisitDateTime());
			ps.setInt(4, res.getNumberOfVisitors());
			ps.setString(5, res.getEmail());
			ps.setString(6, res.getPhoneNumber());
			ps.setString(7, res.getReservationType());
			ps.setString(8, res.getStatus());
			ps.setString(9, res.getPaymentStatus());
			ps.setDouble(10, res.getPrice());
			ps.setTimestamp(11, res.getCreatedAt());
			//sql sees we didn't give reservation_id a value and generates it by itself

			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					res.setReservationId(rs.getInt(1));// column 1 = the new id
					return res;
				}
			}
		} catch (SQLException e) {
			System.out.println("Error creating reservation: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Retrieves all reservations belonging to a visitor.
	 *
	 * @param visitorId visitor identifier
	 * @return list of reservations
	 */
	public List<Reservation> getReservationsByVisitorId(String visitorId) {
		List<Reservation> list = new ArrayList<>();
		Subscriber sub = getSubscriberById(visitorId);
		String sql;
		if (sub != null) {
			sql = "SELECT r.*, p.park_name FROM reservations r JOIN parks p ON r.park_id = p.park_id WHERE r.visitor_id = ? OR r.visitor_id = ?";
		} else {
			sql = "SELECT r.*, p.park_name FROM reservations r JOIN parks p ON r.park_id = p.park_id WHERE r.visitor_id = ?";
		}
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			if (sub != null) {
				ps.setString(1, sub.getIdNumber());
				ps.setString(2, String.valueOf(sub.getSubscriberId()));
			} else {
				ps.setString(1, visitorId);
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {//moves through the rows we got back
					Reservation r = new Reservation(
							rs.getInt("reservation_id"),
							rs.getString("visitor_id"),
							rs.getInt("park_id"),
							rs.getString("park_name"),
							rs.getTimestamp("visit_date_time"),
							rs.getInt("number_of_visitors"),
							rs.getString("email"),
							rs.getString("phone_number"),
							rs.getString("reservation_type"),
							rs.getString("status"),
							rs.getString("payment_status"),
							rs.getDouble("price"),
							rs.getTimestamp("created_at")
							);
					r.setReminderSentTime(rs.getTimestamp("reminder_sent_time"));
					r.setSpotPromotedTime(rs.getTimestamp("spot_promoted_time"));
					r.setActualEntryTime(rs.getTimestamp("actual_entry_time"));
					r.setActualExitTime(rs.getTimestamp("actual_exit_time"));
					//we dont know if these events happend already so we cant pass values
					//in a constructor and we use setters to set them later

					//values that only appear later as the visit plays out, so they're attached afterward
					list.add(r);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error loading reservations by ID: " + e.getMessage());
		}
		return list;
	}

	/**
	 * Retrieves a reservation by its identifier.
	 *
	 * @param resId reservation identifier
	 * @return matching reservation or null if not found
	 */
	public Reservation getReservation(int resId) {
		String sql = "SELECT r.*, p.park_name FROM reservations r JOIN parks p ON r.park_id = p.park_id WHERE r.reservation_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, resId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					Reservation r = new Reservation(
							rs.getInt("reservation_id"),
							rs.getString("visitor_id"),
							rs.getInt("park_id"),
							rs.getString("park_name"),
							rs.getTimestamp("visit_date_time"),
							rs.getInt("number_of_visitors"),
							rs.getString("email"),
							rs.getString("phone_number"),
							rs.getString("reservation_type"),
							rs.getString("status"),
							rs.getString("payment_status"),
							rs.getDouble("price"),
							rs.getTimestamp("created_at")
							);
					r.setReminderSentTime(rs.getTimestamp("reminder_sent_time"));
					r.setSpotPromotedTime(rs.getTimestamp("spot_promoted_time"));
					r.setActualEntryTime(rs.getTimestamp("actual_entry_time"));
					r.setActualExitTime(rs.getTimestamp("actual_exit_time"));
					return r;
				}
			}
		} catch (SQLException e) {
			System.out.println("Error fetching single reservation: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Updates the status of a reservation.
	 *
	 * @param resId reservation identifier
	 * @param status new status
	 * @param ts timestamp associated with the status change
	 * @return true if update succeeded, false otherwise
	 */
	public boolean updateReservationStatus(int resId, String status, Timestamp ts) {
		String sql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";
		// If status changes to CANCELLED, set cancelled_at timestamp
		if ("CANCELLED".equals(status)) {
			sql = "UPDATE reservations SET status = ?, cancelled_at = ? WHERE reservation_id = ?";
		} else if ("ACTIVE".equals(status)) {
			sql = "UPDATE reservations SET status = ?, actual_entry_time = ? WHERE reservation_id = ?";
		} else if ("COMPLETED".equals(status)) {
			sql = "UPDATE reservations SET status = ?, actual_exit_time = ? WHERE reservation_id = ?";
		}
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, status);
			if ("CANCELLED".equals(status) || "ACTIVE".equals(status) || "COMPLETED".equals(status)) {
				ps.setTimestamp(2, ts);
				ps.setInt(3, resId);
			} else {
				ps.setInt(2, resId);
			}
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			System.out.println("Error updating reservation status: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Marks a reservation as a no-show and cancels it.
	 * <p>
	 * Updates the reservation status to CANCELLED, sets the
	 * no-show flag to true, and records the cancellation time.
	 * This method is typically invoked when visitors fail to
	 * arrive within the allowed entry period.
	 * </p>
	 *
	 * @param resId the reservation ID.
	 * @param ts the timestamp at which the reservation was
	 *           marked as a no-show.
	 * @return true if the reservation was successfully updated;
	 *         false otherwise.
	 */
	public boolean flagNoShow(int resId, Timestamp ts) {
		String sql = "UPDATE reservations SET status = 'CANCELLED', is_no_show = TRUE, cancelled_at = ? WHERE reservation_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, ts);
			ps.setInt(2, resId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			System.out.println("Error flagging no show: " + e.getMessage());
			return false;
		}
	}

	
	/**
	 * Records the time at which a reservation reminder was sent.
	 * <p>
	 * Updates the reminder_sent_time field for the specified
	 * reservation. This information is used by the scheduler
	 * to prevent duplicate reminder notifications.
	 * </p>
	 *
	 * @param resId the reservation ID.
	 * @param ts the timestamp when the reminder was sent.
	 */
	public void setReminderSent(int resId, Timestamp ts) {
		String sql = "UPDATE reservations SET reminder_sent_time = ? WHERE reservation_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, ts);
			ps.setInt(2, resId);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Error writing reminder: " + e.getMessage());
		}
	}

	/**
	 * Records the time at which a waiting-list reservation
	 * was promoted to an available spot.
	 * <p>
	 * Updates the spot_promoted_time field for the specified
	 * reservation. This information is used by the scheduler
	 * to track promotion deadlines and waiting-list activity.
	 * </p>
	 *
	 * @param resId the reservation ID.
	 * @param ts the timestamp when the reservation was promoted.
	 */
	public void setSpotPromoted(int resId, Timestamp ts) {
		String sql = "UPDATE reservations SET spot_promoted_time = ? WHERE reservation_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, ts);
			ps.setInt(2, resId);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Error writing spot promotion: " + e.getMessage());
		}
	}

	// ==========================================
	// REAL-TIME VISITOR TRACKING (ENTRANCE)
	// ==========================================

	/**
	 * Calculates the current number of visitors inside a park.
	 *
	 * @param parkId park identifier
	 * @return current occupancy count
	 */
	public int getParkCurrentOccupancy(int parkId) {
		// Counts actual active visitors right now (ACTIVE status)
		String sql = "SELECT SUM(number_of_visitors) FROM reservations WHERE park_id = ? AND status = 'ACTIVE'";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error calculating park current occupancy: " + e.getMessage());
		}
		return 0;
	}

	/**
	 * Stores an occupancy snapshot in the occupancy log.
	 *
	 * @param parkId park identifier
	 * @param currentVisitors current visitor count
	 * @param ts log timestamp
	 */
	public void logOccupancyChange(int parkId, int currentVisitors, Timestamp ts) {
		String sql = "INSERT INTO park_occupancy_log (park_id, log_time, current_visitors) VALUES (?, ?, ?)";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			ps.setTimestamp(2, ts);
			ps.setInt(3, currentVisitors);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Error logging occupancy: " + e.getMessage());
		}
	}

	// ==========================================
	// PROMOTIONS
	// ==========================================

	/**
	 * Creates a new promotion request.
	 *
	 * @param p promotion details
	 * @return true if creation succeeded, false otherwise
	 */
	public boolean createPromotion(Promotion p) {
		String sql = "INSERT INTO promotions (park_id, promotion_name, discount_percentage, start_date, end_date, status) VALUES (?, ?, ?, ?, ?, 'PENDING_APPROVAL')";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, p.getParkId());
			ps.setString(2, p.getPromotionName());
			ps.setDouble(3, p.getDiscountPercentage());
			ps.setDate(4, p.getStartDate());
			ps.setDate(5, p.getEndDate());
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			System.out.println("Error creating promotion: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Retrieves all pending promotions awaiting approval.
	 *
	 * @return list of pending promotions
	 */
	public List<Promotion> getPendingPromotions() {
		List<Promotion> list = new ArrayList<>();
		String sql = "SELECT * FROM promotions WHERE status = 'PENDING_APPROVAL'";
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new Promotion(
						rs.getInt("promotion_id"),
						rs.getInt("park_id"),
						rs.getString("promotion_name"),
						rs.getDouble("discount_percentage"),
						rs.getDate("start_date"),
						rs.getDate("end_date"),
						rs.getString("status")
						));
			}
		} catch (SQLException e) {
			System.out.println("Error fetching pending promotions: " + e.getMessage());
		}
		return list;
	}

	/**
	 * Approves or rejects a promotion request.
	 *
	 * @param promotionId promotion identifier
	 * @param approve true to approve, false to reject
	 * @return true if update succeeded, false otherwise
	 */
	public boolean approvePromotion(int promotionId, boolean approve) {
		String status = approve ? "APPROVED" : "REJECTED";
		String sql = "UPDATE promotions SET status = ? WHERE promotion_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, status);
			ps.setInt(2, promotionId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			System.out.println("Error approving promotion: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Retrieves the active promotion discount for a park.
	 *
	 * @param parkId park identifier
	 * @param time requested date and time
	 * @return discount percentage or 0 if no promotion exists
	 */
	public double getActivePromotionDiscount(int parkId, Timestamp time) {
		String sql = "SELECT discount_percentage FROM promotions WHERE park_id = ? AND status = 'APPROVED' AND ? BETWEEN start_date AND end_date LIMIT 1";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			ps.setDate(2, new Date(time.getTime()));
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getDouble("discount_percentage");
				}
			}
		} catch (SQLException e) {
			System.out.println("Error looking up promotion: " + e.getMessage());
		}
		return 0.0;
	}

	// ==========================================
	// BACKEND TICK SCHEDULER HELPER
	// ==========================================

	/**
	 * Retrieves all reservations that require scheduler monitoring.
	 * <p>
	 * Loads reservations whose status is:
	 * PENDING_CONFIRMATION, CONFIRMED, WAITING_LIST, or ACTIVE.
	 * These reservations are used by the automated scheduler to
	 * check time-based rules such as confirmation deadlines,
	 * waiting-list promotions, reminders, and visit completion.
	 * </p>
	 *
	 * @return a list of reservations that are currently subject
	 *         to scheduler processing; returns an empty list if
	 *         no matching reservations are found.
	 */
	public List<Reservation> getPendingTimerReservations() {
		// Load reservations that are PENDING_CONFIRMATION, CONFIRMED, WAITING_LIST, or ACTIVE
		// so the scheduler can check timestamps and execute rules.
		List<Reservation> list = new ArrayList<>();
		String sql = "SELECT r.*, p.park_name FROM reservations r JOIN parks p ON r.park_id = p.park_id WHERE r.status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'WAITING_LIST', 'ACTIVE')";
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				Reservation r = new Reservation(
						rs.getInt("reservation_id"),
						rs.getString("visitor_id"),
						rs.getInt("park_id"),
						rs.getString("park_name"),
						rs.getTimestamp("visit_date_time"),
						rs.getInt("number_of_visitors"),
						rs.getString("email"),
						rs.getString("phone_number"),
						rs.getString("reservation_type"),
						rs.getString("status"),
						rs.getString("payment_status"),
						rs.getDouble("price"),
						rs.getTimestamp("created_at")
						);
				r.setReminderSentTime(rs.getTimestamp("reminder_sent_time"));
				r.setSpotPromotedTime(rs.getTimestamp("spot_promoted_time"));
				r.setActualEntryTime(rs.getTimestamp("actual_entry_time"));
				r.setActualExitTime(rs.getTimestamp("actual_exit_time"));
				list.add(r);
			}
		} catch (SQLException e) {
			System.out.println("Error loading scheduler reservations: " + e.getMessage());
		}
		return list;
	}

	/**
	 * Retrieves all reservations currently on the waiting list for a specific park
	 * and visit time. The reservations are returned in ascending order of their
	 * creation time, so the reservation that has been waiting the longest appears first.
	 *
	 * @param parkId the unique identifier of the park
	 * @param visitDateTime the requested visit date and time
	 * @return a list of waiting-list reservations for the specified park and time slot,
	 *         ordered by creation date (oldest first). Returns an empty list if no
	 *         matching reservations are found or if an error occurs.
	 */
	public List<Reservation> getFirstInWaitingList(int parkId, Timestamp visitDateTime) {
		// Return waiting list users for the same time slot ordered by creation date
		List<Reservation> list = new ArrayList<>();
		//this returns the waiting list sorted by who has been waitiong the longest 
		String sql = "SELECT r.*, p.park_name FROM reservations r JOIN parks p ON r.park_id = p.park_id WHERE r.park_id = ? AND DATE(r.visit_date_time) = DATE(?) AND r.status = 'WAITING_LIST' ORDER BY r.created_at ASC";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			ps.setTimestamp(2, visitDateTime);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Reservation r = new Reservation(
							rs.getInt("reservation_id"),
							rs.getString("visitor_id"),
							rs.getInt("park_id"),
							rs.getString("park_name"),
							rs.getTimestamp("visit_date_time"),
							rs.getInt("number_of_visitors"),
							rs.getString("email"),
							rs.getString("phone_number"),
							rs.getString("reservation_type"),
							rs.getString("status"),
							rs.getString("payment_status"),
							rs.getDouble("price"),
							rs.getTimestamp("created_at")
							);
					list.add(r);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error fetching first waiting list reservation: " + e.getMessage());
		}
		return list;
	}

	// ==========================================
	// REPORTS GENERATION
	// ==========================================

	/**
	 * Generates a monthly visitor report grouped by reservation type.
	 *
	 * @param parkId park identifier
	 * @param month report month
	 * @param year report year
	 * @return report data grouped by reservation type
	 */
	public Map<String, Integer> getMonthlyVisitorReport(int parkId, int month, int year) {
		Map<String, Integer> data = new HashMap<>();
		data.put("INDIVIDUAL", 0);
		data.put("FAMILY_SUBSCRIBER", 0);
		data.put("ORGANIZED_GROUP", 0);
		//GROUP BY only returns groups that have rows. If no organized groups visited in March,
		//that type simply won't appear in the result

		//GROUP BY reservation_type collapses all the rows into one row per type, all the reservations become a single group for each kind
		String sql = "SELECT reservation_type, SUM(number_of_visitors) FROM reservations " +
				"WHERE park_id = ? AND MONTH(visit_date_time) = ? AND YEAR(visit_date_time) = ? AND status = 'COMPLETED' " +
				"GROUP BY reservation_type";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			ps.setInt(2, month);
			ps.setInt(3, year);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					data.put(rs.getString(1), rs.getInt(2));//(1) is type (2) is sum
				}
			}
		} catch (SQLException e) {
			System.out.println("Error in visitor report query: " + e.getMessage());
		}
		return data;
	}

	/**
	 * Usage report (times when park was NOT occupied).
	 * Returns daily average occupancies.
	 */
	public Map<Integer, Double> getMonthlyUsageReport(int parkId, int month, int year) {
		Map<Integer, Double> usage = new HashMap<>();
		Park park = getPark(parkId);
		if (park == null) return usage;

		int cap = park.getCurrentQuota();
		if (cap == 0) cap = 1;

		// Daily average visitor counts
		String sql = "SELECT DAY(log_time), AVG(current_visitors) FROM park_occupancy_log " +
				"WHERE park_id = ? AND MONTH(log_time) = ? AND YEAR(log_time) = ? " +
				"GROUP BY DAY(log_time)";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			ps.setInt(2, month);
			ps.setInt(3, year);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					double avgOcc = rs.getDouble(2);
					double pct = (avgOcc / cap) * 100.0;
					usage.put(rs.getInt(1), pct);
				}
			}
		} catch (SQLException e) {
			System.out.println("Error in usage report query: " + e.getMessage());
		}
		return usage;
	}

	/**
	 * Duration of stay graph report for Department Manager.
	 * Group vs Individual, segmented by stay lengths.
	 */
	public Map<String, List<Integer>> getMonthlyVisitsReport(int parkId, int month, int year) {
		Map<String, List<Integer>> report = new HashMap<>();
		// Lists represent: index 0: 0-2 hours, index 1: 2-4 hours, index 2: 4-6 hours, index 3: 6+ hours
		List<Integer> indList = new ArrayList<>(List.of(0, 0, 0, 0));
		List<Integer> grpList = new ArrayList<>(List.of(0, 0, 0, 0));
		report.put("INDIVIDUAL", indList);
		report.put("GROUP", grpList);

		String sql = "SELECT reservation_type, actual_entry_time, actual_exit_time FROM reservations " +
				"WHERE (? = 0 OR park_id = ?) AND MONTH(visit_date_time) = ? AND YEAR(visit_date_time) = ? AND status = 'COMPLETED'";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			ps.setInt(2, parkId);
			ps.setInt(3, month);
			ps.setInt(4, year);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String type = rs.getString("reservation_type");
					Timestamp inTime = rs.getTimestamp("actual_entry_time");
					Timestamp outTime = rs.getTimestamp("actual_exit_time");

					if (inTime != null && outTime != null) {
						long diffMs = outTime.getTime() - inTime.getTime();
						double diffHrs = diffMs / (1000.0 * 60 * 60);

						List<Integer> target = "ORGANIZED_GROUP".equals(type) ? grpList : indList;
						if (diffHrs <= 2.0) target.set(0, target.get(0) + 1);
						else if (diffHrs <= 4.0) target.set(1, target.get(1) + 1);
						else if (diffHrs <= 6.0) target.set(2, target.get(2) + 1);
						else target.set(3, target.get(3) + 1);
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Error in visits report query: " + e.getMessage());
		}
		return report;
	}

	/**
	 * Cancellations distribution report.
	 * Segments cancels vs. no-shows by hour of the day (8:00 - 20:00).
	 */
	public Map<String, Map<Integer, Integer>> getCancellationsReport(int parkId, int month, int year) {
		Map<String, Map<Integer, Integer>> data = new HashMap<>();
		Map<Integer, Integer> cancels = new HashMap<>();
		Map<Integer, Integer> noShows = new HashMap<>();
		data.put("CANCELLED", cancels);
		data.put("NO_SHOW", noShows);

		for (int h = 8; h <= 20; h++) {
			cancels.put(h, 0);
			noShows.put(h, 0);
		}

		String sql = "SELECT is_no_show, HOUR(visit_date_time) FROM reservations " +
				"WHERE (? = 0 OR park_id = ?) AND MONTH(visit_date_time) = ? AND YEAR(visit_date_time) = ? AND status = 'CANCELLED'";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, parkId);
			ps.setInt(2, parkId);
			ps.setInt(3, month);
			ps.setInt(4, year);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					boolean noShow = rs.getBoolean(1);
					int hr = rs.getInt(2);
					if (hr >= 8 && hr <= 20) {
						Map<Integer, Integer> target = noShow ? noShows : cancels;
						target.put(hr, target.get(hr) + 1);
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Error in cancellations report: " + e.getMessage());
		}
		return data;
	}

	/**
	 * Updates reservation visitor count, price and payment status.
	 *
	 * @param resId reservation identifier
	 * @param visitors updated number of visitors
	 * @param price updated reservation price
	 * @param payStatus updated payment status
	 * @return true if update succeeded, false otherwise
	 */
	public boolean updateReservationVisitorsAndPrice(int resId, int visitors, double price, String payStatus) {
		String sql = "UPDATE reservations SET number_of_visitors = ?, price = ?, payment_status = ? WHERE reservation_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, visitors);
			ps.setDouble(2, price);
			ps.setString(3, payStatus);
			ps.setInt(4, resId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			System.out.println("Error updating reservation visitors/price: " + e.getMessage());
			return false;
		}
	}
}
