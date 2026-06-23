package gui;

import java.time.LocalDate;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import client.ClientUI;
import common.Message;
import common.Park;
import common.Promotion;

public class ParkManagerController {

    @FXML
    private Label parkLabel;
    @FXML
    private Label managerLabel;
    @FXML
    private Label statusLabel;

    // Parameters Config
    @FXML
    private Label curCapacity;
    @FXML
    private Label curGap;
    @FXML
    private Label curDuration;
    @FXML
    private Label pendingLabel;
    @FXML
    private TextField newCapacityField;
    @FXML
    private TextField newGapField;
    @FXML
    private TextField newDurationField;

    // Promotions
    @FXML
    private TextField promoNameField;
    @FXML
    private TextField promoDiscountField;
    @FXML
    private DatePicker promoStartPicker;
    @FXML
    private DatePicker promoEndPicker;

    // Reports Combo Box
    @FXML
    private ComboBox<Integer> monthComboBox;
    @FXML
    private ComboBox<Integer> yearComboBox;

    // Charts
    @FXML
    private BarChart<String, Number> visitorChart;
    @FXML
    private BarChart<String, Number> usageChart;

    private Park currentPark = null;

    @FXML
    public void initialize() {
        managerLabel.setText("Manager: " + ClientUI.currentUser.getUsername());

        // Setup Month/Year Combo Boxes
        for (int m = 1; m <= 12; m++) {
            monthComboBox.getItems().add(m);
        }
        for (int y = 2025; y <= 2027; y++) {
            yearComboBox.getItems().add(y);
        }
        
        LocalDate now = LocalDate.now();
        monthComboBox.setValue(now.getMonthValue());
        yearComboBox.setValue(now.getYear());

        // Load parameter values
        refreshParameters();
    }

    private void refreshParameters() {
        if (ClientUI.currentUser.getAssignedParkId() == null) {
            statusLabel.setText("ERROR: No assigned park for this manager.");
            return;
        }

        int parkId = ClientUI.currentUser.getAssignedParkId();
        Message pResp = ClientUI.client.sendRequest(new Message(Message.GET_PARK, parkId));
        
        if (Message.OK.equals(pResp.getAction())) {
            currentPark = (Park) pResp.getPayload();
            parkLabel.setText("Park: " + currentPark.getParkName());
            
            curCapacity.setText(String.valueOf(currentPark.getMaxQuota()));
            curGap.setText(String.valueOf(currentPark.getReservedGap()));
            curDuration.setText(String.valueOf(currentPark.getStayDuration()));

            if ("PENDING_APPROVAL".equals(currentPark.getPendingChangesStatus())) {
                pendingLabel.setText("Pending Approval by Department Manager:\n" +
                                     "- Max Quota: " + currentPark.getPendingMaxQuota() + "\n" +
                                     "- Gap Limit: " + currentPark.getPendingReservedGap() + "\n" +
                                     "- Stay Duration: " + currentPark.getPendingStayDuration());
                pendingLabel.setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold;");
            } else {
                pendingLabel.setText("Current configuration is active. No pending approvals.");
                pendingLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            }
        } else {
            statusLabel.setText("ERROR loading parameters.");
        }
    }

    @FXML
    public void onSubmitParameters() {
        String capStr = newCapacityField.getText().trim();
        String gapStr = newGapField.getText().trim();
        String durStr = newDurationField.getText().trim();

        if (capStr.isEmpty() || gapStr.isEmpty() || durStr.isEmpty()) {
            statusLabel.setText("ERROR: Please fill in all proposal fields.");
            return;
        }

        try {
            int newCap = Integer.parseInt(capStr);
            int newGap = Integer.parseInt(gapStr);
            int newDur = Integer.parseInt(durStr);

            if (newCap <= 0 || newGap < 0 || newDur <= 0) throw new NumberFormatException();

            // Set proposed details into object
            currentPark.setPendingMaxQuota(newCap);
            currentPark.setPendingReservedGap(newGap);
            currentPark.setPendingStayDuration(newDur);

            Message response = ClientUI.client.sendRequest(new Message(Message.UPDATE_PARK_PARAMETERS, currentPark));
            if (Message.OK.equals(response.getAction())) {
                showAlert("Success", "Parameters Submitted", "Your parameters change proposal has been forwarded to the Department Manager for review.");
                newCapacityField.clear();
                newGapField.clear();
                newDurationField.clear();
                refreshParameters();
            } else {
                statusLabel.setText("ERROR submitting parameters: " + response.getPayload());
            }

        } catch (NumberFormatException e) {
            statusLabel.setText("ERROR: Input parameters must be positive integers.");
        }
    }

