package gui;

import client.GoNatureClient;
import common.ChatIF;

public class ConnectionController {

    private String serverHost;
    private int serverPort;
    private GoNatureClient client;
    private boolean isConnected;
    
    /**
     * Controller for the connection screen.
     *
     * Handles:
     * - Server connection setup
     * - Client initialization
     * - Navigation to the login screen after a successful connection
     */
    public ConnectionController(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.isConnected = false;
    }

    /**
     * Attempts to open a connection to the server using the configured
     * host and port. On failure, the error message is displayed via the
     * given ChatIF instance.
     *
     * @param ui the ChatIF used to display connection status or error messages
     * @return true if the connection was opened successfully, false otherwise
     */
    public boolean connect(ChatIF ui) {
        try {
            client = new GoNatureClient(serverHost, serverPort, ui);
            client.openConnection();
            isConnected = true;
            return true;
        } catch (Exception e) {
            ui.display("Connection failed: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }

    /**
     * Closes the connection to the server if one is currently open.
     * Any exception raised while closing the connection is silently ignored.
     */
    public void disconnect() {
        if (client != null && isConnected) {
            try {
                client.closeConnection();
            } catch (Exception ignored) {}
            isConnected = false;
        }
    }
    
    /**
     * Returns the client instance used to communicate with the server.
     *
     * @return the GoNatureClient instance, or null if connect(ChatIF)
     *         has not yet been called
     */
    public GoNatureClient getClient() { return client; }
    
    /**
     * Returns whether the controller currently has an active connection
     * to the server.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() { return isConnected; }
}
