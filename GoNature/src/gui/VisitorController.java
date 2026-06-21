package gui;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import client.ClientUI;
import common.Message;
import common.Park;
import common.Reservation;
import common.User;

/**
 * Controller for the visitor screen.
 *
 * Handles:
 * - Displaying a welcome message for the currently logged-in user
 * - Populating available booking hours
 * - Collecting and validating reservation booking input
 * - Submitting reservation requests to the server and showing the result
 * - Logging out the current user
 */
public class VisitorController {

    @FXML
    private Label welcomeLabel;

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

    /**
     * Initializes the visitor screen. Sets the welcome message based on the
     * currently logged-in user, if any, and populates hourComboBox with
     * standard working hours from 08:00 to 20:00.
     */
    @FXML
    public void initialize() {
        User cur = ClientUI.currentUser;
        if (cur != null) {
            welcomeLabel.setText("Welcome " + cur.getFirstName() + " (ID: " + cur.getUsername() + ")");
        }

        // Fill standard working hours (08:00 - 20:00)
        for (int h = 8; h <= 20; h++) {
            hourComboBox.getItems().add(String.format("%02d:00", h));
        }
    }

    /**
     * Handles the booking action. Validates all booking fields, builds a
     * Reservation object from the entered data, and sends a create
     * reservation request to the server.
     *
     * Validation includes:
     * - All fields must be filled in
     * - Visitor count must be a positive integer
     *
     * The reservation type and payment status are determined based on the
     * current user's role: a GUIDE produces an ORGANIZED_GROUP reservation
     * with payment status set according to prepayCheckBox, while any other
     * role produces an INDIVIDUAL reservation with an UNPAID status (the
     * server may upgrade this if the visitor is a subscriber).
     *
     * On success, a confirmation alert is shown with the reservation ID and
     * price, and the booking fields are cleared. On failure, an error alert
     * is shown with the server's error message.
     */
    @FXML
    public void onBookVisit() {
        Park park = parkComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String hour = hourComboBox.getValue();
        String visitorsStr = visitorsField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (park == null || date == null || hour == null || visitorsStr.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            statusLabel.setText("Please fill in all booking fields.");
            return;
        }

        int visitors;
        try {
            visitors = Integer.parseInt(visitorsStr);
            if (visitors <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            statusLabel.setText("Visitors count must be a positive integer.");
            return;
        }

        // Construct Timestamp
        int h = Integer.parseInt(hour.substring(0, 2));
        LocalDateTime ldt = LocalDateTime.of(date, LocalTime.of(h, 0));
        Timestamp visitTs = Timestamp.valueOf(ldt);

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
        
        // Determine type
        if ("GUIDE".equals(ClientUI.currentUser.getRole())) {
            res.setReservationType("ORGANIZED_GROUP");
            if (prepayCheckBox.isSelected()) {
                res.setPaymentStatus("PAID_IN_ADVANCE");
            } else {
                res.setPaymentStatus("UNPAID");
            }
        } else {
            res.setReservationType("INDIVIDUAL"); // Server will check and upgrade if subscriber
            res.setPaymentStatus("UNPAID");
        }

        Message response = ClientUI.client.sendRequest(new Message(Message.CREATE_RESERVATION, res));
        if (Message.OK.equals(response.getAction())) {
            Reservation saved = (Reservation) response.getPayload();
            showAlert("Success", "Booking Confirmed", 
                      "Reservation #" + saved.getReservationId() + " created successfully.\n" +
                      "Total Price: " + saved.getPrice() + " NIS.");
            clearBookingFields();
        } else {
            showAlert("Error", "Booking Failed", response.getPayload().toString());
        }
    }

    /**
     * Resets all booking input fields to their default empty or unselected
     * state, typically called after a successful booking.
     */
    private void clearBookingFields() {
        parkComboBox.setValue(null);
        datePicker.setValue(null);
        hourComboBox.setValue(null);
        visitorsField.clear();
        emailField.clear();
        phoneField.clear();
        prepayCheckBox.setSelected(false);
    }

    /**
     * Handles the logout action. If the current user is logged in and is
     * not a visitor guest, a logout request is sent to the server. The
     * current user is then cleared from ClientUI regardless of role.
     */
    @FXML
    public void onLogout() {
        User cur = ClientUI.currentUser;
        if (cur != null && !"VISITOR".equals(cur.getRole())) {
            ClientUI.client.sendRequest(new Message(Message.LOGOUT, cur.getUsername()));
        }
        ClientUI.currentUser = null;
        System.out.println("Logout action completed.");
    }

    /**
     * Displays an informational alert dialog with the given title, header,
     * and content text, and blocks until the user dismisses it.
     *
     * @param title   the title of the alert window
     * @param header  the header text shown in the alert
     * @param content the main message content shown in the alert
     */
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
