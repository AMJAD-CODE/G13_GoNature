package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import client.ClientUI;
import common.Message;
import common.User;

/**
 * Controller for the login screen.
 *
 * Handles:
 * - Employee login via username and password, authenticated against the server
 * - Visitor login via a local guest session that does not require server authentication
 * - Updating the status label with feedback for the user
 */
public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visitorIdField;
    @FXML
    private Label statusLabel;

    /**
     * Handles the employee login action. Validates that a username and
     * password were entered, sends a login request to the server, and
     * updates the status label based on the result.
     *
     * On success, the returned User is stored as the current user in
     * ClientUI and the user's role is logged to the console. On failure,
     * the error message from the server response is shown to the user.
     */
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
            statusLabel.setText("Login successful! Welcome " + user.getFname());
            
            // Redirect based on role (Dashboard FXMLs will be loaded here in next sprints)
            System.out.println("User logged in with role: " + user.getRole());
        } else {
            statusLabel.setText(response.getPayload().toString());
        }
    }

    /**
     * Handles the visitor login action. Validates that a visitor ID was
     * entered, then creates a local guest User without contacting the
     * server. The guest user is stored as the current user in ClientUI.
     */
    @FXML
    public void onVisitorLogin() {
        String visitorId = visitorIdField.getText().trim();

        if (visitorId.isEmpty()) {
            statusLabel.setText("Please enter your ID or Subscriber Number.");
            return;
        }

        // Create local guest user and cache
        User visitor = new User(visitorId, "", "Visitor", "Guest", "VISITOR");
        ClientUI.currentUser = visitor;
        
        statusLabel.setText("Visitor Login successful!");
        System.out.println("Visitor session initialized: " + visitorId);
    }
}
