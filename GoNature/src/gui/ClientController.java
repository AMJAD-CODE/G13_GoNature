package gui;

import java.sql.Date;
import java.time.LocalDate;

import client.GoNatureClient;
import common.ChatIF;
import common.Message;
import common.Order;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ClientController implements ChatIF {

	// FXML
	@FXML
	private TextField hostField;
	@FXML
	private TextField portField;
	@FXML
	private Button connectButton;
	@FXML
	private Button disconnectButton;
	@FXML
	private Button refreshButton;

	@FXML
	private TableView<Order> ordersTable;
	@FXML
	private TableColumn<Order, Integer> orderNumberColumn;
	@FXML
	private TableColumn<Order, Date> orderDateColumn;
	@FXML
	private TableColumn<Order, Integer> visitorsColumn;
	@FXML
	private TableColumn<Order, Integer> confirmCodeColumn;
	@FXML
	private TableColumn<Order, Integer> subscriberIdColumn;
	@FXML
	private TableColumn<Order, Date> placedDateColumn;

	@FXML
	private DatePicker newDatePicker;
	@FXML
	private TextField newVisitorsField;
	@FXML
	private Button updateButton;
	@FXML
	private Label statusLabel;

	private GoNatureClient client;
	private final ObservableList<Order> orderData = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		orderNumberColumn.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
		orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
		visitorsColumn.setCellValueFactory(new PropertyValueFactory<>("numberOfVisitors"));
		confirmCodeColumn.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
		subscriberIdColumn.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
		placedDateColumn.setCellValueFactory(new PropertyValueFactory<>("dateOfPlacingOrder"));
		ordersTable.setItems(orderData);

		ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldOrder, newOrder) -> {
			if (newOrder != null) {
				newDatePicker.setValue(newOrder.getOrderDate().toLocalDate());
				newVisitorsField.setText(String.valueOf(newOrder.getNumberOfVisitors()));
				updateButton.setDisable(false);
			} else {
				updateButton.setDisable(true);
			}
		});
	}

	@FXML
	public void onConnect() {
		try {
			String host = hostField.getText().trim();
			int port = Integer.parseInt(portField.getText().trim());
			client = new GoNatureClient(host, port, this);
			client.openConnection();
			connectButton.setDisable(true);
			disconnectButton.setDisable(false);
			refreshButton.setDisable(false);
			statusLabel.setText("Connected to " + host + ":" + port);
		} catch (Exception e) {
			statusLabel.setText("ERROR connecting: " + e.getMessage());
		}
	}

	@FXML
	public void onDisconnect() {
		try {
			if (client != null)
				client.closeConnection();
			disconnectedUI();
		} catch (Exception e) {
			statusLabel.setText("ERROR disconnecting: " + e.getMessage());
		}
	}

	@FXML
	public void onRefresh() {
		//Busy wait send request, blocks here until server replies
		client.handleMessageFromClientUI(new Message(Message.GET_ORDERS, null));
		// When we reach here the result is in GoNatureClient.lastOrders
		orderData.setAll(GoNatureClient.lastOrders);
		statusLabel.setText("Received " + GoNatureClient.lastOrders.size() + " orders");
	}

	@FXML
	public void onUpdate() {
		try {
			Order selected = ordersTable.getSelectionModel().getSelectedItem();
			if (selected == null)
				return;

			LocalDate newDate = newDatePicker.getValue();
			int newVisitors = Integer.parseInt(newVisitorsField.getText().trim());

			Order modified = new Order(selected.getOrderNumber(), Date.valueOf(newDate), newVisitors,
					selected.getConfirmationCode(), selected.getSubscriberId(), selected.getDateOfPlacingOrder());

			//Busy wait blocks until server confirms
			client.handleMessageFromClientUI(new Message(Message.UPDATE_ORDER, modified));

			if ("OK".equals(GoNatureClient.lastUpdateResult)) {
				statusLabel.setText("Update successful — refreshing...");
				onRefresh();
			} else {
				statusLabel.setText("Update failed: " + GoNatureClient.lastUpdateResult);
			}
		} catch (Exception e) {
			statusLabel.setText("ERROR updating: " + e.getMessage());
		}
	}

	@Override
	public void display(Object message) {
		System.out.println("[Client received] " + message);
	}

	private void disconnectedUI() {
		connectButton.setDisable(false);
		disconnectButton.setDisable(true);
		refreshButton.setDisable(true);
		updateButton.setDisable(true);
		orderData.clear();
		statusLabel.setText("Disconnected");
	}
}