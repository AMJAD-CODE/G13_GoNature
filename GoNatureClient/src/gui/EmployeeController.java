package gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import client.ClientUI;
import common.Message;
import common.Park;
import common.Reservation;

/**
 * Controller for the employee screen. Handles park capacity display,
 * the simulated clock, visitor entry lookup and check-in, billing,
 * exit check-out, spontaneous (walk-in) entries, and QR scan simulation.
 */
public class EmployeeController {

    @FXML
    private Label parkLabel;
    @FXML
    private Label employeeLabel;
    @FXML
    private Label capacityLabel;
    @FXML
    private Label statusLabel;

    @FXML
    private Label simClockLabel;
    private javafx.animation.Timeline clockTimeline;
    private long simStartMs = 0;
    private double simSpeedup = 1.0;
    private long clientSyncTime = 0;


    // Entrance lookup
    @FXML
    private TextField entrySearchField;
    
    // Details panel
    @FXML
    private VBox detailsBox;
    @FXML
    private Label detailResId;
    @FXML
    private Label detailVisitorId;
    @FXML
    private Label detailType;
    @FXML
    private Label detailSize;
    @FXML
    private Label detailStatus;
    @FXML
    private TextField actualVisitorsField;

    // Billing panel
    @FXML
    private VBox billBox;
    @FXML
    private Label billTextLabel;

    // Exit panel
    @FXML
    private TextField exitSearchField;

    // Spontaneous panel
    @FXML
    private TextField spontIdField;
    @FXML
    private TextField spontCountField;

    private Park currentPark = null;
    private int currentOccupancy = 0;
    private Reservation activeLookupRes = null;

    /**
     * Initializes the controller: shows the logged-in employee's name,
     * loads the assigned park's capacity, and starts the simulated clock.
     */
    @FXML
    public void initialize() {
        employeeLabel.setText("Employee: " + ClientUI.currentUser.getUsername());
        
        // Load assigned park details
        refreshCapacity();

        // Init simulated clock
        initSimClock();
    }


    /**
     * Reloads the assigned park's details and current occupancy from the
     * server and updates the park/capacity labels. Shows an error if the
     * employee has no assigned park or the data can't be loaded.
     */
    @FXML
    public void refreshCapacity() {
        if (ClientUI.currentUser.getAssignedParkId() == null) {
            statusLabel.setText("ERROR: No assigned park for this employee.");
            return;
        }

        int parkId = ClientUI.currentUser.getAssignedParkId();
        
        // Get Park quotas
        Message pResp = ClientUI.client.sendRequest(new Message(Message.GET_PARK, parkId));
        if (Message.OK.equals(pResp.getAction())) {
            currentPark = (Park) pResp.getPayload();
            parkLabel.setText("Park: " + currentPark.getParkName());
        } else {
            statusLabel.setText("ERROR loading park details.");
            return;
        }

        // Get current occupancy
        Message occResp = ClientUI.client.sendRequest(new Message(Message.GET_OCCUPANCY, parkId));
        if (Message.OK.equals(occResp.getAction())) {
            currentOccupancy = (Integer) occResp.getPayload();
            int limit = currentPark.getCurrentQuota() - currentPark.getReservedGap();
            capacityLabel.setText("Occupancy: " + currentOccupancy + " / " + currentPark.getCurrentQuota() + 
                                  " (Reservation Limit: " + limit + ")");
            statusLabel.setText("Park occupancy updated.");
        } else {
            statusLabel.setText("ERROR loading occupancy details.");
        }
    }

