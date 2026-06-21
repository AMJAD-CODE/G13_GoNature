package server;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.ChatIF;
import common.Message;
//import common.User; to be added
import common.Park;
import common.Reservation;
import common.Subscriber;
import common.User;
import common.Promotion;
import db.DatabaseController;
//import db.DatabaseConfig; to be added
import db.DatabaseConfig;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class GoNatureServer extends AbstractServer {

    private final DatabaseController db;
    private final ChatIF ui;
    private final SimulationScheduler scheduler; // Added in Commit 3 for automated queue management
    private Runnable onConnectionsChanged;

    public GoNatureServer(int port, ChatIF ui) {
        super(port);
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
        
        // Initialize config and connect to DB dynamically (Commit 2 feature)
        DatabaseConfig config = new DatabaseConfig();
        config.loadConfig();

        if (db.connect(config.getDbHost(), config.getDbName(), config.getDbUser(), config.getDbPassword())) {
            ui.display("Server: Database connected successfully.");
            scheduler.start(); // Active automated timers
        } else {
            ui.display("Server ERROR: Database connection failed. Queries will fail.");
            ui.display("Server ERROR: Database connection failed.");
        }
    }

    @Override
    protected void serverStopped() {
        ui.display("Server: Listener stopped.");
        scheduler.stop();
        db.disconnect();
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        ui.display("Client connected: " + client.getInetAddress().getHostAddress());
        notifyConnectionsChanged();
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        ui.display("Client disconnected: " + client.getInetAddress().getHostAddress());
        
        String username = (String) client.getInfo("Username");
        if (username != null) {
            db.setLoginStatus(username, false);
            ui.display("Automatically logged out user: " + username);
        markDisconnected(client);
    }

    @Override
    protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
        markDisconnected(client);
    }

    private void markDisconnected(ConnectionToClient client) {
        if (client.getInfo("Disconnected") == null) {
            client.setInfo("Disconnected", true);
            ui.display("Client disconnected: " + client.getInetAddress().getHostAddress());
            notifyConnectionsChanged();
            
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

@@ -70,16 +107,20 @@ protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        try {
            Message response = processRequest(request, client);
            client.sendToClient(response);
        } catch (IOException e) {
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
        if (!db.isConnected()) {
            return new Message(Message.ERROR, "Database is NOT connected to the server.");
        }
        
        switch (request.getAction()) {
            case Message.LOGIN: {
                String[] creds = (String[]) request.getPayload();
@@ -97,8 +138,314 @@ private Message processRequest(Message request, ConnectionToClient client) {
                client.setInfo("Username", null);
                return new Message(Message.OK, "Logged out successfully");
            }
            case Message.GET_PARK: {
                int parkId = (Integer) request.getPayload();
                Park p = db.getPark(parkId);
                if (p != null) return new Message(Message.OK, p);
                return new Message(Message.ERROR, "Park not found");
            }
            case Message.GET_ALL_PARKS: {
                List<Park> all = db.getAllParks();
                return new Message(Message.OK, new ArrayList<>(all));
            }
            case Message.UPDATE_PARK_PARAMETERS: {
                Park p = (Park) request.getPayload();
                if (db.updateParkParameters(p)) {
                    return new Message(Message.OK, "Park parameters updated successfully");
                }
                return new Message(Message.ERROR, "Failed to update parameters");
            }
            case Message.GET_PENDING_PARKS: {
                List<Park> pending = new ArrayList<>();
                for (Park p : db.getAllParks()) {
                    if ("PENDING_APPROVAL".equals(p.getPendingChangesStatus())) {
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
                }
                return new Message(Message.ERROR, "Failed to resolve parameters");
            }
            case Message.CREATE_RESERVATION: {
                Reservation res = (Reservation) request.getPayload();
                Subscriber sub = db.getSubscriberById(res.getVisitorId());
                if (sub != null) {
                    res.setReservationType("FAMILY_SUBSCRIBER");
                }
                
                boolean hasSpace = db.checkCapacityAvailable(res.getParkId(), res.getVisitDateTime(), res.getNumberOfVisitors());
                if (!hasSpace) {
                    return new Message(Message.ERROR, "FULL");
                }
                
                res.setStatus("CONFIRMED");
                if (res.getPaymentStatus() == null) {
                    res.setPaymentStatus("UNPAID");
                }
                
                double basePrice = 50.0;
                double finalPrice = calculatePrice(res, basePrice, db.getActivePromotionDiscount(res.getParkId(), res.getVisitDateTime()));
                res.setPrice(finalPrice);
                
                Reservation saved = db.createReservation(res);
                if (saved != null) return new Message(Message.OK, saved);
                return new Message(Message.ERROR, "Database error creating reservation.");
            }
            case Message.ENTER_WAITING_LIST: {
                Reservation res = (Reservation) request.getPayload();
                Subscriber sub = db.getSubscriberById(res.getVisitorId());
                if (sub != null) {
                    res.setReservationType("FAMILY_SUBSCRIBER");
                }
                res.setStatus("WAITING_LIST");
                res.setPaymentStatus("UNPAID");
                
                double finalPrice = calculatePrice(res, 50.0, db.getActivePromotionDiscount(res.getParkId(), res.getVisitDateTime()));
                res.setPrice(finalPrice);
                Reservation saved = db.createReservation(res);
                if (saved != null) return new Message(Message.OK, saved);
                return new Message(Message.ERROR, "Failed to join waiting list");
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
                if (r != null) return new Message(Message.OK, r);
                return new Message(Message.ERROR, "Reservation not found");
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
                }
                return new Message(Message.ERROR, "Reservation not found");
            }
            case Message.CONFIRM_RESERVATION: {
                int resId = (Integer) request.getPayload();
                if (db.updateReservationStatus(resId, "CONFIRMED", SimulationScheduler.getSimulatedTimestamp())) {
                    return new Message(Message.OK, "Confirmed successfully");
                }
                return new Message(Message.ERROR, "Failed to confirm");
            }
            case Message.REGISTER_ENTRY: {
                Object[] payload = (Object[]) request.getPayload();
                String idOrCode = (String) payload[0];
                int actualVisitors = (Integer) payload[1];
                int parkId = (Integer) payload[2];
                
                Reservation res = null;
                try {
                    int code = Integer.parseInt(idOrCode);
                    res = db.getReservation(code);
                } catch (NumberFormatException ignored) {}
                
                if (res == null) {
                    List<Reservation> activeRes = db.getReservationsByVisitorId(idOrCode);
                    for (Reservation r : activeRes) {
                        if ("CONFIRMED".equals(r.getStatus()) || "PENDING_CONFIRMATION".equals(r.getStatus())) {
                            res = r;
                            break;
                        }
                    }
                }
                
                if (res != null) {
                    if (res.getParkId() != parkId) {
                        return new Message(Message.ERROR, "Reservation is for another park: " + res.getParkName());
                    }
                    int occupancy = db.getParkCurrentOccupancy(parkId);
                    Park p = db.getPark(parkId);
                    if (occupancy + actualVisitors > p.getCurrentQuota()) {
                        return new Message(Message.ERROR, "Park is full. Cannot enter even with reservation.");
                    }
                    
                    res.setNumberOfVisitors(actualVisitors);
                    double finalPrice = calculatePrice(res, 50.0, db.getActivePromotionDiscount(parkId, SimulationScheduler.getSimulatedTimestamp()));
                    res.setPrice(finalPrice);
                    res.setPaymentStatus("PAID_AT_ENTRANCE");
                    res.setStatus("ACTIVE");
                    
                    db.updateReservationStatus(res.getReservationId(), "ACTIVE", SimulationScheduler.getSimulatedTimestamp());
                    db.updateReservationVisitorsAndPrice(res.getReservationId(), res.getNumberOfVisitors(), res.getPrice(), res.getPaymentStatus());
                    db.logOccupancyChange(parkId, occupancy + actualVisitors, SimulationScheduler.getSimulatedTimestamp());
                    return new Message(Message.OK, res);
                } else {
                    // SPONTANEOUS ENTRY
                    int occupancy = db.getParkCurrentOccupancy(parkId);
                    Park p = db.getPark(parkId);
                    if (occupancy + actualVisitors > p.getCurrentQuota()) {
                        return new Message(Message.ERROR, "Park is full. Spontaneous entry denied.");
                    }
                    
                    Reservation spotRes = new Reservation();
                    spotRes.setVisitorId(idOrCode);
                    spotRes.setParkId(parkId);
                    spotRes.setVisitDateTime(SimulationScheduler.getSimulatedTimestamp());
                    spotRes.setNumberOfVisitors(actualVisitors);
                    spotRes.setEmail("spontaneous@gonature.gov.il");
                    spotRes.setPhoneNumber("000-0000");
                    
                    Subscriber sub = db.getSubscriberById(idOrCode);
                    User guide = db.loginUser(idOrCode, "");
                    
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
                    }
                    return new Message(Message.ERROR, "Failed to create spontaneous reservation.");
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
                    List<Reservation> active = db.getReservationsByVisitorId(idOrCode);
                    for (Reservation r : active) {
                        if ("ACTIVE".equals(r.getStatus())) {
                            res = r;
                            break;
                        }
                    }
                }
                
                if (res != null && !"ACTIVE".equals(res.getStatus())) {
                    res = db.getReservation(res.getReservationId());
                }
                
                if (res != null && "ACTIVE".equals(res.getStatus())) {
                    db.updateReservationStatus(res.getReservationId(), "COMPLETED", SimulationScheduler.getSimulatedTimestamp());
                    int parkId = res.getParkId();
                    int occupancy = db.getParkCurrentOccupancy(parkId);
                    db.logOccupancyChange(parkId, occupancy, SimulationScheduler.getSimulatedTimestamp());
                    scheduler.promoteNextWaiting(parkId, res.getVisitDateTime());
                    return new Message(Message.OK, "Exit registered successfully.");
                }
                return new Message(Message.ERROR, "No active visit found.");
            }
            case Message.GET_MONTHLY_VISITOR_REPORT: {
                Object[] payload = (Object[]) request.getPayload();
                int parkId = (Integer) payload[0];
                int month = (Integer) payload[1];
                int year = (Integer) payload[2];
                Map<String, Integer> data = db.getMonthlyVisitorReport(parkId, month, year);
                return new Message(Message.OK, new HashMap<>(data));
            }
            case Message.CREATE_PROMOTION: {
                Promotion p = (Promotion) request.getPayload();
                if (db.createPromotion(p)) return new Message(Message.OK, "Promotion submitted");
                return new Message(Message.ERROR, "Failed to create promotion");
            }
            case Message.GET_PENDING_PROMOTIONS: {
                List<Promotion> pending = db.getPendingPromotions();
                return new Message(Message.OK, new ArrayList<>(pending));
            }
            case Message.APPROVE_PROMOTION: {
                Object[] payload = (Object[]) request.getPayload();
                int promoId = (Integer) payload[0];
                boolean approve = (Boolean) payload[1];
                if (db.approvePromotion(promoId, approve)) return new Message(Message.OK, "Promotion resolved");
                return new Message(Message.ERROR, "Failed to resolve promotion");
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

    private double calculatePrice(Reservation res, double basePrice, double promoDiscount) {
        int visitors = res.getNumberOfVisitors();
        double pricePerPerson = basePrice;
        double rateDiscount = promoDiscount > 0 ? promoDiscount : 0.0;
        double groupSubtotal = 0.0;

        switch (res.getReservationType()) {
            case "INDIVIDUAL":
                if ("CONFIRMED".equals(res.getStatus()) || "PENDING_CONFIRMATION".equals(res.getStatus()) || "WAITING_LIST".equals(res.getStatus())) {
                    double discount = 0.15 + rateDiscount;
                    groupSubtotal = visitors * pricePerPerson * (1.0 - Math.min(discount, 1.0));
                } else {
                    groupSubtotal = visitors * pricePerPerson * (1.0 - Math.min(rateDiscount, 1.0));
                }
                break;
            case "FAMILY_SUBSCRIBER":
                if ("CONFIRMED".equals(res.getStatus()) || "PENDING_CONFIRMATION".equals(res.getStatus()) || "WAITING_LIST".equals(res.getStatus())) {
                    double discount = 0.15 + rateDiscount;
                    double subtotal = visitors * pricePerPerson * (1.0 - Math.min(discount, 1.0));
                    groupSubtotal = subtotal * 0.90;
                } else {
                    double subtotal = visitors * pricePerPerson * (1.0 - Math.min(rateDiscount, 1.0));
                    groupSubtotal = subtotal * 0.90;
                }
                break;
            case "ORGANIZED_GROUP":
                if ("CONFIRMED".equals(res.getStatus()) || "PENDING_CONFIRMATION".equals(res.getStatus()) || "WAITING_LIST".equals(res.getStatus())) {
                    double baseDiscount = 0.25 + rateDiscount;
                    int payingCount = Math.max(1, visitors - 1);
                    double subtotal = payingCount * pricePerPerson * (1.0 - Math.min(baseDiscount, 1.0));
                    if ("PAID_IN_ADVANCE".equals(res.getPaymentStatus())) {
                        groupSubtotal = subtotal * 0.88;
                    } else {
                        groupSubtotal = subtotal;
                    }
                } else {
                    double discount = 0.10 + rateDiscount;
                    groupSubtotal = visitors * pricePerPerson * (1.0 - Math.min(discount, 1.0));
                }
                break;
            default:
                return new Message(Message.ERROR, "Unknown server command in current sprint: " + request.getAction());
                groupSubtotal = visitors * pricePerPerson;
        }
        return Math.round(groupSubtotal * 100.0) / 100.0;
    }
}