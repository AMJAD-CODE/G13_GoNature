package client;

import java.io.IOException;
import common.ChatIF;
import common.Message;
import ocsf.client.AbstractClient;

public class GoNatureClient extends AbstractClient {

    // Busy-waiting flags for synchronous request-response over TCP
    public static boolean awaitResponse = false;
    private static Message lastResponse = null;

    private final ChatIF clientUI;

    public GoNatureClient(String host, int port, ChatIF clientUI) throws IOException {
        super(host, port);
        this.clientUI = clientUI;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        System.out.println("--> handleMessageFromServer: " + msg);
        if (msg instanceof Message) {
            lastResponse = (Message) msg;
        } else {
            lastResponse = new Message(Message.ERROR, "Unknown object type from server");
        }
        
        // Notify any observers via the display interface
        clientUI.display(msg);
        
        // Release the busy-wait thread
        awaitResponse = false;
    }

    /**
     * Sends a request to the server and blocks until the response is received.
     */
    public synchronized Message sendRequest(Message request) {
        try {
            awaitResponse = true;
            lastResponse = null;
            sendToServer(request);
            
            // Block until response changes the flag
            while (awaitResponse) {//wait for "handleMessageFromServer" to change the flag
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            clientUI.display("Connection error: Could not send message to server: " + e.getMessage());
            awaitResponse = false;
            return new Message(Message.ERROR, "Network Error: " + e.getMessage());
        }
        
        return lastResponse;
    }

    public void quit() {
        try {
            closeConnection();
        } catch (IOException ignored) {}
        System.exit(0);
    }
}
