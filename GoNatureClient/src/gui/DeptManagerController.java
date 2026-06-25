package gui;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import client.ClientUI;
import common.Message;
import common.Park;
import common.Promotion;

/**
 * Controller for the Department Manager screen.
 * Handles park/promotion proposal approvals and monthly report generation.
 */
public class DeptManagerController {

    @FXML
    private Label deptLabel;
    @FXML
    private Label statusLabel;

    @FXML
    private Label simClockLabel;
    private javafx.animation.Timeline clockTimeline;
    private long simStartMs = 0;
    private double simSpeedup = 1.0;
    private long clientSyncTime = 0;


    // Parks proposal table
    @FXML
    private TableView<Park> parksTable;
    @FXML
    private TableColumn<Park, Integer> parkIdCol;
    @FXML
    private TableColumn<Park, String> parkNameCol;
    @FXML
    private TableColumn<Park, Integer> parkCapacityCol;
    @FXML
    private TableColumn<Park, Integer> parkGapCol;
    @FXML
    private TableColumn<Park, Integer> parkDurationCol;
    @FXML
    private Button approveParkBtn;
    @FXML
    private Button rejectParkBtn;

    // Promotions proposal table
    @FXML
    private TableView<Promotion> promotionsTable;
    @FXML
    private TableColumn<Promotion, Integer> promoIdCol;
    @FXML
    private TableColumn<Promotion, Integer> promoParkCol;
    @FXML
    private TableColumn<Promotion, String> promoNameCol;
    @FXML
    private TableColumn<Promotion, Double> promoDiscountCol;
    @FXML
    private TableColumn<Promotion, String> promoStartCol;
    @FXML
    private TableColumn<Promotion, String> promoEndCol;
    @FXML
    private Button approvePromoBtn;
    @FXML
    private Button rejectPromoBtn;

    // Reports ComboBoxes
    @FXML
    private ComboBox<Integer> monthComboBox;
    @FXML
    private ComboBox<Integer> yearComboBox;
    @FXML
    private ComboBox<Object> parkFilterComboBox;

    // Reports Charts
    @FXML
    private BarChart<String, Number> durationChart;
    @FXML
    private BarChart<String, Number> cancellationChart;

    private final ObservableList<Park> pendingParks = FXCollections.observableArrayList();
    private final ObservableList<Promotion> pendingPromotions = FXCollections.observableArrayList();
    private final List<Park> allParks = new ArrayList<>();

