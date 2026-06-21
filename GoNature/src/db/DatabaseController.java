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

public class DatabaseController {

    private Connection conn;

    public boolean connect(String host, String dbName, String user, String password) {
        String url = "jdbc:mysql://" + host + "/" + dbName + "?serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("DatabaseController: connected to " + url);
            resetLogins();
            return true;
        } catch (Exception e) {
            System.out.println("ERROR in DatabaseController.connect: " + e.getMessage());
            return false;
        }
    }

    private void resetLogins() {
        String sql = "UPDATE users SET is_logged_in = FALSE";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Error resetting logins: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                resetLogins();
                conn.close();
                System.out.println("DatabaseController: connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("ERROR in DatabaseController.disconnect: " + e.getMessage());
        }
    }

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

    public User loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean alreadyLoggedIn = rs.getBoolean("is_logged_in");
                    if (alreadyLoggedIn) {
                        return null; 
                    }
                    setLoginStatus(username, true);
                    return new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Error logging in user: " + e.getMessage());
        }
        return null;
    }

    public void setLoginStatus(String username, boolean isLoggedIn) {
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

    public Park getPark(int parkId) {
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
                    p.setPendingReservedGap(rs.getInt("pending_reserved_gap"));
                    p.setPendingStayDuration(rs.getInt("pending_stay_duration"));
                    p.setPendingChangesStatus(rs.getString("pending_changes_status"));
                    return p;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching park: " + e.getMessage());
        }
        return null;
    }

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
                p.setPendingChangesStatus(rs.getString("pending_changes_status"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching all parks: " + e.getMessage());
        }
        return list;
    }

    public boolean updateParkParameters(Park park) {
        String sql = "UPDATE parks SET pending_max_quota = ?, pending_reserved_gap = ?, pending_stay_duration = ?, pending_changes_status = 'PENDING_APPROVAL' WHERE park_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, park.getPendingMaxQuota());
            ps.setInt(2, park.getPendingReservedGap());
            ps.setInt(3, park.getPendingStayDuration());
            ps.setInt(4, park.getParkId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating park parameters: " + e.getMessage());
            return false;
        }
    }

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
            System.out.println("Error approving park parameters: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // SUBSCRIBER REGISTER
    // ==========================================

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
                	sub.setIdNumber(String.valueOf(rs.getInt(1)));
                    return sub;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error registering subscriber: " + e.getMessage());
        }
        return null;
    }

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

    public boolean registerGuide(User guide) {
        String sql = "INSERT INTO users (username, password, first_name, last_name, role, email) VALUES (?, ?, ?, ?, 'GUIDE', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, guide.getUsername());
            ps.setString(2, guide.getPassword());
            ps.setString(3, guide.getFname());
            ps.setString(4, guide.getLname());
            ps.setString(5, guide.getRole());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error registering guide: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // CAPACITY VERIFICATION & RESERVATIONS
    // ==========================================

    public boolean checkCapacityAvailable(int parkId, Timestamp visitTime, int numberOfVisitors) {
        Park park = getPark(parkId);
        if (park == null) return false;

        int durationHours = park.getStayDuration();
        long visitStart = visitTime.getTime();
        long visitEnd = visitStart + (durationHours * 60 * 60 * 1000);

        String sql = "SELECT number_of_visitors, visit_date_time, stay_duration FROM reservations " +
                     "WHERE park_id = ? AND DATE(visit_date_time) = DATE(?) AND status IN ('CONFIRMED', 'ACTIVE', 'PENDING_CONFIRMATION')";
                     
        int maxAllowed = park.getCurrentQuota() - park.getReservedGap();
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parkId);
            ps.setTimestamp(2, visitTime);
            
            List<long[]> existing = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp startTs = rs.getTimestamp("visit_date_time");
                    int num = rs.getInt("number_of_visitors");
                    int stayHrs = rs.getInt("stay_duration");
                    existing.add(new long[]{startTs.getTime(), startTs.getTime() + (stayHrs * 60L * 60 * 1000), num});
                }
            }

            for (long t = visitStart; t < visitEnd; t += 15L * 60 * 1000) {
                int currentLoad = 0;
                for (long[] r : existing) {
                    if (t >= r[0] && t < r[1]) {
                        currentLoad += r[2];
                    }
                }
                if (currentLoad + numberOfVisitors > maxAllowed) {
                    return false; 
                }
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Error checking capacity: " + e.getMessage());
            return false;
        }
    }

    public Map<Integer, Integer> getOccupancyForDay(int parkId, Date date) {
        Map<Integer, Integer> loads = new HashMap<>();
        for (int h = 8; h <= 20; h++) {
            loads.put(h, 0);
        }

        String sql = "SELECT number_of_visitors, visit_date_time, stay_duration FROM reservations " +
                     "WHERE park_id = ? AND DATE(visit_date_time) = ? AND status IN ('CONFIRMED', 'ACTIVE', 'PENDING_CONFIRMATION')";
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
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    res.setReservationId(rs.getInt(1));
                    return res;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error creating reservation: " + e.getMessage());
        }
        return null;
    }

    public List<Reservation> getReservationsByVisitorId(String visitorId) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, p.park_name FROM reservations r JOIN parks p ON r.park_id = p.park_id WHERE r.visitor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, visitorId);
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
            System.out.println("Error loading reservations: " + e.getMessage());
        }
        return list;
    }

    public Reservation getReservation(int resId) {
        String sql = "SELECT r.*, p.park_name FROM reservations r JOIN parks p ON r.park_id = p.park_id WHERE r.reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Reservation(
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
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching single reservation: " + e.getMessage());
        }
        return null;
    }

    public boolean updateReservationStatus(int resId, String status, Timestamp ts) {
        String sql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";
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

    // ==========================================
    // REAL-TIME TRACKING & REPORTS
    // ==========================================

    public int getParkCurrentOccupancy(int parkId) {
        String sql = "SELECT SUM(number_of_visitors) FROM reservations WHERE park_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parkId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error calculating occupancy: " + e.getMessage());
        }
        return 0;
    }

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

    public Map<String, Integer> getMonthlyVisitorReport(int parkId, int month, int year) {
        Map<String, Integer> data = new HashMap<>();
        data.put("INDIVIDUAL", 0);
        data.put("FAMILY_SUBSCRIBER", 0);
        data.put("ORGANIZED_GROUP", 0);
        String sql = "SELECT reservation_type, SUM(number_of_visitors) FROM reservations " +
                     "WHERE park_id = ? AND MONTH(visit_date_time) = ? AND YEAR(visit_date_time) = ? AND status = 'COMPLETED' " +
                     "GROUP BY reservation_type";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parkId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.put(rs.getString(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in monthly visitor report: " + e.getMessage());
        }
        return data;
    }

    // ==========================================
    // PROMOTIONS
    // ==========================================

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
            System.out.println("Error loading pending promotions: " + e.getMessage());
        }
        return list;
    }

    public boolean approvePromotion(int promotionId, boolean approve) {
        String status = approve ? "APPROVED" : "REJECTED";
        String sql = "UPDATE promotions SET status = ? WHERE promotion_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, promotionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error resolving promotion: " + e.getMessage());
            return false;
        }
    }

    public double getActivePromotionDiscount(int parkId, Timestamp time) {
        String sql = "SELECT discount_percentage FROM promotions WHERE park_id = ? AND status = 'APPROVED' AND ? BETWEEN start_date AND end_date LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parkId);
            ps.setDate(2, new Date(time.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("discount_percentage");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching active promotion discount: " + e.getMessage());
        }
        return 0.0;
    }

    public boolean updateReservationVisitorsAndPrice(int resId, int visitors, double price, String payStatus) {
        String sql = "UPDATE reservations SET number_of_visitors = ?, price = ?, payment_status = ? WHERE reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitors);
            ps.setDouble(2, price);
            ps.setString(3, payStatus);
            ps.setInt(4, resId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating reservation details: " + e.getMessage());
            return false;
        }
    }
}