    /**
     * Looks up a reservation by code or visitor ID for entrance check-in.
     * Validates that the reservation belongs to the current park, is within
     * the allowed check-in window, and is in a checkable status, then
     * populates the details panel.
     */
    @FXML
    public void onLookupEntry() {
        String search = entrySearchField.getText().trim();
        if (search.isEmpty()) {
            statusLabel.setText("ERROR: Please input a reservation code or visitor ID.");
            return;
        }

        detailsBox.setVisible(false);
        billBox.setVisible(false);
        activeLookupRes = null;

        Reservation found = null;
        
        // Fetch simulated time from server to validate the date
        Message timeResp = ClientUI.client.sendRequest(new Message(Message.GET_SIMULATION_TIME, null));
        Timestamp simToday = new Timestamp(System.currentTimeMillis());
        if (Message.OK.equals(timeResp.getAction())) {
            Object[] tPayload = (Object[]) timeResp.getPayload();
            long simStartMs = (Long) tPayload[0];
            double simSpeedup = (Double) tPayload[1];
            long elapsedReal = System.currentTimeMillis() - simStartMs;
            long currentSim = simStartMs + (long)(elapsedReal * simSpeedup);
            simToday = new Timestamp(currentSim);
        }
        final Timestamp finalSimToday = simToday;
        
        // Try searching by Reservation Code first
        try {
            int code = Integer.parseInt(search);
            Message resp = ClientUI.client.sendRequest(new Message(Message.GET_RESERVATION, code));
            if (Message.OK.equals(resp.getAction())) {
                found = (Reservation) resp.getPayload();
            }
        } catch (NumberFormatException ignored) {}

        // If not found by code, try searching by Visitor ID
        if (found == null) {
            Message resp = ClientUI.client.sendRequest(new Message(Message.GET_RESERVATIONS_BY_ID, search));
            if (Message.OK.equals(resp.getAction())) {
                @SuppressWarnings("unchecked")
                List<Reservation> resList = (List<Reservation>) resp.getPayload();
                for (Reservation r : resList) {
                    if (("CONFIRMED".equals(r.getStatus()) || "PENDING_CONFIRMATION".equals(r.getStatus()))) {
                        long visitTime = r.getVisitDateTime().getTime();
                        long simTime = finalSimToday.getTime();
                        if (simTime >= visitTime - 30 * 60 * 1000L && simTime <= visitTime + 90 * 60 * 1000L) {
                            found = r;
                            break;
                        }
                    }
                }
            }
        }

        if (found != null) {
            long visitTime = found.getVisitDateTime().getTime();
            long simTime = finalSimToday.getTime();
            if (simTime < visitTime - 30 * 60 * 1000L || simTime > visitTime + 90 * 60 * 1000L) {
                statusLabel.setText("ERROR: Check-in is only allowed between 30 minutes before and 90 minutes after the scheduled slot.");
                return;
            }
        }

        if (found != null) {
            if (found.getParkId() != currentPark.getParkId()) {
                statusLabel.setText("ERROR: Reservation belongs to another park: " + found.getParkName());
                return;
            }
            
            // Check status is valid for check-in
            if (!"CONFIRMED".equals(found.getStatus()) && !"PENDING_CONFIRMATION".equals(found.getStatus())) {
                statusLabel.setText("ERROR: Reservation is not in a check-in state (Status: " + found.getStatus() + ")");
                return;
            }

            activeLookupRes = found;
            
            detailResId.setText(String.valueOf(found.getReservationId()));
            detailVisitorId.setText(found.getVisitorId());
            detailType.setText(found.getReservationType());
            detailSize.setText(String.valueOf(found.getNumberOfVisitors()));
            detailStatus.setText(found.getStatus());
            
            actualVisitorsField.setText(String.valueOf(found.getNumberOfVisitors()));
            
            detailsBox.setVisible(true);
            statusLabel.setText("Reservation found.");
        } else {
            statusLabel.setText("ERROR: No active reservation found for code/ID: " + search);
        }
    }

