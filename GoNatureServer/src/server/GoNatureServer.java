package server;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.ChatIF;
import common.Message;
import common.Park;
import common.Reservation;
import common.Subscriber;
import common.User;
import common.Promotion;
import db.DatabaseController;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/**
 * Main server class of the GoNature system.
 *
 * <p>This class is responsible for managing client connections,
 * processing requests received from clients, communicating with the
 * database, handling reservations, park management, occupancy tracking,
 * promotions, reports, and simulation scheduling.</p>
 *
 * <p>The server extends {@link AbstractServer} and uses a
 * {@link DatabaseController} instance to perform all database operations.
 * It also maintains active client connections, performs heartbeat checks
 * to detect disconnected clients, and coordinates background simulation
 * tasks through the {@link SimulationScheduler}.</p>
 *
 * <p>Supported operations include:</p>
 * <ul>
 *   <li>User login and logout</li>
 *   <li>Park information retrieval and updates</li>
 *   <li>Reservation creation, confirmation, cancellation, and lookup</li>
 *   <li>Waiting list management</li>
 *   <li>Visitor entry and exit registration</li>
 *   <li>Promotion management</li>
 *   <li>Generation of management reports</li>
 *   <li>Simulation time synchronization</li>
 * </ul>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
public class GoNatureServer extends AbstractServer  {

	private final DatabaseController db;
	private final String dbHost;
	private final String dbName;
	private final String dbUser;
	private final String dbPassword;
	private final ChatIF ui;
	private final SimulationScheduler scheduler;
	private Runnable onConnectionsChanged; // GUI update callback

	// We track live connections ourselves rather than relying on
	// getClientConnections(), which doesn't reliably drop hard-closed clients.
	private final java.util.Set<ConnectionToClient> liveClients =
			java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());

	/**
	 * Returns a snapshot of all currently connected clients.
	 *
	 * @return a list containing all active client connections
	 */
	public java.util.List<ConnectionToClient> getLiveClients(){
		synchronized (liveClients) {
			return new java.util.ArrayList<>(liveClients);
		}
	}

	/**
	 * Creates and initializes a new GoNature server instance.
	 *
	 * @param port the port on which the server listens for client connections
	 * @param dbHost the database host address
	 * @param dbName the database name
	 * @param dbUser the database username
	 * @param dbPassword the database password
	 * @param ui the user interface used for displaying server messages
	 */
	public GoNatureServer(int port, String dbHost, String dbName,
			String dbUser, String dbPassword, ChatIF ui) {
		super(port);
		this.dbHost = dbHost;
		this.dbName = dbName;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		this.ui = ui;
		this.db = new DatabaseController();
		this.scheduler = new SimulationScheduler(this.db, ui);
	}

	/**
	 * Registers a callback that is executed whenever the
	 * client connection list changes.
	 *
	 * @param callback the callback to invoke when connections change
	 */
	public void setOnConnectionsChanged(Runnable callback) {
		this.onConnectionsChanged = callback;
	}

	@Override
	protected void serverStarted() {
		ui.display("Server: Starting listener on port " + getPort());
		startHeartbeat();
		if (db.connect(dbHost, dbName, dbUser, dbPassword)) {
			ui.display("Server: Database connected successfully.");
			scheduler.start();
		} else {
			ui.display("Server ERROR: Database connection failed. Queries will fail.");
		}
	}    


	/**
	 * Starts the heartbeat monitoring thread.
	 *
	 * <p>The heartbeat thread periodically checks all active client
	 * connections to verify that they are still responsive. A PING
	 * message is sent to each client every two seconds. If a client
	 * does not respond within the configured timeout period, it is
	 * considered disconnected and is removed from the active
	 * connections list.</p>
	 *
	 * <p>The thread runs as a daemon and remains active while the
	 * server is listening for client connections.</p>
	 */
	private void startHeartbeat() {
		Thread hb = new Thread(() -> {
			while (isListening()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					return;
				}
				long now = System.currentTimeMillis();
				for (ConnectionToClient client : getLiveClients()) {
					try {
						Long lastSeen = (Long) client.getInfo("LastSeen");
						long age = (lastSeen != null) ? (now - lastSeen) : -1;
						ui.display("Heartbeat: checking client, last seen " + age + "ms ago");
						if (lastSeen != null && now - lastSeen > 6000) {
							ui.display("Heartbeat: client is STALE, removing.");
							markDisconnected(client);
							try { client.close(); } catch (Exception ignored) {}
							continue;
						}
						client.sendToClient(new Message(Message.PING, null));
					} catch (Exception ex) {
						// Any failure talking to this client = it's gone.
						ui.display("Heartbeat: client unreachable, removing.");
						markDisconnected(client);
					}
				}
			}
		});
		hb.setDaemon(true);
		hb.start();
	}

	/**
	 * Performs cleanup operations when the server stops listening.
	 *
	 * <p>This method stops the simulation scheduler and disconnects
	 * the server from the database.</p>
	 */
	@Override
	protected void serverStopped() {
		ui.display("Server: Listener stopped.");
		scheduler.stop();
		db.disconnect();
	}
	/**
	 * Handles a new client connection.
	 *
	 * <p>Adds the client to the active connections list, logs the
	 * connection event, and notifies listeners that the connection
	 * list has changed.</p>
	 *
	 * @param client the client that has connected to the server
	 */
	@Override
	protected void clientConnected(ConnectionToClient client) {
		liveClients.add(client);
		ui.display("Client connected: " + client.getInetAddress().getHostAddress());
		notifyConnectionsChanged();
	}

	/**
	 * Handles a client disconnection.
	 *
	 * <p>Removes the client from the active connections list and
	 * performs any required cleanup operations.</p>
	 *
	 * @param client the client that has disconnected
	 */
	@Override
	protected synchronized void clientDisconnected(ConnectionToClient client) {
		markDisconnected(client);
	}

	/**
	 * Handles an exception that occurred on a client connection.
	 *
	 * <p>The affected client is treated as disconnected and is
	 * removed from the active connections list.</p>
	 *
	 * @param client the client that caused the exception
	 * @param exception the exception that occurred
	 */
	@Override
	protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
		markDisconnected(client);
	}

	/**
	 * Marks a client as disconnected and performs the necessary cleanup.
	 *
	 * <p>This method removes the client from the active connections list,
	 * logs the disconnection event, updates the login status of any user
	 * associated with the connection, and notifies listeners that the
	 * connection list has changed.</p>
	 *
	 * @param client the client connection that has been disconnected
	 */
	private void markDisconnected(ConnectionToClient client) {
		if (client.getInfo("Disconnected") == null) {
			client.setInfo("Disconnected", true);
			liveClients.remove(client);

			// A closed socket may have no InetAddress, so guard against null.
			java.net.InetAddress addr = client.getInetAddress();
			String ip = (addr != null) ? addr.getHostAddress() : "unknown";
			ui.display("Client disconnected: " + ip);

			// Clean up logged in users associated with this connection
			String username = (String) client.getInfo("Username");
			if (username != null) {
				db.setLoginStatus(username, false);
				ui.display("Logged out user: " + username);
			}

			notifyConnectionsChanged();
		}
	}

	/**
	 * Executes the connection-change callback if one is registered.
	 */
	private void notifyConnectionsChanged() {
		if (onConnectionsChanged != null) {
			onConnectionsChanged.run();
		}
	}

	/**
	 * Handles a message received from a connected client.
	 *
	 * <p>This method updates the client's last activity timestamp,
	 * validates the received object, processes heartbeat responses,
	 * forwards valid requests to the request processor, and sends
	 * the generated response back to the client.</p>
	 *
	 * <p>If an error occurs during request processing, an error
	 * message is returned to the client.</p>
	 *
	 * @param msg the object received from the client, expected to be a
	 *            {@link Message} instance
	 * @param client the client connection that sent the message
	 */
	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client){
		client.setInfo("LastSeen", System.currentTimeMillis());

		if (!(msg instanceof Message)) {
			System.out.println("ERROR: unexpected message type received: " + msg);
			return;
		}

		Message request = (Message) msg;

		// PONG is just a heartbeat reply; LastSeen is already updated above.
		if (Message.PONG.equals(request.getAction())) {
			return;
		}

		ui.display("Request Received: " + request.getAction() + " from " + client.getInetAddress().getHostAddress());

		try {
			Message response = processRequest(request, client);
			client.sendToClient(response);
		} catch (Exception e) {
			ui.display("Error processing request: " + e.getMessage());
			try {
				client.sendToClient(new Message(Message.ERROR, "Server Error: " + e.getMessage()));
			} catch (IOException ioe) {
				System.out.println("Failed to send error response to client: " + ioe.getMessage());
			}
		}
	}

	/**
	 * Processes a request received from a client and returns
	 * an appropriate response message.
	 *
	 * @param request the client request message
	 * @param client the client that sent the request
	 * @return the response message to be sent back to the client
	 */
	private Message processRequest(Message request, ConnectionToClient client) {
		if (!db.isConnected()) {//make sure we are connected to the DB before making any request
			return new Message(Message.ERROR, "Database is NOT connected to the server. Please check your MySQL connection settings on the Server GUI.");
		}
		switch (request.getAction()) {
		case Message.LOGIN: {
			String[] creds = (String[]) request.getPayload();

			User user = db.loginUser(creds[0], creds[1]);
			if (user != null) {
				client.setInfo("Username", user.getUsername());//setinfo lets us attach value data to the connection that survives across requests
				//the only "memory" the server keeps about a connection is in the connection info
				return new Message(Message.OK, user);
			} else {
				return new Message(Message.ERROR, "Login failed. Invalid credentials or user already logged in.");
			}
		}
		case Message.LOGOUT: {
			String username = (String) request.getPayload();

			db.setLoginStatus(username, false);
			client.setInfo("Username", null);
			return new Message(Message.OK, "Logged out successfully");
		}
		case Message.GET_PARK: {
			int parkId = (Integer) request.getPayload();

			Park p = db.getPark(parkId);
			if (p != null) {
				return new Message(Message.OK, p);
			} else {
				return new Message(Message.ERROR, "Park not found");
			}
		}
		case Message.GET_ALL_PARKS: {

			List<Park> all = db.getAllParks();
			return new Message(Message.OK, new ArrayList<>(all));
		}
		case Message.UPDATE_PARK_PARAMETERS: {
			Park p = (Park) request.getPayload();

			if (db.updateParkParameters(p)) {
				return new Message(Message.OK, "Park parameters updated successfully");
			} else {
				return new Message(Message.ERROR, "Failed to update parameters");
			}
		}
		case Message.GET_PENDING_PARKS: {
			List<Park> pending = new ArrayList<>();//new array to store pending parks

			for (Park p : db.getAllParks()) {
				if ("PENDING_APPROVAL".equals(p.getPendingChangesStatus())) {//iterate over all parks and get the pending ones
					pending.add(p);
				}
			}
			return new Message(Message.OK, pending);
		}
		case Message.APPROVE_PARK_PARAMETERS: {
			Object[] payload = (Object[]) request.getPayload();
			int parkId = (Integer) payload[0];
			boolean approve = (Boolean) payload[1];
			if (db.approveParkParameters(parkId, approve)) {
				return new Message(Message.OK, "Park parameters update resolved");
			} else {
				return new Message(Message.ERROR, "Failed to resolve parameters");
			}
		}
		case Message.CREATE_RESERVATION: {
			Reservation res = (Reservation) request.getPayload();






			// If visitor is a subscriber, upgrade the reservation type to FAMILY_SUBSCRIBER
			Subscriber sub = db.getSubscriberById(res.getVisitorId());
			if (sub != null) {
				res.setReservationType("FAMILY_SUBSCRIBER");
			}

			// Check capacity
			boolean hasSpace = db.checkCapacityAvailable(res.getParkId(), res.getVisitDateTime(), res.getNumberOfVisitors());
			if (!hasSpace) {
				return new Message(Message.ERROR, "FULL");
			}

			// Set status before price calculation so that advance discount is applied
			res.setStatus("CONFIRMED");
			if (res.getPaymentStatus() == null) {
				res.setPaymentStatus("UNPAID");
			}

			// Calculate pricing
			double basePrice = 50.0; // Assume 50 NIS standard entrance
			double finalPrice = calculatePrice(res, basePrice, db.getActivePromotionDiscount(res.getParkId(), res.getVisitDateTime()));
			res.setPrice(finalPrice);

			Reservation saved = db.createReservation(res);
			if (saved != null) {
				return new Message(Message.OK, saved);
			} else {
				return new Message(Message.ERROR, "Database error creating reservation.");
			}
		}
		case Message.ENTER_WAITING_LIST: {
			Reservation res = (Reservation) request.getPayload();

			// If visitor is a subscriber, upgrade the reservation type to FAMILY_SUBSCRIBER
			Subscriber sub = db.getSubscriberById(res.getVisitorId());
			if (sub != null) {
				res.setReservationType("FAMILY_SUBSCRIBER");
			}

			res.setStatus("WAITING_LIST");
			res.setPaymentStatus("UNPAID");

			// Base price calculation still saved
			double finalPrice = calculatePrice(res, 50.0, db.getActivePromotionDiscount(res.getParkId(), res.getVisitDateTime()));
			res.setPrice(finalPrice);
			Reservation saved = db.createReservation(res);
			if (saved != null) {
				return new Message(Message.OK, saved);
			} else {
				return new Message(Message.ERROR, "Failed to join waiting list");
			}
		}
		case Message.GET_OCCUPANCY: {
			int parkId = (Integer) request.getPayload();
			int occ = db.getParkCurrentOccupancy(parkId);
			return new Message(Message.OK, occ);
		}
		case Message.GET_OCCUPANCY_TABLE: {
			Object[] payload = (Object[]) request.getPayload();
			int parkId = (Integer) payload[0];
			java.sql.Date date = (java.sql.Date) payload[1];
			Map<Integer, Integer> table = db.getOccupancyForDay(parkId, date);
			return new Message(Message.OK, new HashMap<>(table));
		}
		case Message.GET_RESERVATION: {
			int resId = (Integer) request.getPayload();
			Reservation r = db.getReservation(resId);
			if (r != null) {
				return new Message(Message.OK, r);
			} else {
				return new Message(Message.ERROR, "Reservation not found");
			}
		}
		case Message.GET_RESERVATIONS_BY_ID: {
			String visitorId = (String) request.getPayload();
			List<Reservation> list = db.getReservationsByVisitorId(visitorId);
			return new Message(Message.OK, new ArrayList<>(list));
		}
		case Message.CANCEL_RESERVATION: {
			int resId = (Integer) request.getPayload();
			Reservation r = db.getReservation(resId);
			if (r != null) {
				db.updateReservationStatus(resId, "CANCELLED", SimulationScheduler.getSimulatedTimestamp());
				scheduler.promoteNextWaiting(r.getParkId(), r.getVisitDateTime());
				return new Message(Message.OK, "Cancelled successfully");
			} else {
				return new Message(Message.ERROR, "Reservation not found");
			}
		}
		case Message.CONFIRM_RESERVATION: {
			int resId = (Integer) request.getPayload();
			if (db.updateReservationStatus(resId, "CONFIRMED", SimulationScheduler.getSimulatedTimestamp())) {
				return new Message(Message.OK, "Confirmed successfully");
			} else {
				return new Message(Message.ERROR, "Failed to confirm");
			}
		}
		case Message.REGISTER_SUBSCRIBER: {
			Subscriber sub = (Subscriber) request.getPayload();
			Subscriber saved = db.registerSubscriber(sub);
			if (saved != null) {
				return new Message(Message.OK, saved);
			} else {
				return new Message(Message.ERROR, "Failed to register subscriber. ID may already exist.");
			}
		}
		case Message.REGISTER_GUIDE: {
			User guide = (User) request.getPayload();
			if (db.registerGuide(guide)) {
				return new Message(Message.OK, "Guide registered successfully");
			} else {
				return new Message(Message.ERROR, "Failed to register guide. Username may already exist.");
			}
		}
		case Message.REGISTER_ENTRY: {
			Object[] payload = (Object[]) request.getPayload();
			String idOrCode = (String) payload[0];
			int actualVisitors = (Integer) payload[1];
			int parkId = (Integer) payload[2];

			// Try to find a reservation by code
			Reservation res = null;
			try {
				int code = Integer.parseInt(idOrCode);
				res = db.getReservation(code);
			} catch (NumberFormatException ignored) {}

			if (res == null) {
				// Try to find reservation by visitor ID
				List<Reservation> activeRes = db.getReservationsByVisitorId(idOrCode);
				for (Reservation r : activeRes) {
					if ("CONFIRMED".equals(r.getStatus()) || "PENDING_CONFIRMATION".equals(r.getStatus())) {
						res = r;
						break;
					}
				}
			}

			if (res != null) {
				// Check if reservation is for the correct park
				if (res.getParkId() != parkId) {
					return new Message(Message.ERROR, "Reservation is for another park: " + res.getParkName());
				}

				// Verify reservation fits capacity
				int occupancy = db.getParkCurrentOccupancy(parkId);
				Park p = db.getPark(parkId);
				if (occupancy + actualVisitors > p.getCurrentQuota()) {
					return new Message(Message.ERROR, "Park is full. Cannot enter even with reservation.");
				}

				// If visitors count changed, recalculate price
				res.setNumberOfVisitors(actualVisitors);
				double finalPrice = calculatePrice(res, 50.0, db.getActivePromotionDiscount(parkId, SimulationScheduler.getSimulatedTimestamp()));
				res.setPrice(finalPrice);
				res.setPaymentStatus("PAID_AT_ENTRANCE");
				res.setStatus("ACTIVE");

				db.updateReservationStatus(res.getReservationId(), "ACTIVE", SimulationScheduler.getSimulatedTimestamp());
				// Sync visitors count and price in DB too
				updateReservationDetails(res);

				db.logOccupancyChange(parkId, occupancy + actualVisitors, SimulationScheduler.getSimulatedTimestamp());
				return new Message(Message.OK, res);
			} else {
				// SPONTANEOUS ENTRY
				// Check if space available
				int occupancy = db.getParkCurrentOccupancy(parkId);
				Park p = db.getPark(parkId);
				if (occupancy + actualVisitors > p.getCurrentQuota()) {
					return new Message(Message.ERROR, "Park is full. Spontaneous entry denied.");
				}

				// Create spontaneous reservation
				Reservation spotRes = new Reservation();
				spotRes.setVisitorId(idOrCode);
				spotRes.setParkId(parkId);
				spotRes.setVisitDateTime(SimulationScheduler.getSimulatedTimestamp());
				spotRes.setNumberOfVisitors(actualVisitors);
				spotRes.setEmail("spontaneous@gonature.gov.il");
				spotRes.setPhoneNumber("000-0000");

				// Determine reservation type
				Subscriber sub = db.getSubscriberById(idOrCode);
				User guide = db.loginUser(idOrCode, ""); // Check if guide by ID (login user check)

				if (sub != null) {
					spotRes.setReservationType("FAMILY_SUBSCRIBER");
				} else if (guide != null && "GUIDE".equals(guide.getRole())) {
					spotRes.setReservationType("ORGANIZED_GROUP");
				} else {
					spotRes.setReservationType("INDIVIDUAL");
				}

				double finalPrice = calculatePrice(spotRes, 50.0, db.getActivePromotionDiscount(parkId, SimulationScheduler.getSimulatedTimestamp()));
				spotRes.setPrice(finalPrice);
				spotRes.setStatus("ACTIVE");
				spotRes.setPaymentStatus("PAID_AT_ENTRANCE");
				spotRes.setCreatedAt(SimulationScheduler.getSimulatedTimestamp());

				Reservation saved = db.createReservation(spotRes);
				if (saved != null) {
					db.updateReservationStatus(saved.getReservationId(), "ACTIVE", SimulationScheduler.getSimulatedTimestamp());
					db.logOccupancyChange(parkId, occupancy + actualVisitors, SimulationScheduler.getSimulatedTimestamp());
					return new Message(Message.OK, saved);
				} else {
					return new Message(Message.ERROR, "Failed to create spontaneous reservation.");
				}
			}
		}
		case Message.REGISTER_EXIT: {
			String idOrCode = (String) request.getPayload();
			Reservation res = null;
			try {
				int code = Integer.parseInt(idOrCode);
				res = db.getReservation(code);
			} catch (NumberFormatException ignored) {}

			if (res == null) {
				// Try to find active reservation by visitor ID
				String sql = "SELECT reservation_id FROM reservations WHERE visitor_id = ? AND status = 'ACTIVE'";
				// query database directly
				List<Reservation> active = db.getReservationsByVisitorId(idOrCode);
				for (Reservation r : active) {
					if ("ACTIVE".equals(r.getStatus())) {
						res = r;
						break;
					}
				}
			}

			// Fetch full reservation if found by visitor ID
			if (res != null && !"ACTIVE".equals(res.getStatus())) {
				res = db.getReservation(res.getReservationId());
			}

			if (res != null && "ACTIVE".equals(res.getStatus())) {
				db.updateReservationStatus(res.getReservationId(), "COMPLETED", SimulationScheduler.getSimulatedTimestamp());

				int parkId = res.getParkId();
				int occupancy = db.getParkCurrentOccupancy(parkId);
				db.logOccupancyChange(parkId, occupancy, SimulationScheduler.getSimulatedTimestamp()); // Save new occupancy after exit

				// Promote anyone waiting since space freed
				scheduler.promoteNextWaiting(parkId, res.getVisitDateTime());
				return new Message(Message.OK, "Exit registered successfully. Have a nice day!");
			} else {
				return new Message(Message.ERROR, "No active visit found for the provided code/ID");
			}
		}
		case Message.GET_MONTHLY_VISITOR_REPORT: {
			Object[] payload = (Object[]) request.getPayload();
			int parkId = (Integer) payload[0];
			int month = (Integer) payload[1];
			int year = (Integer) payload[2];
			Map<String, Integer> data = db.getMonthlyVisitorReport(parkId, month, year);
			return new Message(Message.OK, new HashMap<>(data));
		}
		case Message.GET_MONTHLY_USAGE_REPORT: {
			Object[] payload = (Object[]) request.getPayload();
			int parkId = (Integer) payload[0];
			int month = (Integer) payload[1];
			int year = (Integer) payload[2];
			Map<Integer, Double> data = db.getMonthlyUsageReport(parkId, month, year);
			return new Message(Message.OK, new HashMap<>(data));
		}
		case Message.GET_MONTHLY_VISITS_REPORT: {
			Object[] payload = (Object[]) request.getPayload();
			int parkId = (Integer) payload[0];
			int month = (Integer) payload[1];
			int year = (Integer) payload[2];
			Map<String, List<Integer>> data = db.getMonthlyVisitsReport(parkId, month, year);
			return new Message(Message.OK, new HashMap<>(data));
		}
		case Message.GET_MONTHLY_CANCELLATIONS_REPORT: {
			Object[] payload = (Object[]) request.getPayload();
			int parkId = (Integer) payload[0];
			int month = (Integer) payload[1];
			int year = (Integer) payload[2];
			Map<String, Map<Integer, Integer>> data = db.getCancellationsReport(parkId, month, year);
			return new Message(Message.OK, new HashMap<>(data));
		}
		case Message.CREATE_PROMOTION: {
			Promotion p = (Promotion) request.getPayload();
			if (db.createPromotion(p)) {
				return new Message(Message.OK, "Promotion submitted for approval");
			} else {
				return new Message(Message.ERROR, "Failed to create promotion");
			}
		}
		case Message.GET_PENDING_PROMOTIONS: {
			List<Promotion> pending = db.getPendingPromotions();
			return new Message(Message.OK, new ArrayList<>(pending));
		}
		case Message.APPROVE_PROMOTION: {
			Object[] payload = (Object[]) request.getPayload();
			int promoId = (Integer) payload[0];
			boolean approve = (Boolean) payload[1];
			if (db.approvePromotion(promoId, approve)) {
				return new Message(Message.OK, "Promotion resolved");
			} else {
				return new Message(Message.ERROR, "Failed to resolve promotion");
			}
		}
		case Message.GET_SIMULATION_TIME: {
			long start = SimulationScheduler.getStartTimeMs();
			double speed = SimulationScheduler.getSpeedup();
			return new Message(Message.OK, new Object[]{start, speed});
		}
		default:
			return new Message(Message.ERROR, "Unknown server command: " + request.getAction());
		}
	}

	/**
	 * Updates the number of visitors, payment status, and price
	 * of an existing reservation in the database.
	 *
	 * @param res the reservation containing the updated details
	 */
	private void updateReservationDetails(Reservation res) {
		db.updateReservationVisitorsAndPrice(res.getReservationId(), res.getNumberOfVisitors(), res.getPrice(), res.getPaymentStatus());
	}

	/**
	 * Calculates the final reservation price according to the
	 * GoNature pricing policy, including reservation discounts,
	 * subscriber benefits, group discounts, advance-payment discounts,
	 * and promotional discounts.
	 *
	 * @param res the reservation for which the price is calculated
	 * @param basePrice the standard entrance price per visitor
	 * @param promoDiscount the active promotional discount rate
	 * @return the final calculated price rounded to two decimal places
	 */
	private double calculatePrice(Reservation res, double basePrice,
			double promoDiscount) {
		int visitors = res.getNumberOfVisitors();
		double pricePerPerson = basePrice;

		// Apply promotional discount if active (additional discount, e.g., subtracted from rate)
		double rateDiscount = 0.0;
		if (promoDiscount > 0) {
			rateDiscount = promoDiscount;
		}

		double groupSubtotal = 0.0;
		StringBuilder sb = new StringBuilder();

		switch (res.getReservationType()) {
		case "INDIVIDUAL":
			if ("CONFIRMED".equals(res.getStatus()) || "PENDING_CONFIRMATION".equals(res.getStatus()) || "WAITING_LIST".equals(res.getStatus())) {
				// Reserved in advance: 15% discount
				double discount = 0.15 + rateDiscount;
				double baseSubtotal = visitors * pricePerPerson;
				sb.append(String.format("Base Price: %d visitors x %.2f NIS = %.2f NIS\n", visitors, pricePerPerson, baseSubtotal));
				sb.append("Reservation Type: Individual (Pre-registered)\n");
				sb.append("Discounts applied:\n");
				sb.append("  - Advance booking discount: -15%\n");
				if (promoDiscount > 0) {
					sb.append(String.format("  - Active Park Promotion: -%.0f%%\n", promoDiscount * 100));
				}
				groupSubtotal = baseSubtotal * (1.0 - Math.min(discount, 1.0));
				sb.append(String.format("Final Price: %.2f NIS", groupSubtotal));
			} else {
				// Spontaneous: full price
				double discount = rateDiscount;
				double baseSubtotal = visitors * pricePerPerson;
				sb.append(String.format("Base Price: %d visitors x %.2f NIS = %.2f NIS\n", visitors, pricePerPerson, baseSubtotal));
				sb.append("Reservation Type: Individual (Spontaneous)\n");
				if (promoDiscount > 0) {
					sb.append("Discounts applied:\n");
					sb.append(String.format("  - Active Park Promotion: -%.0f%%\n", promoDiscount * 100));
				}
				groupSubtotal = baseSubtotal * (1.0 - Math.min(discount, 1.0));
				sb.append(String.format("Final Price: %.2f NIS", groupSubtotal));
			}
			break;
		case "FAMILY_SUBSCRIBER":
			// Subscribers get the individual reserved discount (15%) + subscriber discount (10% compound)
			if ("CONFIRMED".equals(res.getStatus()) || "PENDING_CONFIRMATION".equals(res.getStatus()) || "WAITING_LIST".equals(res.getStatus())) {
				double discount = 0.15 + rateDiscount;
				double baseSubtotal = visitors * pricePerPerson;
				sb.append(String.format("Base Price: %d visitors x %.2f NIS = %.2f NIS\n", visitors, pricePerPerson, baseSubtotal));
				sb.append("Reservation Type: Subscriber (Pre-registered)\n");
				sb.append("Discounts applied:\n");
				sb.append("  - Advance booking discount: -15%\n");
				if (promoDiscount > 0) {
					sb.append(String.format("  - Active Park Promotion: -%.0f%%\n", promoDiscount * 100));
				}
				double subtotal = baseSubtotal * (1.0 - Math.min(discount, 1.0));
				sb.append("  - Subscriber club discount: -10% (compounded)\n");
				groupSubtotal = subtotal * 0.90;
				sb.append(String.format("Final Price: %.2f NIS", groupSubtotal));
			} else {
				// Spontaneous subscriber: full price minus subscriber 10% compound
				double discount = rateDiscount;
				double baseSubtotal = visitors * pricePerPerson;
				sb.append(String.format("Base Price: %d visitors x %.2f NIS = %.2f NIS\n", visitors, pricePerPerson, baseSubtotal));
				sb.append("Reservation Type: Subscriber (Spontaneous)\n");
				sb.append("Discounts applied:\n");
				if (promoDiscount > 0) {
					sb.append(String.format("  - Active Park Promotion: -%.0f%%\n", promoDiscount * 100));
				}
				double subtotal = baseSubtotal * (1.0 - Math.min(discount, 1.0));
				sb.append("  - Subscriber club discount: -10% (compounded)\n");
				groupSubtotal = subtotal * 0.90;
				sb.append(String.format("Final Price: %.2f NIS", groupSubtotal));
			}
			break;
		case "ORGANIZED_GROUP":
			// Group is max 15 people
			if ("CONFIRMED".equals(res.getStatus()) || "PENDING_CONFIRMATION".equals(res.getStatus()) || "WAITING_LIST".equals(res.getStatus())) {
				// Reserved group: 25% discount. Guide does not pay (visitors - 1).
				// If advance payment: additional 12% discount.
				double baseDiscount = 0.25 + rateDiscount;
				int payingCount = Math.max(1, visitors - 1);
				double baseSubtotal = payingCount * pricePerPerson;
				sb.append(String.format("Base Price (Guide Free): %d paying visitors x %.2f NIS = %.2f NIS\n", payingCount, pricePerPerson, baseSubtotal));
				sb.append("Reservation Type: Organized Group (Pre-registered)\n");
				sb.append("Discounts applied:\n");
				sb.append("  - Group booking discount: -25%\n");
				if (promoDiscount > 0) {
					sb.append(String.format("  - Active Park Promotion: -%.0f%%\n", promoDiscount * 100));
				}
				double subtotal = baseSubtotal * (1.0 - Math.min(baseDiscount, 1.0));

				if ("PAID_IN_ADVANCE".equals(res.getPaymentStatus())) {
					sb.append("  - Pre-payment discount: -12% (compounded)\n");
					groupSubtotal = subtotal * 0.88; // 12% additional discount
				} else {
					groupSubtotal = subtotal;
				}
				sb.append(String.format("Final Price: %.2f NIS", groupSubtotal));
			} else {
				// Spontaneous group: 10% discount. Guide pays.
				double discount = 0.10 + rateDiscount;
				double baseSubtotal = visitors * pricePerPerson;
				sb.append(String.format("Base Price (Guide Pays): %d visitors x %.2f NIS = %.2f NIS\n", visitors, pricePerPerson, baseSubtotal));
				sb.append("Reservation Type: Organized Group (Spontaneous)\n");
				sb.append("Discounts applied:\n");
				sb.append("  - Spontaneous group discount: -10%\n");
				if (promoDiscount > 0) {
					sb.append(String.format("  - Active Park Promotion: -%.0f%%\n", promoDiscount * 100));
				}
				groupSubtotal = baseSubtotal * (1.0 - Math.min(discount, 1.0));
				sb.append(String.format("Final Price: %.2f NIS", groupSubtotal));
			}
			break;
		default:
			groupSubtotal = visitors * pricePerPerson;
			sb.append(String.format("Base Price: %d visitors x %.2f NIS = %.2f NIS\n", visitors, pricePerPerson, groupSubtotal));
		}

		double finalRounded = Math.round(groupSubtotal * 100.0) / 100.0;
		res.setPriceBreakdown(sb.toString());
		return finalRounded;
	}

	private static boolean isSameDay(Timestamp ts1, Timestamp ts2) {
		if (ts1 == null || ts2 == null) return false;
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(ts1).equals(sdf.format(ts2));
	}
}
