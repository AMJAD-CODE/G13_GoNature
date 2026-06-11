package server;

import java.io.IOException;
import common.ChatIF;
import common.Message;
//import common.User; to be added
import db.DatabaseController;
//import db.DatabaseConfig; to be added
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class GoNatureServer extends AbstractServer {

    private final DatabaseController db;
    private final ChatIF ui;

    public GoNatureServer(int port, ChatIF ui) {
        super(port);
        this.ui = ui;
        this.db = new DatabaseController();
    }

    @Override
    protected void serverStarted() {
        ui.display("Server: Starting listener on port " + getPort());
        
        // Initialize config and connect to DB dynamically (Commit 2 feature)
        DatabaseConfig config = new DatabaseConfig();
        config.loadConfig();
        
        if (db.connect(config.getDbHost(), config.getDbName(), config.getDbUser(), config.getDbPassword())) {
            ui.display("Server: Database connected successfully.");
        } else {
            ui.display("Server ERROR: Database connection failed. Queries will fail.");
        }
    }

    @Override
    protected void serverStopped() {
        ui.display("Server: Listener stopped.");
        db.disconnect();
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        ui.display("Client connected: " + client.getInetAddress().getHostAddress());
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        ui.display("Client disconnected: " + client.getInetAddress().getHostAddress());
        
        String username = (String) client.getInfo("Username");
        if (username != null) {
            db.setLoginStatus(username, false);
            ui.display("Automatically logged out user: " + username);
        }
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if (!(msg instanceof Message)) {
            System.out.println("ERROR: unexpected message type received: " + msg);
            return;
        }

        Message request = (Message) msg;
        ui.display("Request Received: " + request.getAction() + " from " + client.getInetAddress().getHostAddress());
        
        try {
            Message response = processRequest(request, client);
            client.sendToClient(response);
        } catch (IOException e) {
            ui.display("Error processing request: " + e.getMessage());
        }
    }

    private Message processRequest(Message request, ConnectionToClient client) {
        if (!db.isConnected()) {
            return new Message(Message.ERROR, "Database is NOT connected to the server.");
        }
        
        switch (request.getAction()) {
            case Message.LOGIN: {
                String[] creds = (String[]) request.getPayload();
                User user = db.loginUser(creds[0], creds[1]);
                if (user != null) {
                    client.setInfo("Username", user.getUsername());
                    return new Message(Message.OK, user);
                } else {
                    return new Message(Message.ERROR, "Login failed. Invalid credentials or user already logged in.");
                }
            }
            case Message.LOGOUT: {
                String username = (String) request.getPayload();
                db.setLoginStatus(username, false);
                client.setInfo("Username", null);
                return new Message(Message.OK, "Logged out successfully");
            }
            default:
                return new Message(Message.ERROR, "Unknown server command in current sprint: " + request.getAction());
        }
    }
}
