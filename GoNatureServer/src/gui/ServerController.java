package gui;

import java.net.InetAddress;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import common.ChatIF;
import ocsf.server.ConnectionToClient;
import server.GoNatureServer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import server.SimulationScheduler;

/**
* Controller for the server console screen. Handles starting and stopping
* the OCSF server, displaying connected clients, logging console output,
* and showing the simulated clock.
*/
public class ServerController implements ChatIF {

    @FXML
    private TextField portField;
    @FXML
    private TextField dbHostField;
    @FXML
    private TextField dbNameField;
    @FXML
    private TextField dbUserField;
    @FXML
    private PasswordField dbPasswordField;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private TableView<ClientInfo> clientsTable;
    @FXML
    private TableColumn<ClientInfo, String> ipColumn;
    @FXML
    private TableColumn<ClientInfo, String> hostColumn;
    @FXML
    private TableColumn<ClientInfo, String> statusColumn;
    @FXML
    private TextArea consoleLogArea;
    @FXML
    private Label statusLabel;
    @FXML
    private Label simClockLabel;

    private GoNatureServer server;
    private Timeline clockTimeline;
    private final ObservableList<ClientInfo> clientData = FXCollections.observableArrayList();

    /**
     * Wires up the clients table columns and shows a welcome message in
     * the console log.
     */
    @FXML
    public void initialize() {
        ipColumn.setCellValueFactory(new PropertyValueFactory<>("ip"));
        hostColumn.setCellValueFactory(new PropertyValueFactory<>("host"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        clientsTable.setItems(clientData);
        display("System: Welcome to GoNature Server Console.");
    }

    /**
     * Appends a message to the console log area on the JavaFX thread.
     *
     * @param message message to log
     */
    @Override
    public void display(Object message) {
        Platform.runLater(() -> {
            if (message != null) {
                consoleLogArea.appendText(message.toString() + "\n");
            }
        });
    }

    /**
     * Starts the OCSF server using the entered port and database
     * credentials, disables the input fields, and starts the simulated
     * clock. Shows an error in the status label and console log on
     * failure.
     */
    @FXML
    public void onStartServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            String host = dbHostField.getText().trim();
            String name = dbNameField.getText().trim();
            String user = dbUserField.getText().trim();
            String pass = dbPasswordField.getText();

            display("System: Starting server connection...");
            server = new GoNatureServer(port, host, name, user, pass, this);
            server.setOnConnectionsChanged(this::refreshClientsTable);
            server.listen(); // Start OCSF listener

            startButton.setDisable(true);
            stopButton.setDisable(false);
            toggleInputs(true);
            
            statusLabel.setText("Server Online (Port: " + port + ")");
            statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            
            startClockTimeline();
        } catch (Exception e) {
            statusLabel.setText("Server Startup Failed");
            statusLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            display("System ERROR: " + e.getMessage());
        }
    }

    /**
     * Stops the running server and the simulated clock, re-enables the
     * input fields, and clears the connected clients table.
     */
    @FXML
    public void onStopServer() {
        try {
            if (server != null) {
                server.close();
            }
            if (clockTimeline != null) {
                clockTimeline.stop();
            }
            simClockLabel.setText("Simulated Time: --");
            
            startButton.setDisable(false);
            stopButton.setDisable(true);
            toggleInputs(false);
            clientData.clear();
            
            statusLabel.setText("Server Offline");
            statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
            display("System: Server stopped successfully.");
        } catch (Exception e) {
            display("System ERROR: " + e.getMessage());
        }
    }

    /**
     * Starts (or restarts) the recurring timeline that updates the
     * simulated clock label every 250ms from the simulation scheduler.
     */
    private void startClockTimeline() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        clockTimeline = new Timeline(new KeyFrame(Duration.millis(250), event -> {
            long simTime = SimulationScheduler.getSimulatedTime();
            Timestamp ts = new Timestamp(simTime);
            LocalDateTime ldt = ts.toLocalDateTime();
            String formatted = ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            simClockLabel.setText("Simulated Time: " + formatted);
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    /**
     * Enables or disables the server configuration input fields.
     *
     * @param disable true to disable the fields, false to enable them
     */
    private void toggleInputs(boolean disable) {
        portField.setDisable(disable);
        dbHostField.setDisable(disable);
        dbNameField.setDisable(disable);
        dbUserField.setDisable(disable);
        dbPasswordField.setDisable(disable);
    }

    /**
     * Rebuilds the clients table from the server's current live
     * connections.
     */
    private void refreshClientsTable() {
        Platform.runLater(() -> {
            clientData.clear();
            if (server == null) return;
            for (ConnectionToClient c : server.getLiveClients()) {
                InetAddress addr = c.getInetAddress();
                String ip = addr != null ? addr.getHostAddress() : "?";
                String host = addr != null ? addr.getHostName() : "?";
                String status = (c.getInfo("Disconnected") != null) ? "Disconnected" : "Connected";
                clientData.add(new ClientInfo(ip, host, status));
            }
        });
    }

    /**
     * Simple row model for the connected-clients table: IP address,
     * host name, and connection status.
     */
    public static class ClientInfo {
        private final SimpleStringProperty ip;
        private final SimpleStringProperty host;
        private final SimpleStringProperty status;

        /**
         * Creates a client row with the given IP, host, and status.
         *
         * @param ip     client IP address
         * @param host   client host name
         * @param status connection status
         */
        public ClientInfo(String ip, String host, String status) {
            this.ip = new SimpleStringProperty(ip);
            this.host = new SimpleStringProperty(host);
            this.status = new SimpleStringProperty(status);
        }
        
        /** @return the client's IP address */
        public String getIp() { return ip.get(); }
        /** @return the client's host name */
        public String getHost() { return host.get(); }
        /** @return the client's connection status */
        public String getStatus() { return status.get(); }
    }
}
