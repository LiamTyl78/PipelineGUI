package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import org.json.JSONObject;

import com.example.controllers.mainWindowController;

import javax.net.ssl.HttpsURLConnection;

import fi.iki.elonen.NanoHTTPD;

public class OAuthRedirectServer extends NanoHTTPD {
    private HttpsURLConnection con;

    // private
    public OAuthRedirectServer() {
        super(8080);
    }

    public Response serve(IHTTPSession session) {
        if (session.getUri().equals("/callback")) {
            String code = session.getParameters().get("code").get(0);
            exchangeCodeForToken(code);
            return newFixedLengthResponse(
                    "OAuth flow complete. You can close this window and return to the application.");
        } else {
            return newFixedLengthResponse(("404 Not Found"));
        }
    }

    private void exchangeCodeForToken(String authorizationCode) {
        String url = "https://gitlab.com/oauth/token";
        String clientId = "fd6b326760c2de5aeee3e08f85f0e2197821a8000663b76f055463722b722e10";
        String clientSecret = Vars.clientSecret;
        String redirectUri = "http://localhost:8080/callback";

        String params = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&code=" + authorizationCode +
                "&redirect_uri=" + redirectUri +
                "&grant_type=authorization_code";

        // System.out.println(params);

        try {
            URL obj = new URL(url);
            con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        try (OutputStream os = con.getOutputStream()) {

            byte[] input = params.getBytes("utf-8");
            os.write(input, 0, input.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("Response: " + response.toString());

            JSONObject json = new JSONObject(response.toString());
            mainWindowController.setAccessToken(json.getString("access_token"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