    /**
     * Initializes table columns, combo boxes, loads pending proposals,
     * and starts the simulated clock.
     */
    @FXML
    public void initialize() {
        deptLabel.setText("Logged in as Dept Manager: " + ClientUI.currentUser.getUsername());

        // Setup Parks columns
        parkIdCol.setCellValueFactory(new PropertyValueFactory<>("parkId"));
        parkNameCol.setCellValueFactory(new PropertyValueFactory<>("parkName"));
        parkCapacityCol.setCellValueFactory(new PropertyValueFactory<>("pendingMaxQuota"));
        parkGapCol.setCellValueFactory(new PropertyValueFactory<>("pendingReservedGap"));
        parkDurationCol.setCellValueFactory(new PropertyValueFactory<>("pendingStayDuration"));
        parksTable.setItems(pendingParks);

        // Setup Promotions columns
        promoIdCol.setCellValueFactory(new PropertyValueFactory<>("promotionId"));
        promoParkCol.setCellValueFactory(new PropertyValueFactory<>("parkId"));
        promoNameCol.setCellValueFactory(new PropertyValueFactory<>("promotionName"));
        
        promoDiscountCol.setCellValueFactory(cellData -> {
            double discount = cellData.getValue().getDiscountPercentage();
            return new javafx.beans.property.SimpleDoubleProperty(discount * 100.0).asObject();
        });
        
        promoStartCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStartDate().toString()));
        promoEndCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEndDate().toString()));
        promotionsTable.setItems(pendingPromotions);

        // Listeners for enabling approval action buttons
        parksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean disable = (newSel == null);
            approveParkBtn.setDisable(disable);
            rejectParkBtn.setDisable(disable);
        });

        promotionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean disable = (newSel == null);
            approvePromoBtn.setDisable(disable);
            rejectPromoBtn.setDisable(disable);
        });

        // Setup combo boxes
        for (int m = 1; m <= 12; m++) {
            monthComboBox.getItems().add(m);
        }
        for (int y = 2025; y <= 2027; y++) {
            yearComboBox.getItems().add(y);
        }

        LocalDate now = LocalDate.now();
        monthComboBox.setValue(now.getMonthValue());
        yearComboBox.setValue(now.getYear());

        // Load data
        loadParksFilter();
        refreshPendingParks();
        refreshPendingPromotions();

        // Init simulated clock
        initSimClock();
    }

    /**
     * Populates the park filter combo box with "All Parks" and parks 1-3
     * fetched from the server.
     */
    private void loadParksFilter() {
        parkFilterComboBox.getItems().clear();
        parkFilterComboBox.getItems().add("All Parks");
        allParks.clear();
        
        // Fetch standard Masada, Banias, Carmel
        for (int id = 1; id <= 3; id++) {
            Message resp = ClientUI.client.sendRequest(new Message(Message.GET_PARK, id));
            if (Message.OK.equals(resp.getAction())) {
                Park p = (Park) resp.getPayload();
                allParks.add(p);
                parkFilterComboBox.getItems().add(p);
            }
        }
        parkFilterComboBox.getSelectionModel().selectFirst(); // Default to "All Parks"
    }

    /**
     * Fetches pending park parameter proposals from the server and
     * refreshes the parks table.
     */
    private void refreshPendingParks() {
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_PENDING_PARKS, null));
        if (Message.OK.equals(response.getAction())) {
            @SuppressWarnings("unchecked")
            List<Park> list = (List<Park>) response.getPayload();
            pendingParks.clear();
            pendingParks.addAll(list);
            statusLabel.setText("Pending park parameter updates refreshed.");
        } else {
            statusLabel.setText("Failed to load pending parks updates.");
        }
    }

    /**
     * Fetches pending promotion proposals from the server and refreshes
     * the promotions table.
     */
    private void refreshPendingPromotions() {
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_PENDING_PROMOTIONS, null));
        if (Message.OK.equals(response.getAction())) {
            @SuppressWarnings("unchecked")
            List<Promotion> list = (List<Promotion>) response.getPayload();
            pendingPromotions.clear();
            pendingPromotions.addAll(list);
            statusLabel.setText("Pending promotions list refreshed.");
        } else {
            statusLabel.setText("Failed to load pending promotions.");
        }
    }

    /** Approves the selected park parameter proposal. */
    @FXML
    public void onApprovePark() {
        resolveParkProposal(true);
    }

    /** Rejects the selected park parameter proposal. */
    @FXML
    public void onRejectPark() {
        resolveParkProposal(false);
    }

    /**
     * Sends an approve or reject decision for the selected park proposal to
     * the server, then refreshes the table.
     *
     * @param approve true to approve, false to reject
     */
    private void resolveParkProposal(boolean approve) {
        Park sel = parksTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Object[] payload = new Object[]{sel.getParkId(), approve};
        Message response = ClientUI.client.sendRequest(new Message(Message.APPROVE_PARK_PARAMETERS, payload));

        if (Message.OK.equals(response.getAction())) {
            showAlert("Success", "Proposal Resolved", "Park parameter changes proposed for " + sel.getParkName() + 
                      " have been " + (approve ? "APPROVED" : "REJECTED") + ".");
            refreshPendingParks();
        } else {
            showAlert("Error", "Action Failed", response.getPayload().toString());
        }
    }

    /** Approves the selected promotion proposal. */
    @FXML
    public void onApprovePromo() {
        resolvePromoProposal(true);
    }

    /** Rejects the selected promotion proposal. */
    @FXML
    public void onRejectPromo() {
        resolvePromoProposal(false);
    }

    /**
     * Sends an approve or reject decision for the selected promotion to
     * the server, then refreshes the table.
     *
     * @param approve true to approve, false to reject
     */
    private void resolvePromoProposal(boolean approve) {
        Promotion sel = promotionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Object[] payload = new Object[]{sel.getPromotionId(), approve};
        Message response = ClientUI.client.sendRequest(new Message(Message.APPROVE_PROMOTION, payload));

        if (Message.OK.equals(response.getAction())) {
            showAlert("Success", "Promotion Resolved", "Promotion campaign " + sel.getPromotionName() + 
                      " has been " + (approve ? "APPROVED" : "REJECTED") + ".");
            refreshPendingPromotions();
        } else {
            showAlert("Error", "Action Failed", response.getPayload().toString());
        }
    }

    /**
     * Fetches and renders the monthly visit-duration and cancellation
     * bar charts for the selected month, year, and park filter.
     * A park ID of 0 aggregates across all parks.
     */
    @FXML
    public void onGenerateReports() {
        Integer month = monthComboBox.getValue();
        Integer year = yearComboBox.getValue();
        Object filter = parkFilterComboBox.getValue();

        if (month == null || year == null || filter == null) return;

        int parkId = 0; // 0 represents "All Parks"
        if (filter instanceof Park) {
            parkId = ((Park) filter).getParkId();
        }

        // Clear existing charts
        durationChart.getData().clear();
        cancellationChart.getData().clear();

        // 1. Load Visits Duration Report (Total region)
        Object[] durationPayload = new Object[]{parkId, month, year};
        Message vResp = ClientUI.client.sendRequest(new Message(Message.GET_MONTHLY_VISITS_REPORT, durationPayload));
        if (Message.OK.equals(vResp.getAction())) {
            @SuppressWarnings("unchecked")
            Map<String, List<Integer>> data = (Map<String, List<Integer>>) vResp.getPayload();
            
            List<Integer> indList = data.getOrDefault("INDIVIDUAL", List.of(0, 0, 0, 0));
            List<Integer> grpList = data.getOrDefault("GROUP", List.of(0, 0, 0, 0));

            XYChart.Series<String, Number> indSeries = new XYChart.Series<>();
            indSeries.setName("Individuals/Subscribers");
            indSeries.getData().add(new XYChart.Data<>("0-2 hrs", indList.get(0)));
            indSeries.getData().add(new XYChart.Data<>("2-4 hrs", indList.get(1)));
            indSeries.getData().add(new XYChart.Data<>("4-6 hrs", indList.get(2)));
            indSeries.getData().add(new XYChart.Data<>("6+ hrs", indList.get(3)));

            XYChart.Series<String, Number> grpSeries = new XYChart.Series<>();
            grpSeries.setName("Organized Groups");
            grpSeries.getData().add(new XYChart.Data<>("0-2 hrs", grpList.get(0)));
            grpSeries.getData().add(new XYChart.Data<>("2-4 hrs", grpList.get(1)));
            grpSeries.getData().add(new XYChart.Data<>("4-6 hrs", grpList.get(2)));
            grpSeries.getData().add(new XYChart.Data<>("6+ hrs", grpList.get(3)));

            durationChart.getData().addAll(indSeries, grpSeries);
        }

        // 2. Load Cancellations Distribution Report (Hourly segments)
        Object[] cancelPayload = new Object[]{parkId, month, year};
        Message cResp = ClientUI.client.sendRequest(new Message(Message.GET_MONTHLY_CANCELLATIONS_REPORT, cancelPayload));
        if (Message.OK.equals(cResp.getAction())) {
            @SuppressWarnings("unchecked")
            Map<String, Map<Integer, Integer>> data = (Map<String, Map<Integer, Integer>>) cResp.getPayload();
            
            Map<Integer, Integer> cancels = data.getOrDefault("CANCELLED", new HashMap<>());
            Map<Integer, Integer> noShows = data.getOrDefault("NO_SHOW", new HashMap<>());

            XYChart.Series<String, Number> cancelSeries = new XYChart.Series<>();
            cancelSeries.setName("Canceled by Traveler");

            XYChart.Series<String, Number> noShowSeries = new XYChart.Series<>();
            noShowSeries.setName("System No-Show Cancel");

            for (int h = 8; h <= 20; h++) {
                String hrStr = String.format("%02d:00", h);
                cancelSeries.getData().add(new XYChart.Data<>(hrStr, cancels.getOrDefault(h, 0)));
                noShowSeries.getData().add(new XYChart.Data<>(hrStr, noShows.getOrDefault(h, 0)));
            }

            cancellationChart.getData().addAll(cancelSeries, noShowSeries);
        }
        
        statusLabel.setText("Department reports charts rendered.");
    }

    /**
     * Logs out the current user, stops the clock, and returns to the
     * login screen.
     */
    @FXML
    public void onLogout() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        ClientUI.client.sendRequest(new Message(Message.LOGOUT, ClientUI.currentUser.getUsername()));
        ClientUI.currentUser = null;
        ClientUI.setRoot("/gui/LoginUI.fxml", "GoNature - Login Portal", 500, 670);
    }

    /**
     * Fetches the simulation start time and speedup factor from the server
     * and starts the clock timeline.
     */
    private void initSimClock() {
        Message response = ClientUI.client.sendRequest(new Message(Message.GET_SIMULATION_TIME, null));
        if (Message.OK.equals(response.getAction())) {
            Object[] payload = (Object[]) response.getPayload();
            simStartMs = (Long) payload[0];
            simSpeedup = (Double) payload[1];
            clientSyncTime = System.currentTimeMillis();
            startClockTimeline();
        }
    }

    /**
     * Starts a timeline that updates simClockLabel every 250 ms with the
     * current simulated time, scaled by simSpeedup.
     */
    private void startClockTimeline() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        clockTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(250), event -> {
            long now = System.currentTimeMillis();
            long elapsedReal = now - clientSyncTime;
            long elapsedSim = (long)(elapsedReal * simSpeedup);
            long currentSim = getSimulatedTimeAtSync() + elapsedSim;
            
            java.sql.Timestamp ts = new java.sql.Timestamp(currentSim);
            java.time.LocalDateTime ldt = ts.toLocalDateTime();
            String formatted = ldt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            if (simClockLabel != null) {
                simClockLabel.setText("Simulated Time: " + formatted);
            }
        }));
        clockTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        clockTimeline.play();
    }

    /**
     * Returns the simulated timestamp at the moment of last server sync.
     *
     * @return simulated epoch milliseconds at sync time
     */
    private long getSimulatedTimeAtSync() {
        long elapsedReal = clientSyncTime - simStartMs;
        return simStartMs + (long)(elapsedReal * simSpeedup);
    }

    /**
     * Displays an information alert dialog to the user.
     *
     * @param title   the title of the alert
     * @param header  the header text
     * @param content the message content
     */
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
