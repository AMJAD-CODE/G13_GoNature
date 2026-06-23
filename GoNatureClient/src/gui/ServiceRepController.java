package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

    @FXML
    private Label statusLabel;

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
