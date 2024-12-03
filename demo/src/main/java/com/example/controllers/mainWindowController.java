package com.example.controllers;

import com.example.OAuthRedirectServer;
import com.example.mainWindowGUI;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import java.util.concurrent.CompletableFuture;

public class mainWindowController {

    private OAuthRedirectServer server;
    private static String access_token = "";

    private static CompletableFuture<String> accesstokenfuture = new CompletableFuture<>();
    private boolean server_running = false;
    private static boolean access_token_set = false;

    private static final String CLIENT_ID = "3aea31e0bb0ac19a4de7f6b58b276575ec6416174658183c795dfbf18a18f3d1";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    private static final String API_BASE_URL = "https://gitlab.com/api/v4";

    @FXML
    private Button loginButton;
    @FXML
    private Text userInfo;
    @FXML
    private ListView<Integer> repoList;
    @FXML
    private AnchorPane loginPage;
    @FXML
    private AnchorPane mainPage;


    @FXML
    void loginButtonPressed(ActionEvent event) {
        if (!server_running) {
            startCallbackServer();
            server_running = true;
        }
        if (!isOnline()) {
            return;
        }
        try {
            String oauthUrl = "https://gitlab.com/oauth/authorize?client_id=" + CLIENT_ID + "&redirect_uri="
                    + REDIRECT_URI + "&response_type=code&scope=api";
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(oauthUrl));
            } else {
                System.out.println("Desktop browsing is not supported.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        waitForAccessTokenFuture();
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

    public static void setAccessToken(String token) {
        access_token = token;
        if (!access_token_set) {
        }
        accesstokenfuture.complete(token);
        System.out.println("set access token to " + access_token);
    }

    public static CompletableFuture<String> getAccessTokenFuture() {
        return accesstokenfuture;
    }

    public void waitForAccessTokenFuture() {
        CompletableFuture<String> future = getAccessTokenFuture();
        future.thenAccept(token -> {
            System.out.println("Access token recieved: " + token);
            loginUser();
        });
    }

    /**
     * Make specific api requests using this method
     * @param endpoint the specific endpoint you are trying to access
     * @return the json response from the endpoint of GitLab API
     */
    public HttpResponse<String> APIRequest(String endpoint) {
        try {

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(API_BASE_URL + "" + endpoint))
                    .header("Authorization", "Bearer " + access_token)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // System.out.println("Response: " + response.body());
                return response;
            } else {
                throw new Exception("Request failed. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void loginUser() {
        ObservableList<Integer> repos = FXCollections.observableArrayList();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (!isOnline()) {
                    return null;
                }
                
                // Perform network calls in the background and keep seperate from GUI thread

                long start = System.currentTimeMillis();
                HttpResponse<String> userInfoResponse = APIRequest("/user");
                long duration = System.currentTimeMillis() - start;
                System.out.println("response time " + duration + " ms");

                JSONObject userInfoJson = new JSONObject(userInfoResponse.body());
                String username = userInfoJson.getString("username");
                String email = userInfoJson.getString("commit_email");

                HttpResponse<String> userAuthResponse = APIRequest("/projects/61935538");

                /**
                 * Check to see if logged in user is allowed to 
                 * access our pipeline if they are not the response 
                 * will be null then display an error
                 */
                if (userAuthResponse == null) {
                    Platform.runLater(() -> {
                        mainWindowGUI.error("Unauthorized", "This account is unauthorized to access the pipeline resource!\nPlease sign in with a different GitLab account that is authorized!");
                    });
                    return null;
                }
                JSONObject projectInfoJson = new JSONObject(userAuthResponse.body());
                System.out.println(projectInfoJson);
                repos.clear();
                
                // Update the GUI on the JavaFX Application thread
                Platform.runLater(() -> {
                    userInfo.setText("Welcome, " + username + "\nE-mail: " + email);
                    repoList.setItems(repos);
                    mainPage.setVisible(true);
                    loginPage.setVisible(false);
                });
                return null;
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                exception.printStackTrace();
                Platform.runLater(() -> {
                    userInfo.setText("Login was unsuccessful");
                    repoList.setVisible(false);
                });
            }
        };

        new Thread(task).start();
    }

    public boolean isOnline() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("google.com", 80), 2000);
            return true;
        } catch (Exception e) {
            mainWindowGUI.error("Offline", "You appear to be offline, please check your connection and try logging in again");
            return false;
        }
    }
}
