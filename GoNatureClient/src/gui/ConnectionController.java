package gui;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import client.ClientUI;
import client.GoNatureClient;
import common.ChatIF;

/**
 * Controller for the connection screen. Collects the server host and port,
 * opens the OCSF client connection, and on success switches to the login
 * screen.
 */
public class ConnectionController implements ChatIF {

    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private Button connectButton;
    @FXML
    private Label statusLabel;

    /**
     * Reads the entered host and port, opens a connection to the server,
     * and navigates to the login screen on success. Shows an error if the
     * fields are invalid or the connection fails.
     */
    @FXML
    public void onConnect() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            statusLabel.setText("Connecting...");
            
            // Instantiate and connect OCSF client
            GoNatureClient c = new GoNatureClient(host, port, this);
            c.openConnection();
            ClientUI.client = c;
            
            statusLabel.setText("Connected successfully!");
            // Switch to Login screen (width: 500, height: 400)
            ClientUI.setRoot("/gui/LoginUI.fxml", "GoNature Workstation - Login", 500, 670);
            
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid port format.");
        } catch (IOException e) {
            statusLabel.setText("Connection failed: " + e.getMessage());
        }
    }

    /**
     * Logs a connection-related message to the console.
     *
     * @param message message to display
     */
    @Override
    public void display(Object message) {
        System.out.println("Connection Log: " + message);
    }
}
