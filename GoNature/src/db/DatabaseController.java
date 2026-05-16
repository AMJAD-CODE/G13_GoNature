package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import common.Order;

public class DatabaseController {

	// Connection settings
	private static final String DB_URL = "jdbc:mysql://localhost:3306/gonature?serverTimezone=Asia/Jerusalem&useSSL=false";
	private static final String DB_USER = "root";
	private static final String DB_PASSWORD = "Amjad2002";

	private Connection conn;

	// Open the database connection called once by the server at startup

	// @return true if successful, false if the connection failed.

	public boolean connect() {
		try {
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
			System.out.println("DatabaseController: connection opened.");
			return true;
		} catch (SQLException e) {
			System.out.println("ERROR in DatabaseController.connect: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Reads every row from the Order table and converts each into an Order object.
	 * 
	 * @return list of Orders (empty list if the query fails for any reason).
	 */
	public List<Order> getAllOrders() {
		List<Order> result = new ArrayList<>();
		String sql = "SELECT * FROM `Order`";

		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				Order order = new Order(rs.getInt("order_number"), rs.getDate("order_date"),
						rs.getInt("number_of_visitors"), rs.getInt("confirmation_code"), rs.getInt("subscriber_id"),
						rs.getDate("date_of_placing_order"));
				result.add(order);
			}
		} catch (SQLException e) {
			System.out.println("ERROR in DatabaseController.getAllOrders: " + e.getMessage());
		}

		return result;
	}

	/**
	 * Updates an existing order's order_date and number_of_visitors fields. The
	 * order_number identifies which row to change.
	 * 
	 * @param order Order carrying the new values.
	 * @return true if exactly one row was modified.
	 */
	public boolean updateOrder(Order order) {
		String sql = "UPDATE `Order` SET order_date = ?, number_of_visitors = ? WHERE order_number = ?";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setDate(1, order.getOrderDate());
			ps.setInt(2, order.getNumberOfVisitors());
			ps.setInt(3, order.getOrderNumber());

			int rowsAffected = ps.executeUpdate();
			return rowsAffected > 0;
		} catch (SQLException e) {
			System.out.println("ERROR in DatabaseController.updateOrder: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Closes the DB connection cleanly. Called when the server shuts down.
	 */
	public void disconnect() {
		try {
			if (conn != null && !conn.isClosed()) {
				conn.close();
				System.out.println("DatabaseController: connection closed.");
			}
		} catch (SQLException e) {
			System.out.println("ERROR in DatabaseController.disconnect: " + e.getMessage());
		}
	}
}