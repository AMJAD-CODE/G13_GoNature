package server;

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

        GoNatureServer server = new GoNatureServer(port, null);
        try {
            server.listen();
        } catch (Exception ex) {
            System.out.println("ERROR - Could not listen for clients: " + ex.getMessage());
        }
    }
}