package gui;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import client.ClientUI;
import client.GoNatureClient;
import common.ChatIF;

public class ConnectionController implements ChatIF {

    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private Button connectButton;
    @FXML
    private Label statusLabel;

    //it just collects the server IP/host and opens the connection, then swaps to the login screen
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

    @Override
    public void display(Object message) {
        System.out.println("Connection Log: " + message);
    }
}
