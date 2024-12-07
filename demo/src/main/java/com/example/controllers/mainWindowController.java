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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class mainWindowController {

    private OAuthRedirectServer server;
    private static String access_token = "";

    private static CompletableFuture<String> accesstokenfuture = new CompletableFuture<>();
    private boolean server_running = false;
    private static boolean access_token_set = false;

    private static final String CLIENT_ID = "fd6b326760c2de5aeee3e08f85f0e2197821a8000663b76f055463722b722e10";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    private static final String API_BASE_URL = "https://gitlab.com/api/v4";

    @FXML
    private Button loginButton;
    @FXML
    private Text welcomeMessage;
    @FXML
    private ListView<String> pipelineList;
    @FXML
    private AnchorPane loginPage;
    @FXML
    private AnchorPane mainPage;
    @FXML
    private ProgressBar loginProgressIndicator;
    @FXML
    private TabPane pipelineTabs;
    @FXML
    private Text testResultsLabel;

    @FXML
    private ImageView buildIcon;
    @FXML
    private ImageView deployIcon;
    @FXML
    private ImageView testIcon;
    @FXML
    private ImageView packageIcon;

    @FXML
    private Text buildStatusLabel;
    @FXML
    private Text testStatusLabel;
    @FXML
    private Text packageStatusLabel;
    @FXML
    private Text deployStatusLabel;


    ArrayList<Integer> pipelineIds = new ArrayList<>();

    public void pressLoginButton(){
        loginButton.fire();
    }

    @FXML
    void loginButtonPressed(ActionEvent event) {
        loginProgressIndicator.setVisible(true);
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
     * 
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
        ObservableList<String> pipelineTests = FXCollections.observableArrayList();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                if (!isOnline()) {
                    loginProgressIndicator.setVisible(false);
                    return null;
                }

                // Perform network calls in the background and keep seperate from GUI thread

                long start = System.currentTimeMillis();
                HttpResponse<String> userInfoResponse = APIRequest("/user");
                long duration = System.currentTimeMillis() - start;
                System.out.println("response time " + duration + " ms");

                JSONObject userInfoJson = new JSONObject(userInfoResponse.body());
                String username = userInfoJson.getString("username");
                // String email = userInfoJson.getString("commit_email");

                HttpResponse<String> userAuthResponse = APIRequest("/projects/61935538");

                /**
                 * Check to see if logged in user is allowed to
                 * access our pipeline if they are not the response
                 * will be null, then display an error
                 */
                if (userAuthResponse == null) {
                    Platform.runLater(() -> {
                        loginProgressIndicator.setVisible(false);
                        mainWindowGUI.mainWindowFront();
                        mainWindowGUI.error("Unauthorized",
                                "This account is unauthorized to access the pipeline resource!\nPlease sign in with a different GitLab account that is authorized!");
                    });
                    return null;
                }
                // JSONObject projectInfoJson = new JSONObject(userAuthResponse.body());
                // System.out.println(projectInfoJson);
                pipelineTests.clear();

                HttpResponse<String> pipelineInfo = APIRequest("/projects/61935538/pipelines");
                JSONArray pipelineInfoJson = new JSONArray(pipelineInfo.body());
                // System.out.println(pipelineInfoJson);

                for (int i = 0; i < pipelineInfoJson.length(); i++) {
                    pipelineTests.add((i + 1) + " " + getJsonStringValue(pipelineInfoJson, i, "ref") + " "
                            + formatISOTime(getJsonStringValue(pipelineInfoJson, i, "updated_at")) + " "
                            + getJsonStringValue(pipelineInfoJson, i, "status"));
                    pipelineIds.add(pipelineInfoJson.getJSONObject(i).getInt("id"));
                    // if (pipelineInfoJson.getJSONObject(i).getString("ref").equals("main")) {
                    // }
                }
                // Update the GUI on the JavaFX Application thread
                Platform.runLater(() -> {
                    welcomeMessage.setText(
                            "Welcome " + username + ", please select a pipeline\nfrom the list below to view detailed results.");
                    pipelineList.setItems(pipelineTests);
                    loginProgressIndicator.setVisible(false);
                    mainPage.setVisible(true);
                    loginPage.setVisible(false);
                    mainWindowGUI.mainWindowFront();
                });
                return null;
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                exception.printStackTrace();
                Platform.runLater(() -> {
                    welcomeMessage.setText("Login was unsuccessful");
                    pipelineList.setVisible(false);
                });
            }
        };

        new Thread(task).start();
    }

    private String getJsonStringValue(JSONArray jsonArray, int indx, String key) {
        return jsonArray.getJSONObject(indx).getString(key);
    }

    private boolean isOnline() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("google.com", 80), 2000);
            return true;
        } catch (Exception e) {
            mainWindowGUI.error("Offline",
                    "You appear to be offline, please check your connection and try logging in again");
            return false;
        }
    }

    private String formatISOTime(String unformatedTime) {

        ZonedDateTime zonedDateTime = ZonedDateTime.parse(unformatedTime);

        // Format the ZonedDateTime to include date and AM/PM time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");
        return zonedDateTime.format(formatter);
    }

    /**
     * After user selects a pipeline in the pipeline list this method then
     * sets up and displays statuses of the the individual pipeline jobs.
     * 
     * @param event
     */
    @FXML
    public void pipelineChosen(ActionEvent event) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                
                int selectedPipeline = pipelineIds.get(pipelineList.getSelectionModel().getSelectedIndex());
                testResultsLabel.setText("Pipeline " + selectedPipeline + " Results");

                HttpResponse<String> pipelineStages = APIRequest("/projects/61935538/pipelines/" + selectedPipeline + "/jobs");
                JSONArray selectedPlineJobsJson = new JSONArray(pipelineStages.body());
                System.out.println(selectedPlineJobsJson);

                Map<String, JSONArray> stageJobsMap = new HashMap<>();
                
                for (int i = 0; i < selectedPlineJobsJson.length(); i++) {
                    JSONObject job = selectedPlineJobsJson.getJSONObject(i);
                    String stage = job.getString("stage");

                    stageJobsMap.computeIfAbsent(stage, k -> new JSONArray()).put(job);
                }

                

                JSONArray buildJobs = stageJobsMap.get("build");
                JSONArray testJobs = stageJobsMap.get("test");
                JSONArray packageJobs = stageJobsMap.get("package");
                JSONArray deployJobs = stageJobsMap.get("deploy");

                setImageandLabel(buildJobs, buildIcon, buildStatusLabel);
                setImageandLabel(testJobs, testIcon, testStatusLabel);
                setImageandLabel(packageJobs, packageIcon, packageStatusLabel);
                setImageandLabel(deployJobs, deployIcon, deployStatusLabel);

                Platform.runLater(() -> {
                    pipelineTabs.getSelectionModel().select(1);
                });
                return null;
            }
            @Override
            protected void failed() {
                Throwable exception = getException();
                exception.printStackTrace();
                Platform.runLater(() -> {
                    
                });
            }
        };
        new Thread(task).start();
    }

    /**
     * check to see if any build jobs failed and if so 
     * change the corrisponding image to the failed icon and label to status
     * @param jobs
     * @param imageView
     * @param label
     */
    private void setImageandLabel(JSONArray jobs, ImageView imageView, Text label){
        boolean failedRequired = false;
        boolean failedAllowed = false;
        boolean allJobsOptional = true;
        boolean skipped = false;
        for (int i = 0; jobs != null && i < jobs.length(); i++) {
            JSONObject job = jobs.getJSONObject(i);

            String status = job.getString("status");
            boolean allowFailure = job.getBoolean("allow_failure");
            if (!allowFailure) {
                allJobsOptional = false;
                if (status.equals("failed")) {
                    failedRequired = true;
                    break;
                }
                else if (status.equals("skipped")) {
                    skipped = true;
                }
            }
            else if (status.equals("failed")){
                failedAllowed = true;
            }
        }

        if (failedRequired || jobs == null) {
            imageView.setImage(new Image(getClass().getResourceAsStream("/icons/error.png")));
            label.setText("Failed");
        }
        else if (failedAllowed){
            imageView.setImage(new Image(getClass().getResourceAsStream("/icons/warning.png")));
            label.setText("Warning");
        }
        else if (allJobsOptional || allJobsSuccessful(jobs)){
            imageView.setImage(new Image(getClass().getResourceAsStream("/icons/passed.png")));
            label.setText("Passed");
        }
        else if (skipped == true){
            imageView.setImage(new Image(getClass().getResourceAsStream("/icons/skipped.png")));
            label.setText("Skipped");
        }
        else{
            imageView.setImage(new Image(getClass().getResourceAsStream("/icons/pending.png")));
            label.setText("Pending");
        }


    }

    private boolean allJobsSuccessful(JSONArray jobs) {
        for (int i = 0; i < jobs.length(); i++) {
            String jobStatus = jobs.getJSONObject(i).getString("status");
            if (!jobStatus.equals("success")) {
                return false; // Found a non-successful job
            }
        }
        return true;
    }
}
