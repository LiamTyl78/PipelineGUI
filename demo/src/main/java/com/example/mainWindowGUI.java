package com.example;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class mainWindowGUI extends Application{
    private static Stage login = new Stage();
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent mainWindow = FXMLLoader.load(getClass().getResource("/mainWindow.fxml"));
        Scene s = new Scene(mainWindow);
        primaryStage.setTitle("GitLab");
        primaryStage.setScene(s);
        primaryStage.setResizable(false);
        primaryStage.show();

        login.setTitle("login to gitlab");
        login.initModality(Modality.APPLICATION_MODAL);
        login.setResizable(false);
    }

     public static void error(String title, String message){
        Alert error = new Alert(AlertType.ERROR);
        error.setTitle(title);
        error.setHeaderText(message);
        error.show();
    }

}