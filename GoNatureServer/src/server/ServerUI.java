package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
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
