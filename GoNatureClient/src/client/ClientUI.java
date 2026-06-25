package client;

import javafx.application.Application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import common.User;

/**
 * JavaFX entry point for the GoNature client application.
 * Responsible for launching the UI, holding application-wide state
 * such as the connected client and logged-in user, and switching
 * between scenes on the main stage.
 */
public class ClientUI extends Application {

    public static GoNatureClient client = null;
    public static User currentUser = null; // Stored upon successful login
    public static Stage mainStage = null;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Called by the JavaFX runtime to initialize the primary stage.
     * Loads the connection screen and registers a close handler
     * that disconnects the client before the application exits.
     *
     * @param stage the primary stage provided by JavaFX
     * @throws Exception if the initial FXML view cannot be loaded
     */
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
     *
     * @param fxmlPath path to the FXML file to load, relative to the classpath
     * @param title the new window title
     * @param width the new window width
     * @param height the new window height
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
