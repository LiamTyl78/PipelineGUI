package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class mainWindowGUI extends Application{
    private static Stage main = new Stage();
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent mainWindow = FXMLLoader.load(getClass().getResource("/mainWindow.fxml"));
        Scene s = new Scene(mainWindow);
        main.setTitle("Pipeline GUI");
        main.setScene(s);
        main.setResizable(false);
        main.show();
        // main.setAlwaysOnTop(true);
    }

    public static void error(String title, String message){
        Alert error = new Alert(AlertType.ERROR);
        error.setTitle(title);
        error.setHeaderText(message);
        error.show();
    }

    public static void mainWindowFront(){
        main.setAlwaysOnTop(true);
        main.setAlwaysOnTop(false);
    }

}