    /**
     * Registers entry for the currently looked-up reservation using the
     * entered actual visitor count, then displays the generated bill.
     */
    @FXML
    public void onProcessEntryBill() {
        if (activeLookupRes == null) return;

        String actualStr = actualVisitorsField.getText().trim();
        int actualCount;
        try {
            actualCount = Integer.parseInt(actualStr);
            if (actualCount <= 0 || actualCount > activeLookupRes.getNumberOfVisitors()) {
                statusLabel.setText("ERROR: Actual visitors count must be between 1 and " + activeLookupRes.getNumberOfVisitors());
                return;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("ERROR: Invalid visitors count.");
            return;
        }

        // Send register entry request
        Object[] payload = new Object[]{String.valueOf(activeLookupRes.getReservationId()), actualCount, currentPark.getParkId()};
        Message response = ClientUI.client.sendRequest(new Message(Message.REGISTER_ENTRY, payload));

        if (Message.OK.equals(response.getAction())) {
            Reservation checkedIn = (Reservation) response.getPayload();
            String breakdown = (checkedIn.getPriceBreakdown() != null) ? "\n" + checkedIn.getPriceBreakdown() : "";
            billTextLabel.setText("Invoice Total: " + checkedIn.getPrice() + " NIS (" + checkedIn.getReservationType() + ")" + breakdown);
            billBox.setVisible(true);
            statusLabel.setText("Entry registered. Bill generated.");

        } else {
            showAlert("Error", "Check-in Failed", response.getPayload().toString());
        }
    }

    /**
     * Confirms payment for the displayed bill, clears the entry/details
     * panels, and refreshes park capacity.
     */
    @FXML
    public void onConfirmCheckIn() {
        // Payment collected, clear screen and update capacity
        showAlert("Success", "Entrance Confirmed", "Visitor entry completed. Bill marked as PAID.");
        detailsBox.setVisible(false);
        billBox.setVisible(false);
        entrySearchField.clear();
        activeLookupRes = null;
        refreshCapacity();
    }

    /**
     * Registers an exit for the visitor ID or reservation code entered in
     * the exit field, then refreshes park capacity.
     */
    @FXML
    public void onLookupExit() {
        String code = exitSearchField.getText().trim();
        if (code.isEmpty()) {
            statusLabel.setText("ERROR: Please enter a visitor ID or reservation code to check out.");
            return;
        }

        Message response = ClientUI.client.sendRequest(new Message(Message.REGISTER_EXIT, code));
        if (Message.OK.equals(response.getAction())) {
            showAlert("Success", "Check-Out Registered", response.getPayload().toString());
            exitSearchField.clear();
            refreshCapacity();
        } else {
            showAlert("Error", "Check-Out Failed", response.getPayload().toString());
        }
    }

    /**
     * Registers a walk-in (no reservation) entry for the given visitor ID
     * and group size, after checking the park is within operating hours.
     * Shows the generated bill on success.
     */
    @FXML
    public void onSpontaneousEntry() {
        String visitorId = spontIdField.getText().trim();
        String countStr = spontCountField.getText().trim();

        if (visitorId.isEmpty() || countStr.isEmpty()) {
            statusLabel.setText("ERROR: Please fill in all spontaneous entry fields.");
            return;
        }

        // Get simulated time from server to validate the operating hours
        Message timeResp = ClientUI.client.sendRequest(new Message(Message.GET_SIMULATION_TIME, null));
        Timestamp simToday = new Timestamp(System.currentTimeMillis());
        if (Message.OK.equals(timeResp.getAction())) {
            Object[] tPayload = (Object[]) timeResp.getPayload();
            long simStartMs = (Long) tPayload[0];
            double simSpeedup = (Double) tPayload[1];
            long elapsedReal = System.currentTimeMillis() - simStartMs;
            long currentSim = simStartMs + (long)(elapsedReal * simSpeedup);
            simToday = new Timestamp(currentSim);
        }
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(simToday.getTime());
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 8 || hour > 20) {
            statusLabel.setText("ERROR: Park is closed. Spontaneous entry only allowed between 08:00 and 20:00.");
            showAlert("Entry Denied", "Park Closed", "Spontaneous entries are only permitted during active operating hours (08:00 - 20:00).");
            return;
        }

        int count;
        try {
            count = Integer.parseInt(countStr);
            if (count <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            statusLabel.setText("ERROR: Spontaneous group size must be a positive integer.");
            return;
        }

        // Spontaneous check-in request
        Object[] payload = new Object[]{visitorId, count, currentPark.getParkId()};
        Message response = ClientUI.client.sendRequest(new Message(Message.REGISTER_ENTRY, payload));

        if (Message.OK.equals(response.getAction())) {
            Reservation saved = (Reservation) response.getPayload();
            String breakdown = (saved.getPriceBreakdown() != null) ? "\n\nPrice Calculation Breakdown:\n" + saved.getPriceBreakdown() : "";
            showAlert("Spontaneous Entry Approved", "Payment Invoice Generated",
                      "Entry Approved!\n" +
                      "Generated Code: " + saved.getReservationId() + "\n" +
                      "Type: " + saved.getReservationType() + "\n" +
                      "Bill Invoice Total: " + saved.getPrice() + " NIS." + breakdown + "\n\n" +
                      "Payment is collected at the desk.");

            
            spontIdField.clear();
            spontCountField.clear();
            refreshCapacity();
        } else {
            showAlert("Spontaneous Entry Rejected", "Capacity Limit Exceeded", response.getPayload().toString());
        }
    }

    /**
     * Simulates scanning a visitor's QR code at the entrance by letting the
     * employee pick from the park's confirmed/pending reservations, then
     * auto-fills the entry search field and runs the lookup.
     */
    @FXML
    public void onSimulateEntryQRScan() {
        if (currentPark == null) {
            statusLabel.setText("ERROR: No active park loaded.");
            return;
        }
        
        // Fetch all pending/confirmed reservations for this park
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_PARK_RESERVATIONS, currentPark.getParkId()));
        if (Message.OK.equals(response.getAction())) {
            @SuppressWarnings("unchecked")
            List<Reservation> list = (List<Reservation>) response.getPayload();
            List<String> options = new ArrayList<>();
            for (Reservation r : list) {
                if ("CONFIRMED".equals(r.getStatus()) || "PENDING_CONFIRMATION".equals(r.getStatus())) {
                    options.add("Code: " + r.getReservationId() + " (Visitor: " + r.getVisitorId() + ", Size: " + r.getNumberOfVisitors() + ")");
                }
            }
            
            if (options.isEmpty()) {
                showAlert("QR Code Scanner Simulator", "No Reservations Found", "There are no confirmed/pending reservations for today to scan.");
                return;
            }
            
            ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
            dialog.setTitle("QR Code Scanner Simulator - Entrance");
            dialog.setHeaderText("Simulate scanning visitor QR Code");
            dialog.setContentText("Select reservation to scan:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                // Parse out the code
                String sel = result.get();
                String codeStr = sel.substring(6, sel.indexOf(" ("));
                entrySearchField.setText(codeStr);
                statusLabel.setText("QR Code scanned successfully: Reservation Code " + codeStr);
                // Auto trigger lookup
                onLookupEntry();
            }
        } else {
            showAlert("Error", "Failed to fetch reservations", response.getPayload().toString());
        }
    }

    /**
     * Simulates scanning a visitor's QR code at the exit by letting the
     * employee pick from currently active visits, then auto-fills the exit
     * search field and runs check-out.
     */
    @FXML
    public void onSimulateExitQRScan() {
        if (currentPark == null) {
            statusLabel.setText("ERROR: No active park loaded.");
            return;
        }
        
        // Fetch all active/checked-in visits
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_PARK_RESERVATIONS, currentPark.getParkId()));
        if (Message.OK.equals(response.getAction())) {
            @SuppressWarnings("unchecked")
            List<Reservation> list = (List<Reservation>) response.getPayload();
            List<String> options = new ArrayList<>();
            for (Reservation r : list) {
                if ("ACTIVE".equals(r.getStatus())) {
                    options.add("Code: " + r.getReservationId() + " (Visitor: " + r.getVisitorId() + ", Size: " + r.getNumberOfVisitors() + ")");
                }
            }
            
            if (options.isEmpty()) {
                showAlert("QR Code Scanner Simulator", "No Active Visits", "There are no visitors currently inside the park (ACTIVE status) to scan.");
                return;
            }
            
            ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
            dialog.setTitle("QR Code Scanner Simulator - Exit");
            dialog.setHeaderText("Simulate scanning visitor QR Code for Checkout");
            dialog.setContentText("Select visitor to scan out:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                // Parse out the code
                String sel = result.get();
                String codeStr = sel.substring(6, sel.indexOf(" ("));
                exitSearchField.setText(codeStr);
                statusLabel.setText("Exit QR Code scanned successfully: " + codeStr);
                // Auto trigger exit registration
                onLookupExit();
            }
        } else {
            showAlert("Error", "Failed to fetch reservations", response.getPayload().toString());
        }
    }

    /**
     * Stops the simulated clock, logs the employee out on the server, and
     * returns to the login screen.
     */
    @FXML
    public void onLogout() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        ClientUI.client.sendRequest(new Message(Message.LOGOUT, ClientUI.currentUser.getUsername()));
        ClientUI.currentUser = null;
        ClientUI.setRoot("/gui/LoginUI.fxml", "GoNature - Login Portal", 500, 670);
    }

    /**
     * Fetches the server's simulated time and speedup factor and starts
     * the local clock display timeline.
     */
    private void initSimClock() {
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_SIMULATION_TIME, null));
        if (Message.OK.equals(response.getAction())) {
            Object[] payload = (Object[]) response.getPayload();
            simStartMs = (Long) payload[0];
            simSpeedup = (Double) payload[1];
            clientSyncTime = System.currentTimeMillis();
            startClockTimeline();
        }
    }

    /**
     * Starts (or restarts) the recurring timeline that updates the
     * simulated clock label every 250ms based on the synced server time
     * and speedup factor.
     */
    private void startClockTimeline() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        clockTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(250), event -> {
            long now = System.currentTimeMillis();
            long elapsedReal = now - clientSyncTime;
            long elapsedSim = (long)(elapsedReal * simSpeedup);
            long currentSim = getSimulatedTimeAtSync() + elapsedSim;
            
            java.sql.Timestamp ts = new java.sql.Timestamp(currentSim);
            java.time.LocalDateTime ldt = ts.toLocalDateTime();
            String formatted = ldt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            if (simClockLabel != null) {
                simClockLabel.setText("Simulated Time: " + formatted);
            }
        }));
        clockTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        clockTimeline.play();
    }

    /**
     * Computes the simulated time at the moment of the last server sync.
     *
     * @return simulated time in milliseconds at sync time
     */
    private long getSimulatedTimeAtSync() {
        long elapsedReal = clientSyncTime - simStartMs;
        return simStartMs + (long)(elapsedReal * simSpeedup);
    }

    /**
     * Checks whether two timestamps fall on the same calendar day.
     *
     * @param ts1 first timestamp
     * @param ts2 second timestamp
     * @return true if both are non-null and on the same day
     */
    private boolean isSameDay(Timestamp ts1, Timestamp ts2) {
        if (ts1 == null || ts2 == null) return false;
        return new SimpleDateFormat("yyyy-MM-dd").format(ts1).equals(new SimpleDateFormat("yyyy-MM-dd").format(ts2));
    }

    /**
     * Shows a blocking informational alert dialog.
     *
     * @param title   dialog window title
     * @param header  dialog header text
     * @param content dialog body text
     */
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
