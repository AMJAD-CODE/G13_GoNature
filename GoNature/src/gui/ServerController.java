package gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import ocsf.server.ConnectionToClient;
import server.GoNatureServer;

/**
 * Controller for ServerUI.fxml. Holds the GoNatureServer instance and keeps the
 * connected clients table in sync with the server's actual state.
 */
public class ServerController {

	//Widgets from FXML fx:id must mach
	@FXML
	private TextField portField;
	@FXML
	private Button startButton;
	@FXML
	private Button stopButton;
	@FXML
	private TableView<ClientInfo> clientsTable;
	@FXML
	private TableColumn<ClientInfo, String> ipColumn;
	@FXML
	private TableColumn<ClientInfo, String> hostColumn;
	@FXML
	private TableColumn<ClientInfo, String> statusColumn;
	@FXML
	private Label statusLabel;

	private GoNatureServer server;
	private final ObservableList<ClientInfo> clientData = FXCollections.observableArrayList();

	//Called automatically by JavaFX after the FXML widgets are added
	@FXML
	public void initialize() {
		ipColumn.setCellValueFactory(new PropertyValueFactory<>("ip"));
		hostColumn.setCellValueFactory(new PropertyValueFactory<>("host"));
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		clientsTable.setItems(clientData);
	}

	@FXML
	public void onStartServer() {
		try {
			int port = Integer.parseInt(portField.getText().trim());
			server = new GoNatureServer(port);
			server.setOnConnectionsChanged(this::refreshClientsTable);
			server.listen();

			startButton.setDisable(true);
			stopButton.setDisable(false);
			portField.setDisable(true);
			statusLabel.setText("Server running on port " + port);
		} catch (Exception e) {
			statusLabel.setText("ERROR starting server: " + e.getMessage());
			System.out.println("ERROR in ServerController.onStartServer: " + e.getMessage());
		}
	}

	@FXML
	public void onStopServer() {
		try {
			if (server != null) {
				server.close();
			}
			startButton.setDisable(false);
			stopButton.setDisable(true);
			portField.setDisable(false);
			statusLabel.setText("Server stopped");
			clientData.clear();
		} catch (Exception e) {
			statusLabel.setText("ERROR stopping server: " + e.getMessage());
			System.out.println("ERROR in ServerController.onStopServer: " + e.getMessage());
		}
	}

	//GoNatureServer calls this whenever a client connects or disconnects
	private void refreshClientsTable() {
		//check this again!!!!!
		// Server callbacks fire on the OCSF thread — UI mutations must happen on the
		// JavaFX thread.
		Platform.runLater(() -> {
			clientData.clear();
			Thread[] connections = server.getClientConnections();
			for (Thread t : connections) {
				if (t instanceof ConnectionToClient) {
					ConnectionToClient c = (ConnectionToClient) t;
					String ip = c.getInetAddress() != null ? c.getInetAddress().getHostAddress() : "?";
					String host = c.getInetAddress() != null ? c.getInetAddress().getHostName() : "?";
					String status = (c.getInfo("Disconnected") != null) ? "Disconnected" : "Connected";
					clientData.add(new ClientInfo(ip, host, status));
				}
			}
		});
	}

	
	public static class ClientInfo {
		private final SimpleStringProperty ip;
		private final SimpleStringProperty host;
		private final SimpleStringProperty status;

		public ClientInfo(String ip, String host, String status) {
			this.ip = new SimpleStringProperty(ip);
			this.host = new SimpleStringProperty(host);
			this.status = new SimpleStringProperty(status);
		}

		public String getIp() {
			return ip.get();
		}

		public String getHost() {
			return host.get();
		}

		public String getStatus() {
			return status.get();
		}
	}
}