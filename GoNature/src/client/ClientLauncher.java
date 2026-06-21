package client;

import common.ChatIF;

public class ClientLauncher {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5555;

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port. Using default: " + DEFAULT_PORT);
            }
        }

        ChatIF clientUI = new ChatIF() {
            @Override
            public void display(Object msg) {
                System.out.println(msg);
            }
        };

        try {
            GoNatureClient client = new GoNatureClient(host, port, clientUI);
            client.openConnection();
            System.out.println("Client connected to server successfully at " + host + ":" + port);
        } catch (Exception e) {
            System.out.println("ERROR - Could not connect to server: " + e.getMessage());
        }
    }
}