    @FXML
    public void onSubmitPromotion() {
        String name = promoNameField.getText().trim();
        String discountStr = promoDiscountField.getText().trim();
        LocalDate start = promoStartPicker.getValue();
        LocalDate end = promoEndPicker.getValue();

        if (name.isEmpty() || discountStr.isEmpty() || start == null || end == null) {
            statusLabel.setText("ERROR: Please fill in all promotion fields.");
            return;
        }

        try {
            double rate = Double.parseDouble(discountStr);
            if (rate <= 0 || rate >= 100) {
                statusLabel.setText("ERROR: Discount rate must be between 1% and 99%.");
                return;
            }

            if (end.isBefore(start)) {
                statusLabel.setText("ERROR: Campaign end date cannot be before start date.");
                return;
            }

            Promotion p = new Promotion();
            p.setParkId(currentPark.getParkId());
            p.setPromotionName(name);
            p.setDiscountPercentage(rate / 100.0);
            p.setStartDate(Date.valueOf(start));
            p.setEndDate(Date.valueOf(end));
            p.setStatus("PENDING_APPROVAL");

            Message response = ClientUI.client.sendRequest(new Message(Message.CREATE_PROMOTION, p));
            if (Message.OK.equals(response.getAction())) {
                showAlert("Success", "Promotion Submitted", "The promotion has been submitted to the Department Manager for review.");
                promoNameField.clear();
                promoDiscountField.clear();
                promoStartPicker.setValue(null);
                promoEndPicker.setValue(null);
                statusLabel.setText("Promotion request submitted.");
            } else {
                showAlert("Error", "Submission Failed", response.getPayload().toString());
            }

        } catch (NumberFormatException e) {
            statusLabel.setText("ERROR: Invalid discount rate format.");
        }
    }

    @FXML
    public void onGenerateReports() {
        Integer month = monthComboBox.getValue();
        Integer year = yearComboBox.getValue();

        if (month == null || year == null) return;

        // Clear existing charts
        visitorChart.getData().clear();
        usageChart.getData().clear();

        // Load visitor count report
        Object[] payload = new Object[]{currentPark.getParkId(), month, year};
        Message vResp = ClientUI.client.sendRequest(new Message(Message.GET_MONTHLY_VISITOR_REPORT, payload));
        if (Message.OK.equals(vResp.getAction())) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> map = (Map<String, Integer>) vResp.getPayload();
            
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Visitors count");
            series.getData().add(new XYChart.Data<>("Individuals", map.getOrDefault("INDIVIDUAL", 0)));
            series.getData().add(new XYChart.Data<>("Subscribers", map.getOrDefault("FAMILY_SUBSCRIBER", 0)));
            series.getData().add(new XYChart.Data<>("Groups", map.getOrDefault("ORGANIZED_GROUP", 0)));
            visitorChart.getData().add(series);
        }

        // Load occupancy usage report
        Message uResp = ClientUI.client.sendRequest(new Message(Message.GET_MONTHLY_USAGE_REPORT, payload));
        if (Message.OK.equals(uResp.getAction())) {
            @SuppressWarnings("unchecked")
            Map<Integer, Double> map = (Map<Integer, Double>) uResp.getPayload();
            
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Average Daily occupancy %");
            
            // Show average daily load
            for (int day = 1; day <= 31; day++) {
                if (map.containsKey(day)) {
                    series.getData().add(new XYChart.Data<>(String.valueOf(day), map.get(day)));
                }
            }
            usageChart.getData().add(series);
        }
        
        statusLabel.setText("Performance charts rendered.");
    }

    @FXML
    public void onLogout() {
        ClientUI.client.sendRequest(new Message(Message.LOGOUT, ClientUI.currentUser.getUsername()));
        ClientUI.currentUser = null;
        ClientUI.setRoot("/gui/LoginUI.fxml", "GoNature - Login Portal", 500, 670);
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
