package com.example;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class loginGUI extends Application{
    Stage stage = new Stage();
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/loginWindow.fxml"));
        Scene s = new Scene(root);
        primaryStage.setTitle("GitLab");
        primaryStage.setScene(s);
        primaryStage.setResizable(false);
        primaryStage.show();
        stage = primaryStage;
    }
}