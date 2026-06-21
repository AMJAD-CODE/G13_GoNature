package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import common.User;

public class DatabaseController {

    private Connection conn;

    public boolean connect(String host, String dbName, String user, String password) {
        String url = "jdbc:mysql://" + host + "/" + dbName + "?serverTimezone=Asia/Jerusalem&useSSL=false";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database connection established.");
            return true;
        } catch (Exception e) {
            System.out.println("Database connection error: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Database disconnection error: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public User loginUser(String username, String password) {
        if (!isConnected()) {
            System.out.println("Error: Database not connected.");
            return null;
        }

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
            System.out.println("SQL Error in loginUser: " + e.getMessage());
        }
        return null;
    }
}
