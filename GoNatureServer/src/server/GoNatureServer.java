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

public class GoNatureServer extends AbstractServer {

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

    public java.util.List<ConnectionToClient> getLiveClients() {
        synchronized (liveClients) {
            return new java.util.ArrayList<>(liveClients);
        }
    }

    public GoNatureServer(int port, String dbHost, String dbName, String dbUser, String dbPassword, ChatIF ui) {
        super(port);
        this.dbHost = dbHost;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.ui = ui;
        this.db = new DatabaseController();
        this.scheduler = new SimulationScheduler(this.db, ui);
    }

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
    
    
    private void startHeartbeat() {
        Thread hb = new Thread(() -> {
            while (isListening()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    return;
                }
                long now = System.currentTimeMillis();
                for (ConnectionToClient client : getLiveClients()) {
                    Long lastSeen = (Long) client.getInfo("LastSeen");
                    if (lastSeen != null && now - lastSeen > 15000) {
                        markDisconnected(client);
                        try { client.close(); } catch (IOException ignored) {}
                        continue;
                    }
                    try {
                        client.sendToClient(new Message(Message.PING, null));
                    } catch (IOException ex) {
                        markDisconnected(client);
                    }
                }
            }
        });
        hb.setDaemon(true);
        hb.start();
    }
    
    

    @Override
    protected void serverStopped() {
        ui.display("Server: Listener stopped.");
        scheduler.stop();
        db.disconnect();
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        liveClients.add(client);
        ui.display("Client connected: " + client.getInetAddress().getHostAddress());
        notifyConnectionsChanged();
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        markDisconnected(client);
    }

    @Override
    protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
        markDisconnected(client);
    }

    private void markDisconnected(ConnectionToClient client) {
        if (client.getInfo("Disconnected") == null) {
            client.setInfo("Disconnected", true);
            liveClients.remove(client);
            ui.display("Client disconnected: " + client.getInetAddress().getHostAddress());
            notifyConnectionsChanged();
            
            // Clean up logged in users associated with this connection
            String username = (String) client.getInfo("Username");
            if (username != null) {
                db.setLoginStatus(username, false);
                ui.display("Logged out user: " + username);
            }
        }
    }

    private void notifyConnectionsChanged() {
        if (onConnectionsChanged != null) {
            onConnectionsChanged.run();
        }
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {//a ConnectionToClient handle representing which client sent it
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

    private void updateReservationDetails(Reservation res) {
        db.updateReservationVisitorsAndPrice(res.getReservationId(), res.getNumberOfVisitors(), res.getPrice(), res.getPaymentStatus());
    }

    /**
     * Calculates the discounted price based on the pricing matrix:
     * - Standard visit full price: 100% (50.0 NIS) per person
     * - Individual/Family reserved: 15% discount
     * - Individual/Family spontaneous: Full price (0% discount)
     * - Group reserved: 25% discount, + 12% additional discount if paid in advance. Guide is free (n-1 visitors pay).
     * - Group spontaneous: 10% discount. Guide pays (n visitors pay).
     * - Subscribers: additional 10% discount (compounded on total).
     */
    private double calculatePrice(Reservation res, double basePrice, double promoDiscount) {
        int visitors = res.getNumberOfVisitors();
        double pricePerPerson = basePrice;
        
        // Apply promotional discount if active (additional discount, e.g., subtracted from rate)
        double rateDiscount = 0.0;
        if (promoDiscount > 0) {
            rateDiscount = promoDiscount;
        }

        double groupSubtotal = 0.0;

        switch (res.getReservationType()) {
            case "INDIVIDUAL":
                if ("CONFIRMED".equals(res.getStatus()) || "PENDING_CONFIRMATION".equals(res.getStatus()) || "WAITING_LIST".equals(res.getStatus())) {
                    // Reserved in advance: 15% discount
                    double discount = 0.15 + rateDiscount;
                    groupSubtotal = visitors * pricePerPerson * (1.0 - Math.min(discount, 1.0));
                } else {
                    // Spontaneous: full price
                    double discount = rateDiscount;
                    groupSubtotal = visitors * pricePerPerson * (1.0 - Math.min(discount, 1.0));
                }
                break;
            case "FAMILY_SUBSCRIBER":
                // Subscribers get the individual reserved discount (15%) + subscriber discount (10% compound)
                if ("CONFIRMED".equals(res.getStatus()) || "PENDING_CONFIRMATION".equals(res.getStatus()) || "WAITING_LIST".equals(res.getStatus())) {
                    double discount = 0.15 + rateDiscount;
                    double subtotal = visitors * pricePerPerson * (1.0 - Math.min(discount, 1.0));
                    // Additional 10% compound discount
                    groupSubtotal = subtotal * 0.90;
                } else {
                    // Spontaneous subscriber: full price minus subscriber 10% compound
                    double subtotal = visitors * pricePerPerson * (1.0 - Math.min(rateDiscount, 1.0));
                    groupSubtotal = subtotal * 0.90;
                }
                break;
            case "ORGANIZED_GROUP":
                // Group is max 15 people
                if ("CONFIRMED".equals(res.getStatus()) || "PENDING_CONFIRMATION".equals(res.getStatus()) || "WAITING_LIST".equals(res.getStatus())) {
                    // Reserved group: 25% discount. Guide does not pay (visitors - 1).
                    // If advance payment: additional 12% discount.
                    double baseDiscount = 0.25 + rateDiscount;
                    int payingCount = Math.max(1, visitors - 1);
                    double subtotal = payingCount * pricePerPerson * (1.0 - Math.min(baseDiscount, 1.0));
                    
                    if ("PAID_IN_ADVANCE".equals(res.getPaymentStatus())) {
                        groupSubtotal = subtotal * 0.88; // 12% additional discount
                    } else {
                        groupSubtotal = subtotal;
                    }
                } else {
                    // Spontaneous group: 10% discount. Guide pays.
                    double discount = 0.10 + rateDiscount;
                    groupSubtotal = visitors * pricePerPerson * (1.0 - Math.min(discount, 1.0));
                }
                break;
            default:
                groupSubtotal = visitors * pricePerPerson;
        }
        
        return Math.round(groupSubtotal * 100.0) / 100.0; // Round to 2 decimal places
    }
}
