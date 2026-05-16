package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the GoNature server.
 * Run this class as a Java Application.
 */
public class ServerUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ServerUI.fxml"));
        stage.setTitle("GoNature Server");
        stage.setScene(new Scene(root, 640, 420));
        stage.show();
    }
}