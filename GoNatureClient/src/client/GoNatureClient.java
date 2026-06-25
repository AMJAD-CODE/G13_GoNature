package client;

import java.io.IOException;
import common.ChatIF;
import common.Message;
import ocsf.client.AbstractClient;

/**
 * Client side of the GoNature application.
 * Extends the OCSF AbstractClient and adds a synchronous
 * request-response mechanism on top of the underlying async TCP connection.
 */
public class GoNatureClient extends AbstractClient {

    // Busy-waiting flags for synchronous request-response over TCP
    public static boolean awaitResponse = false;
    private static Message lastResponse = null;

    private final ChatIF clientUI;

    /**
     * Creates the client and opens a connection to the server.
     *
     * @param host the server host name or address
     * @param port the server port
     * @param clientUI the UI used to display messages
     * @throws IOException if the connection to the server cannot be established
     */
    public GoNatureClient(String host, int port, ChatIF clientUI) throws IOException {
        super(host, port);
        this.clientUI = clientUI;
    }

    /**
     * Handles messages pushed from the server.
     * Responds automatically to PING messages with a PONG,
     * and otherwise stores the message as the last response,
     * forwards it to the UI, and releases any thread waiting in sendRequest.
     *
     * @param msg the message received from the server
     */
    @Override
    protected void handleMessageFromServer(Object msg) {
    	//========= ping
    	if (msg instanceof Message && Message.PING.equals(((Message) msg).getAction())) {
    	    try {
    	        sendToServer(new Message(Message.PONG, null));
    	    } catch (IOException e) {
    	    }
    	    return;
    	}
    	//=========
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
     *
     * @param request the message to send to the server
     * @return the server's response, or an ERROR message if sending failed
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

    /**
     * Closes the connection to the server and terminates the application.
     */
    public void quit() {
        try {
            closeConnection();
        } catch (IOException ignored) {}
        System.exit(0);
    }
}
