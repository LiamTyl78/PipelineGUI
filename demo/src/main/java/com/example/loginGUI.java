package com.example;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class loginGUI extends Application{
    private static Stage login = new Stage();
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent mainWindow = FXMLLoader.load(getClass().getResource("/mainWindow.fxml"));
        Parent loginWindow = FXMLLoader.load(getClass().getResource("/loginWindow.fxml"));
        Scene s = new Scene(mainWindow);
        Scene s2 = new Scene(loginWindow);
        primaryStage.setTitle("GitLab");
        primaryStage.setScene(s);
        primaryStage.setResizable(false);
        primaryStage.show();

        login.setScene(s2);
        login.setTitle("login to gitlab");
        login.initModality(Modality.APPLICATION_MODAL);
        login.setResizable(false);
    }

    public static void showLoginScreen(){
        login.show();
    }
}