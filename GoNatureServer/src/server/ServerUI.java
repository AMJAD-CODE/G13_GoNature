package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the GoNature Server application.
 *
 * <p>This class is responsible for launching the server's graphical
 * user interface (GUI). It loads the server control panel from the
 * FXML file and displays the main application window.</p>
 *
 * <p>The application provides administrators with a central interface
 * for monitoring and managing the GoNature server.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
public class ServerUI extends Application {

	/**
	 * Launches the JavaFX server application.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
        launch(args);
    }

	/**
	 * Initializes and displays the main server window.
	 *
	 * <p>Loads the server user interface from the FXML file,
	 * configures the application stage, and handles application
	 * shutdown when the window is closed.</p>
	 *
	 * @param stage the primary stage provided by the JavaFX runtime
	 * @throws Exception if the FXML file cannot be loaded or the
	 *                   user interface fails to initialize
	 */
	@Override
	public void start(Stage stage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ServerUI.fxml"));
        stage.setTitle("GoNature Central Server Control");
        stage.setScene(new Scene(root, 1000, 600));
        
        // Handle window close cleanly
        stage.setOnCloseRequest(event -> {
            System.exit(0);
        });
        
        stage.show();
    }
}
