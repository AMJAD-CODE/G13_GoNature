package server;

import common.ChatIF;

public class ServerLauncher {

    private static final int DEFAULT_PORT = 5555;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number specified. Using default: " + DEFAULT_PORT);
            }
        }

        // Simple console UI implementation for ChatIF to output server events
        ChatIF consoleUI = new ChatIF() {
            @Override
            public void display(Object msg) {
                System.out.println(msg);
            }
        };

        GoNatureServer server = new GoNatureServer(port, consoleUI);
        try {
            server.listen();
        } catch (Exception ex) {
            System.out.println("ERROR - Could not listen for clients: " + ex.getMessage());
        }
    }
}
