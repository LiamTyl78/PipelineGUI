package com.example.controllers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class loginController {

    @FXML
    private WebView gitlabLogin;

    @FXML
    private Button logInButton;

    @FXML
    void loginButtonPressed(ActionEvent event) {
            WebEngine webEngine = gitlabLogin.getEngine();
            webEngine.load("https://gitlab.com/oauth/authorize?client_id=3aea31e0bb0ac19a4de7f6b58b276575ec6416174658183c795dfbf18a18f3d1&redirect_uri=http://localhost:1801/callback&response_type=code&scope=api");
    }

}
