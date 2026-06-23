package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import common.User;

public class ClientUI extends Application {

    public static GoNatureClient client = null;
    public static User currentUser = null; // Stored upon successful login
    public static Stage mainStage = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        mainStage = stage;
        
        // Load the connection screen first
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ConnectionUI.fxml"));
        stage.setTitle("GoNature Workstation - Connect");
        stage.setScene(new Scene(root, 400, 400));
        
        // Clean up connection on close
        stage.setOnCloseRequest(event -> {
            if (client != null) {
                client.quit();
            }
            System.exit(0);
        });
        
        stage.show();
    }

    /**
     * Switch the root scene of the main window.
     */
    public static void setRoot(String fxmlPath, String title, double width, double height) {
        try {
            Parent root = FXMLLoader.load(ClientUI.class.getResource(fxmlPath));
            mainStage.setTitle(title);
            mainStage.getScene().setRoot(root);
            mainStage.setWidth(width);
            mainStage.setHeight(height);
            mainStage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("Error switching scene to " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
