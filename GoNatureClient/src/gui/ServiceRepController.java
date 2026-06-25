package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import client.ClientUI;
import common.Message;
import common.Subscriber;
import common.User;

public class ServiceRepController {

    // Subscriber Fields
    @FXML
    private TextField subIdField;
    @FXML
    private TextField subFirstNameField;
    @FXML
    private TextField subLastNameField;
    @FXML
    private TextField subEmailField;
    @FXML
    private TextField subPhoneField;
    @FXML
    private TextField subFamilySizeField;
    @FXML
    private TextField subCreditCardField;

    // Guide Fields
    @FXML
    private TextField guideUsernameField;
    @FXML
    private PasswordField guidePasswordField;
    @FXML
    private TextField guideFirstNameField;
    @FXML
    private TextField guideLastNameField;
    @FXML
    private TextField guideEmailField;

    // Search Profile Fields
    @FXML
    private TextField searchQueryField;
    @FXML
    private GridPane profileResultGrid;
    @FXML
    private Label resProfileType;
    @FXML
    private Label resFirstName;
    @FXML
    private Label resLastName;
    @FXML
    private Label resEmail;
    @FXML
    private Label resPhone;
    @FXML
    private Label resExtraInfo;

    @FXML
    private Label statusLabel;

    @FXML
    private Label simClockLabel;
    private javafx.animation.Timeline clockTimeline;
    private long simStartMs = 0;
    private double simSpeedup = 1.0;
    private long clientSyncTime = 0;

    @FXML
    public void initialize() {
        initSimClock();
    }


    @FXML
    public void onRegisterSubscriber() {
        String id = subIdField.getText().trim();
        String firstName = subFirstNameField.getText().trim();
        String lastName = subLastNameField.getText().trim();
        String email = subEmailField.getText().trim();
        String phone = subPhoneField.getText().trim();
        String familySizeStr = subFamilySizeField.getText().trim();
        String creditCard = subCreditCardField.getText().trim();

        if (id.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || familySizeStr.isEmpty()) {
            statusLabel.setText("ERROR: Please fill in all subscriber fields.");
            return;
        }

        if (!email.contains("@")) {
            statusLabel.setText("ERROR: Email must contain '@' character.");
            return;
        }

        String cleanPhone = phone.replace("-", "").replace(" ", "");
        if (cleanPhone.length() != 10 || !cleanPhone.matches("\\d+")) {
            statusLabel.setText("ERROR: Phone number must contain exactly 10 digits.");
            return;
        }

        if (id.length() != 9) {
            statusLabel.setText("ERROR: National ID number must be exactly 9 digits.");
            return;
        }

        int familySize;
        try {
            familySize = Integer.parseInt(familySizeStr);
            if (familySize <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            statusLabel.setText("ERROR: Family size must be a positive integer.");
            return;
        }

        Subscriber sub = new Subscriber(0, id, firstName, lastName, email, phone, familySize, creditCard.isEmpty() ? null : creditCard);

        Message response = ClientUI.client.sendRequest(new Message(Message.REGISTER_SUBSCRIBER, sub));
        
        if (Message.OK.equals(response.getAction())) {
            Subscriber saved = (Subscriber) response.getPayload();
            showAlert("Subscriber Registered", "Club Membership Active",
                      "Subscriber successfully registered!\n" +
                      "Assigned Subscriber Number: " + saved.getSubscriberId() + "\n" +
                      "Name: " + saved.getFirstName() + " " + saved.getLastName() + "\n" +
                      "Family Size: " + saved.getFamilySize());
            clearSubscriberFields();
            statusLabel.setText("Subscriber registered successfully.");
        } else {
            showAlert("Error", "Registration Failed", response.getPayload().toString());
        }
    }

    @FXML
    public void onRegisterGuide() {
        String username = guideUsernameField.getText().trim();
        String password = guidePasswordField.getText();
        String firstName = guideFirstNameField.getText().trim();
        String lastName = guideLastNameField.getText().trim();
        String email = guideEmailField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            statusLabel.setText("ERROR: Please fill in all guide fields.");
            return;
        }

        if (!email.contains("@")) {
            statusLabel.setText("ERROR: Email must contain '@' character.");
            return;
        }

        User guide = new User(username, password, firstName, lastName, "GUIDE", email, null);

        Message response = ClientUI.client.sendRequest(new Message(Message.REGISTER_GUIDE, guide));

        if (Message.OK.equals(response.getAction())) {
            showAlert("Guide Registered", "Guide Account Created",
                      "Guide account is now active!\n" +
                      "Username: " + username + "\n" +
                      "Guides can login to book and manage group visits.");
            clearGuideFields();
            statusLabel.setText("Guide registered successfully.");
        } else {
            showAlert("Error", "Registration Failed", response.getPayload().toString());
        }
    }

    private void clearSubscriberFields() {
        subIdField.clear();
        subFirstNameField.clear();
        subLastNameField.clear();
        subEmailField.clear();
        subPhoneField.clear();
        subFamilySizeField.clear();
        subCreditCardField.clear();
    }

    private void clearGuideFields() {
        guideUsernameField.clear();
        guidePasswordField.clear();
        guideFirstNameField.clear();
        guideLastNameField.clear();
        guideEmailField.clear();
    }

    @FXML
    public void onSearchUserProfile() {
        String query = searchQueryField.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("ERROR: Please enter an ID or Username to search.");
            profileResultGrid.setVisible(false);
            profileResultGrid.setManaged(false);
            return;
        }

        Message response = ClientUI.client.sendRequest(new Message(Message.GET_USER_PROFILE, query));
        if (Message.OK.equals(response.getAction())) {
            Object payload = response.getPayload();
            if (payload instanceof Subscriber) {
                Subscriber sub = (Subscriber) payload;
                resProfileType.setText("Subscriber");
                resFirstName.setText(sub.getFirstName());
                resLastName.setText(sub.getLastName());
                resEmail.setText(sub.getEmail());
                resPhone.setText(sub.getPhoneNumber());
                resExtraInfo.setText("Sub ID: " + sub.getSubscriberId() + " | Family Size: " + sub.getFamilySize());
            } else if (payload instanceof User) {
                User user = (User) payload;
                resProfileType.setText("User / Employee");
                resFirstName.setText(user.getFirstName());
                resLastName.setText(user.getLastName());
                resEmail.setText(user.getEmail());
                resPhone.setText("-");
                String parkInfo = (user.getAssignedParkId() != null) ? "Park ID: " + user.getAssignedParkId() : "No assigned park";
                resExtraInfo.setText("Role: " + user.getRole() + " | Username: " + user.getUsername() + " | " + parkInfo);
            }
            profileResultGrid.setVisible(true);
            profileResultGrid.setManaged(true);
            statusLabel.setText("Profile details loaded.");
        } else {
            profileResultGrid.setVisible(false);
            profileResultGrid.setManaged(false);
            statusLabel.setText("ERROR: " + response.getPayload());
            showAlert("Search Failed", "Profile Not Found", response.getPayload().toString());
        }
    }

    @FXML
    public void onLogout() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        ClientUI.client.sendRequest(new Message(Message.LOGOUT, ClientUI.currentUser.getUsername()));
        ClientUI.currentUser = null;
        ClientUI.setRoot("/gui/LoginUI.fxml", "GoNature - Login Portal", 500, 670);
    }

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

    private long getSimulatedTimeAtSync() {
        long elapsedReal = clientSyncTime - simStartMs;
        return simStartMs + (long)(elapsedReal * simSpeedup);
    }


    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
