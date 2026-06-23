package gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import client.ClientUI;
import common.Message;
import common.Park;
import common.Reservation;

public class EmployeeController {

    @FXML
    private Label parkLabel;
    @FXML
    private Label employeeLabel;
    @FXML
    private Label capacityLabel;
    @FXML
    private Label statusLabel;

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

    @FXML
    public void initialize() {
        employeeLabel.setText("Employee: " + ClientUI.currentUser.getUsername());
        
        // Load assigned park details
        refreshCapacity();
    }

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
                    if ("CONFIRMED".equals(r.getStatus()) || "PENDING_CONFIRMATION".equals(r.getStatus())) {
                        found = r;
                        break;
                    }
                }
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
            billTextLabel.setText("Invoice Total: " + checkedIn.getPrice() + " NIS (" + checkedIn.getReservationType() + ")");
            billBox.setVisible(true);
            statusLabel.setText("Entry registered. Bill generated.");
        } else {
            showAlert("Error", "Check-in Failed", response.getPayload().toString());
        }
    }

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

    @FXML
    public void onSpontaneousEntry() {
        String visitorId = spontIdField.getText().trim();
        String countStr = spontCountField.getText().trim();

        if (visitorId.isEmpty() || countStr.isEmpty()) {
            statusLabel.setText("ERROR: Please fill in all spontaneous entry fields.");
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
            showAlert("Spontaneous Entry Approved", "Payment Invoice Generated",
                      "Entry Approved!\n" +
                      "Generated Code: " + saved.getReservationId() + "\n" +
                      "Type: " + saved.getReservationType() + "\n" +
                      "Bill Invoice Total: " + saved.getPrice() + " NIS.\n" +
                      "Payment is collected at the desk.");
            
            spontIdField.clear();
            spontCountField.clear();
            refreshCapacity();
        } else {
            showAlert("Spontaneous Entry Rejected", "Capacity Limit Exceeded", response.getPayload().toString());
        }
    }

    @FXML
    public void onLogout() {
        ClientUI.client.sendRequest(new Message(Message.LOGOUT, ClientUI.currentUser.getUsername()));
        ClientUI.currentUser = null;
        ClientUI.setRoot("/gui/LoginUI.fxml", "GoNature - Login Portal", 500, 670);
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
