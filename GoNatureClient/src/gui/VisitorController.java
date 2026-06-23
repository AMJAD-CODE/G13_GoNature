package gui;

import java.sql.Timestamp;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import client.ClientUI;
import common.Message;
import common.Park;
import common.Reservation;
import common.User;

public class VisitorController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private TableView<Reservation> reservationsTable;
    @FXML
    private TableColumn<Reservation, Integer> resIdCol;
    @FXML
    private TableColumn<Reservation, String> parkCol;
    @FXML
    private TableColumn<Reservation, String> dateTimeCol;
    @FXML
    private TableColumn<Reservation, Integer> visitorsCol;
    @FXML
    private TableColumn<Reservation, String> typeCol;
    @FXML
    private TableColumn<Reservation, Double> priceCol;
    @FXML
    private TableColumn<Reservation, String> statusCol;
    
    @FXML
    private Button confirmButton;
    @FXML
    private Button cancelButton;

    // Booking Fields
    @FXML
    private ComboBox<Park> parkComboBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> hourComboBox;
    @FXML
    private TextField visitorsField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private Label prepayLabel;
    @FXML
    private CheckBox prepayCheckBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Label simClockLabel;

    private javafx.animation.Timeline clockTimeline;
    private long simStartMs = 0;
    private double simSpeedup = 1.0;
    private long clientSyncTime = 0;

    // Waiting list section
    @FXML
    private VBox waitingListSection;
    @FXML
    private TableView<AlternateHour> alternatesTable;
    @FXML
    private TableColumn<AlternateHour, String> altHourCol;
    @FXML
    private TableColumn<AlternateHour, String> altLoadCol;

    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
    private final ObservableList<AlternateHour> alternatesList = FXCollections.observableArrayList();
    private List<Park> parksList = new ArrayList<>();
    private Reservation pendingReservationAttempt = null;

    @FXML
    public void initialize() {
        // Welcome message
        User cur = ClientUI.currentUser;
        if ("GUIDE".equals(cur.getRole())) {
            welcomeLabel.setText("Logged in as Guide: " + cur.getFirstName() + " " + cur.getLastName());
            prepayLabel.setVisible(true);
            prepayCheckBox.setVisible(true);
        } else {
            welcomeLabel.setText("Logged in as Visitor ID: " + cur.getUsername());
        }

        // Table Columns
        resIdCol.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        parkCol.setCellValueFactory(new PropertyValueFactory<>("parkName"));
        
        dateTimeCol.setCellValueFactory(cellData -> {
            Timestamp ts = cellData.getValue().getVisitDateTime();
            String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(ts);
            return new SimpleStringProperty(formatted);
        });
        
        visitorsCol.setCellValueFactory(new PropertyValueFactory<>("numberOfVisitors"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("reservationType"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        reservationsTable.setItems(reservationList);

        // Alternates Table Columns
        altHourCol.setCellValueFactory(new PropertyValueFactory<>("hour"));
        altLoadCol.setCellValueFactory(new PropertyValueFactory<>("load"));
        alternatesTable.setItems(alternatesList);

        // Enable buttons on select
        reservationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                String status = newSel.getStatus();
                confirmButton.setDisable(!"PENDING_CONFIRMATION".equals(status));
                cancelButton.setDisable("CANCELLED".equals(status) || "COMPLETED".equals(status) || "ACTIVE".equals(status));
            } else {
                confirmButton.setDisable(true);
                cancelButton.setDisable(true);
            }
        });

        // Load hours (08:00 - 20:00)
        for (int h = 8; h <= 20; h++) {
            hourComboBox.getItems().add(String.format("%02d:00", h));
        }

        // Load parks
        loadParks();
        
        // Refresh reservations
        refreshReservations();
        
        // Init simulated clock
        initSimClock();
    }

    private void loadParks() {
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_ALL_PARKS, null));
        if (Message.OK.equals(response.getAction())) {
            @SuppressWarnings("unchecked")
            List<Park> list = (List<Park>) response.getPayload();
            parksList.clear();
            parksList.addAll(list);
            parkComboBox.setItems(FXCollections.observableArrayList(parksList));
        } else {
            statusLabel.setText("ERROR: Failed to load parks: " + response.getPayload());
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            showAlert("Connection Error", "Server Request Failed", response.getPayload().toString());
        }
    }

    @FXML
    public void refreshReservations() {
        String visitorId = ClientUI.currentUser.getUsername();
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_RESERVATIONS_BY_ID, visitorId));
        if (Message.OK.equals(response.getAction())) {
            @SuppressWarnings("unchecked")
            List<Reservation> res = (List<Reservation>) response.getPayload();
            reservationList.clear();
            reservationList.addAll(res);
            statusLabel.setText("Reservations loaded successfully.");
            statusLabel.setStyle("-fx-text-fill: #7f8c8d;");
        } else {
            statusLabel.setText("ERROR: Failed to load reservations: " + response.getPayload());
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            showAlert("Connection Error", "Server Request Failed", response.getPayload().toString());
        }
    }

    @FXML
    public void onConfirmReservation() {
        Reservation sel = reservationsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        
        Message resp = ClientUI.client.sendRequest(new Message(Message.CONFIRM_RESERVATION, sel.getReservationId()));
        if (Message.OK.equals(resp.getAction())) {
            showAlert("Success", "Reservation Confirmed", "Your reservation is now fully active for the scheduled time!");
            refreshReservations();
        } else {
            showAlert("Error", "Confirmation Failed", resp.getPayload().toString());
        }
    }

    @FXML
    public void onCancelReservation() {
        Reservation sel = reservationsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Message resp = ClientUI.client.sendRequest(new Message(Message.CANCEL_RESERVATION, sel.getReservationId()));
        if (Message.OK.equals(resp.getAction())) {
            showAlert("Cancelled", "Reservation Cancelled", "Your reservation was cancelled successfully. Any pending slots have been released.");
            refreshReservations();
        } else {
            showAlert("Error", "Cancellation Failed", resp.getPayload().toString());
        }
    }

    @FXML
    public void onBookVisit() {
        Park park = parkComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String hour = hourComboBox.getValue();
        String visitorsStr = visitorsField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        //===========
        // step 1 validate all inputs
        //===========
        if (park == null || date == null || hour == null || visitorsStr.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            statusLabel.setText("ERROR: Please fill in all booking fields.");
            return;
        }

        int visitors;
        try {
            visitors = Integer.parseInt(visitorsStr);
            if (visitors <= 0) throw new NumberFormatException();
            if ("GUIDE".equals(ClientUI.currentUser.getRole()) && visitors > 15) {
                statusLabel.setText("ERROR: Organized groups are limited to max 15 people.");
                return;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("ERROR: Visitors count must be a positive integer.");
            return;
        }

        // Construct Timestamp
        int h = Integer.parseInt(hour.substring(0, 2));
        LocalDateTime ldt = LocalDateTime.of(date, LocalTime.of(h, 0));
        Timestamp visitTs = Timestamp.valueOf(ldt);

        if (visitTs.before(new Timestamp(System.currentTimeMillis()))) {
            statusLabel.setText("ERROR: Selected date & time is in the past.");
            return;
        }

        //===========
        // step 1 done
        //===========
        
        
        //===========
        // step 2 build the reservation
        //===========
        // this is why we needed the empty constructor(we use setters to make the reservation)
        // Build reservation request object
        Reservation res = new Reservation();
        res.setVisitorId(ClientUI.currentUser.getUsername());
        res.setParkId(park.getParkId());
        res.setParkName(park.getParkName());
        res.setVisitDateTime(visitTs);
        res.setNumberOfVisitors(visitors);
        res.setEmail(email);
        res.setPhoneNumber(phone);
        res.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        
        //===========
        // step 2 done
        //===========
        
        
        
        // Determine type
        if ("GUIDE".equals(ClientUI.currentUser.getRole())) {
            res.setReservationType("ORGANIZED_GROUP");
            if (prepayCheckBox.isSelected()) {
                res.setPaymentStatus("PAID_IN_ADVANCE");
            } else {
                res.setPaymentStatus("UNPAID");
            }
        } else {
            res.setReservationType("INDIVIDUAL"); // Server will upgrade if they are a subscriber
            res.setPaymentStatus("UNPAID");
        }

        //===========
        // step 3 save the reservation and reset the page
        //===========
        //save the reservation we built
        pendingReservationAttempt = res;

        // Hide waiting list panel at start of check
        waitingListSection.setVisible(false);
        waitingListSection.setManaged(false);
        alternatesTable.setVisible(false);
        alternatesTable.setManaged(false);

        //===========
        // step 4 round trip
        //===========
        Message response = ClientUI.client.sendRequest(new Message(Message.CREATE_RESERVATION, res));
        if (Message.OK.equals(response.getAction())) {
            Reservation saved = (Reservation) response.getPayload();
            showAlert("Success", "Booking Confirmed", 
                      "Reservation #" + saved.getReservationId() + " created successfully.\n" +
                      "Total Price: " + saved.getPrice() + " NIS.\n" +
                      "Please remember to confirm 1 simulation day (72 seconds) before, when prompted.");
            clearBookingFields();
            refreshReservations();
            //if the slot is full
        } else if ("FULL".equals(response.getPayload())) {
            // Show waiting list section
            waitingListSection.setVisible(true);
            waitingListSection.setManaged(true);
            statusLabel.setText("Time slot is FULL. Join waiting list or look for alternates.");
        } else {
            showAlert("Error", "Booking Failed", response.getPayload().toString());
        }
    }

    
    @FXML
    public void onJoinWaitingList() {
        if (pendingReservationAttempt == null) return;
        
        Message response = ClientUI.client.sendRequest(new Message(Message.ENTER_WAITING_LIST, pendingReservationAttempt));
        if (Message.OK.equals(response.getAction())) {
            Reservation saved = (Reservation) response.getPayload();
            showAlert("Waiting List Joined", "Registered on Waiting List", 
                      "You have successfully joined the waiting list at spot queue.\n" +
                      "Reservation ID: " + saved.getReservationId() + "\n" +
                      "You will be alerted if space becomes available.");
            
            waitingListSection.setVisible(false);
            waitingListSection.setManaged(false);
            clearBookingFields();
            refreshReservations();
        } else {
            showAlert("Error", "Failed to join waiting list", response.getPayload().toString());
        }
    }

    @FXML
    public void onViewAlternates() {
        if (pendingReservationAttempt == null) return;
        
        LocalDate date = datePicker.getValue();
        if (date == null) return;
        
        // Fetch day's occupancy
        Object[] payload = new Object[]{pendingReservationAttempt.getParkId(), Date.valueOf(date)};
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_OCCUPANCY_TABLE, payload));
        
        if (Message.OK.equals(response.getAction())) {
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> table = (Map<Integer, Integer>) response.getPayload();
            alternatesList.clear();
            
            Park park = null;
            for (Park p : parksList) {
                if (p.getParkId() == pendingReservationAttempt.getParkId()) {
                    park = p;
                    break;
                }
            }
            if (park == null) return;
            
            int limit = park.getCurrentQuota() - park.getReservedGap();
            
            for (int h = 8; h <= 20; h++) {
                int load = table.getOrDefault(h, 0);
                alternatesList.add(new AlternateHour(String.format("%02d:00", h), load + " / " + limit));
            }
            
            alternatesTable.setVisible(true);
            alternatesTable.setManaged(true);
        } else {
            statusLabel.setText("Error loading alternate hours.");
        }
    }

    private void clearBookingFields() {
        parkComboBox.setValue(null);
        datePicker.setValue(null);
        hourComboBox.setValue(null);
        visitorsField.clear();
        emailField.clear();
        phoneField.clear();
        prepayCheckBox.setSelected(false);
        pendingReservationAttempt = null;
    }

    @FXML
    public void onLogout() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        User cur = ClientUI.currentUser;
        if (!"VISITOR".equals(cur.getRole())) {
            ClientUI.client.sendRequest(new Message(Message.LOGOUT, cur.getUsername()));
        }
        ClientUI.currentUser = null;
        ClientUI.setRoot("/gui/LoginUI.fxml", "GoNature - Login Portal", 500, 670);
    }

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

    private List<Integer> alertedReservations = new ArrayList<>();
    private int tickCount = 0;

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
            String formatted = ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            simClockLabel.setText("Simulated Time: " + formatted);
            
            // Auto-refresh table every 8 ticks (2 seconds)
            tickCount++;
            if (tickCount >= 8) {
                tickCount = 0;
                refreshReservationsSilent();
            }
        }));
        clockTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private long getSimulatedTimeAtSync() {
        long elapsedReal = clientSyncTime - simStartMs;
        return simStartMs + (long)(elapsedReal * simSpeedup);
    }

    private void refreshReservationsSilent() {
        String visitorId = ClientUI.currentUser.getUsername();
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_RESERVATIONS_BY_ID, visitorId));
        if (Message.OK.equals(response.getAction())) {
            @SuppressWarnings("unchecked")
            List<Reservation> res = (List<Reservation>) response.getPayload();
            
            // Check if any reservation changed to PENDING_CONFIRMATION and notify user
            for (Reservation r : res) {
                if ("PENDING_CONFIRMATION".equals(r.getStatus())) {
                    if (!alertedReservations.contains(r.getReservationId())) {
                        alertedReservations.add(r.getReservationId());
                        javafx.application.Platform.runLater(() -> {
                            showAlert("Reservation Alert", 
                                      "Action Required: Confirm Reservation", 
                                      "Your reservation #" + r.getReservationId() + " to " + r.getParkName() + 
                                      " requires confirmation.\n" +
                                      "Please select it in the table and click 'Confirm Reservation' within 6 seconds!");
                        });
                    }
                }
            }
            
            // Update table items while preserving selection
            Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
            int selectedId = selected != null ? selected.getReservationId() : -1;
            
            reservationList.clear();
            reservationList.addAll(res);
            
            if (selectedId != -1) {
                for (Reservation r : reservationList) {
                    if (r.getReservationId() == selectedId) {
                        reservationsTable.getSelectionModel().select(r);
                        break;
                    }
                }
            }
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Helper class for alternates table
    public static class AlternateHour {
        private final String hour;
        private final String load;

        public AlternateHour(String hour, String load) {
            this.hour = hour;
            this.load = load;
        }

        public String getHour() { return hour; }
        public String getLoad() { return load; }
    }
}
