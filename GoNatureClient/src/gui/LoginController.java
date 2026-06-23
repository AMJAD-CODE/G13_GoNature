package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import client.ClientUI;
import common.Message;
import common.User;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visitorIdField;
    @FXML
    private Label statusLabel;

    @FXML
    public void onEmployeeLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }

        // Send login request to server
        Message response = ClientUI.client.sendRequest(new Message(Message.LOGIN, new String[]{username, password}));

        if (Message.OK.equals(response.getAction())) {
            User user = (User) response.getPayload();
            ClientUI.currentUser = user;
            statusLabel.setText("Login successful!");
            
            // Redirect based on role
            switch (user.getRole()) {
                case "PARK_EMPLOYEE":
                    ClientUI.setRoot("/gui/EmployeeDashboard.fxml", "GoNature Workstation - Park Employee", 1000, 600);
                    break;
                case "PARK_MANAGER":
                    ClientUI.setRoot("/gui/ParkManagerDashboard.fxml", "GoNature Workstation - Park Manager", 950, 650);
                    break;
                case "DEPARTMENT_MANAGER":
                    ClientUI.setRoot("/gui/DeptManagerDashboard.fxml", "GoNature Workstation - Department Manager", 1050, 650);
                    break;
                case "SERVICE_REPRESENTATIVE":
                    ClientUI.setRoot("/gui/ServiceRepDashboard.fxml", "GoNature Workstation - Service Representative", 800, 650);
                    break;
                case "GUIDE":
                    ClientUI.setRoot("/gui/VisitorDashboard.fxml", "GoNature Workstation - Guide Reservation Panel", 1335, 680);
                    break;
                default:
                    statusLabel.setText("Unknown employee role: " + user.getRole()); 
            }
        } else {
            statusLabel.setText(response.getPayload().toString());
        }
    }

    @FXML
    public void onVisitorLogin() {
        String visitorId = visitorIdField.getText().trim();

        if (visitorId.isEmpty()) {
            statusLabel.setText("Please enter your ID or Subscriber Number.");
            return;
        }

        // Set current user as Visitor/Subscriber
        User visitor = new User(visitorId, "", "Visitor", "Guest", "VISITOR", "visitor@mail.com", null);
        ClientUI.currentUser = visitor;
        
        statusLabel.setText("Login successful!");
        ClientUI.setRoot("/gui/VisitorDashboard.fxml", "GoNature Portal - Visitor Panel", 1335, 680);
    }

    @FXML
    public void onBack() {
        // Cleanly disconnect from the server, then return to the Connection screen.
        try {
            if (ClientUI.client != null) {
                ClientUI.client.closeConnection();
            }
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
        ClientUI.client = null;
        ClientUI.currentUser = null;
        ClientUI.setRoot("/gui/ConnectionUI.fxml", "GoNature Workstation - Connect", 400, 420);
    }
}
