package client;

import java.io.IOException;
import common.ChatIF;
import common.Message;
import ocsf.client.AbstractClient;

/**
 * Client side network handler for the GoNature application.
 *
 * Extends the OCSF AbstractClient to provide a synchronous request/response
 * communication model on top of OCSF's underlying asynchronous messaging.
 * Callers use sendRequest(Message) to send a request to the server and
 * block until a corresponding response is received.
 */
public class GoNatureClient extends AbstractClient {

    private final ChatIF clientUI;
    private static Message lastResponse = null;
    public static boolean awaitResponse = false;

    /**
     * Constructs a GoNatureClient and configures it to connect to the
     * given host and port. Does not open the connection itself; call
     * openConnection() separately to connect.
     *
     * @param host     the host name or IP address of the server
     * @param port     the port number of the server
     * @param clientUI the UI used to display messages and errors
     * 
     * @throws IOException if the client cannot be configured for the given host and port
     */
    public GoNatureClient(String host, int port, ChatIF clientUI) throws IOException {
        super(host, port);
        this.clientUI = clientUI;
    }


    /**
     * Handles a message received from the server. If the message is a
     * recognized Message instance, it is stored as the last response;
     * otherwise an error Message is stored instead. Displays the response
     * via the client UI and clears awaitResponse so that any thread
     * blocked in sendRequest(Message) can proceed.
     *
     * @param msg the message received from the server
     */
    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg instanceof Message) {
            lastResponse = (Message) msg;
        } else {
            lastResponse = new Message(Message.ERROR, "Unknown response structure");
        }
        
        clientUI.display("Received response from server: " + lastResponse);
        awaitResponse = false;
    }

    /**
     * Sends a request to the server and blocks the calling thread until
     * a response is received or sending fails.
     *
     * Internally sets awaitResponse to true and busy-waits in short
     * intervals until handleMessageFromServer(Object) clears the flag
     * upon receiving a response. If sending the request fails due to a
     * network error, an error Message is returned immediately instead
     * of waiting.
     *
     * @param request the request message to send to the server
     * @return the response Message received from the server, or an error
     *         Message if the request could not be sent
     */
    public synchronized Message sendRequest(Message request) {
        try {
            awaitResponse = true;
            lastResponse = null;
            sendToServer(request);
            
            // Simple busy wait loop until response thread unlocks the flag
            while (awaitResponse) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            clientUI.display("Network Error: Could not send request: " + e.getMessage());
            awaitResponse = false;
            return new Message(Message.ERROR, "Network Error: " + e.getMessage());
        }
        
        return lastResponse;
    }
}
