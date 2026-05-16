package client;

import java.io.IOException;
import java.util.ArrayList;

import common.ChatIF;
import common.Message;
import common.Order;
import ocsf.client.AbstractClient;

public class GoNatureClient extends AbstractClient {

	//Set true when a request is in flight, reset by handleMessageFromServer
	public static boolean awaitResponse = false;

	//Last list of orders received from the server (after GET_ORDERS request)
	public static ArrayList<Order> lastOrders = new ArrayList<>();

	//Result of the last update request: "OK", "ERROR <msg>" or null
	public static String lastUpdateResult = null;

	//The output channel. Set by the GUI when constructing the client
	ChatIF clientUI;

	public GoNatureClient(String host, int port, ChatIF clientUI) throws IOException {
		super(host, port);
		this.clientUI = clientUI;
	}

	
	  //when the server sends us something Just
	  // stores the result and signals the busy waiting thread to wake up.
	
	@Override
	protected void handleMessageFromServer(Object msg) {
		System.out.println("--> handleMessageFromServer");

		if (msg instanceof Message) {
			Message message = (Message) msg;
			switch (message.getAction()) {
			case Message.ORDERS_LIST:
				@SuppressWarnings("unchecked")
				ArrayList<Order> orders = (ArrayList<Order>) message.getPayload();
				lastOrders = orders;
				break;
			case Message.UPDATE_OK:
				lastUpdateResult = "OK";
				break;
			case Message.ERROR:
				lastUpdateResult = "ERROR " + message.getPayload();
				break;
			}
		}
		clientUI.display(msg); //show status
		awaitResponse = false;
	}

	/**
	 * Called by the GUI when the user clicks a button. Sends the message and blocks
	 * until handleMessageFromServer flips awaitResponse to false.
	 */
	public void handleMessageFromClientUI(Object msg) {
		try {
			awaitResponse = true;
			sendToServer(msg);
			while (awaitResponse) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			clientUI.display("Could not send message to server: " + e.getMessage());
			awaitResponse = false;
		}
	}

	//Closes the connection and terminates the client
	public void quit() {
		try {
			closeConnection();
		} catch (IOException e) {
		}
		System.exit(0);
	}
}