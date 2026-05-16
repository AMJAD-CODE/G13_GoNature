package server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import common.Message;
import common.Order;
import db.DatabaseController;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/**
 * The OCSF server for GoNature. Owns the DatabaseController, listens for
 * incoming Messages from clients, and dispatches them to the appropriate
 * handler. Notifies a GUI (via a Runnable callback) when the connected client
 * list changes.
 */
public class GoNatureServer extends AbstractServer {

	private final DatabaseController db;
	private Runnable onConnectionsChanged; //GUI refresh

	public GoNatureServer(int port) {
		super(port);
		this.db = new DatabaseController();
	}

	//GUI calls this to register a refresh callback
	public void setOnConnectionsChanged(Runnable callback) {
		this.onConnectionsChanged = callback;
	}

	
	@Override
	protected void serverStarted() {
		System.out.println("Server started on port " + getPort());
		if (!db.connect()) {
			System.out.println("WARNING: DB connection failed. Queries will not work.");
		}
	}

	@Override
	protected void serverStopped() {
		System.out.println("Server stopped.");
		db.disconnect();
	}

	
	@Override
	protected void clientConnected(ConnectionToClient client) {
		System.out.println("Client connected: " + client.getInetAddress());
		notifyConnectionsChanged();
	}

	@Override
	protected synchronized void clientDisconnected(ConnectionToClient client) {
		markDisconnected(client);
	}

	@Override
	protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
		markDisconnected(client);
	}

	
	 //Both clientDisconnected and clientException end up here. The flag ensures we
	 //update the GUI only once, no matter which (or both) fired.
	 
	private void markDisconnected(ConnectionToClient client) {
		if (client.getInfo("Disconnected") == null) {
			client.setInfo("Disconnected", true);
			System.out.println("Client disconnected: " + client.getInetAddress());
			notifyConnectionsChanged();
		}
	}

	private void notifyConnectionsChanged() {
		if (onConnectionsChanged != null) {
			onConnectionsChanged.run();
		}
	}

	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		try {
			if (!(msg instanceof Message)) {
				System.out.println("ERROR in handleMessageFromClient: unexpected object: " + msg);
				return;
			}
			Message request = (Message) msg;

			switch (request.getAction()) {
			case Message.GET_ORDERS:
				handleGetOrders(client);
				break;
			case Message.UPDATE_ORDER:
				handleUpdateOrder((Order) request.getPayload(), client);
				break;
			default:
				System.out.println("ERROR: unknown action: " + request.getAction());
				client.sendToClient(new Message(Message.ERROR, "Unknown action: " + request.getAction()));
			}
		} catch (Exception e) {
			System.out.println("ERROR in handleMessageFromClient: " + e.getMessage());
			try {
				client.sendToClient(new Message(Message.ERROR, e.getMessage()));
			} catch (IOException ioe) {
				System.out.println("Also failed to send error back to client: " + ioe.getMessage());
			}
		}
	}


	private void handleGetOrders(ConnectionToClient client) throws IOException {
		List<Order> orders = db.getAllOrders();
		// Wrap in ArrayList to guarantee a Serializable  type on the wire.
		client.sendToClient(new Message(Message.ORDERS_LIST, new ArrayList<>(orders)));
	}

	private void handleUpdateOrder(Order order, ConnectionToClient client) throws IOException {
		boolean ok = db.updateOrder(order);
		if (ok) {
			client.sendToClient(new Message(Message.UPDATE_OK, null));
		} else {
			client.sendToClient(new Message(Message.ERROR, "Update failed for order " + order.getOrderNumber()));
		}
	}
}