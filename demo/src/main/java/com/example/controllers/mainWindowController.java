package com.example.controllers;

import com.example.OAuthRedirectServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

import java.awt.Desktop;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

public class mainWindowController {

    private OAuthRedirectServer server;
    private static String access_token = "";

    private static final String CLIENT_ID = "3aea31e0bb0ac19a4de7f6b58b276575ec6416174658183c795dfbf18a18f3d1";

    @FXML
    private Button loginButton;

    @FXML
    private Text userInfo;


    @FXML
    void loginButtonPressed(ActionEvent event) {
        startCallbackServer();
        try {
            String oauthUrl = "https://gitlab.com/oauth/authorize?client_id=" + CLIENT_ID + "&redirect_uri=http://localhost:8080/callback&response_type=code&scope=api";
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(oauthUrl));
            } else {
                System.out.println("Desktop browsing is not supported.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (access_token == "") {
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateGUIUserInfo();
    }

    private void startCallbackServer() {
        server = new OAuthRedirectServer();
        try {
            server.start();
            System.out.println("Callback server started on port 8080");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static void setAccessToken(String token){
        access_token = token;
        System.out.println("set access token to " + access_token);
    }
    
    public HttpResponse<String> requestUserInfo(){
        try {
            
            HttpClient client = HttpClient.newHttpClient();
    
            HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("https://gitlab.com/api/v4/user"))
            .header("Authorization", "Bearer " + access_token)
            .build();
    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Response: " + response.body());
                return response;
            } else {
                throw new Exception("Request failed. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

    public void updateGUIUserInfo(){
        HttpResponse<String> response = requestUserInfo();
        JSONObject json = new JSONObject(response.body());
        String username = json.getString("username");
        String email = json.getString("commit_email");
        userInfo.setText(
            "Welcome, " + username + "\n" +
            "E-mail: " +  email
        );

    }
}
