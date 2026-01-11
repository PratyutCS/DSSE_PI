package com.example.pi.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NetworkUtils {

    // Generic Request helper
    public static String performRequest(String requestUrl, String method, String jsonBody, String authToken) throws Exception {
        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (authToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }

        if (jsonBody != null && (method.equals("POST") || method.equals("PUT") || method.equals("DELETE"))) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        return readResponse(conn);
    }

    // Helper method to perform POST request with JSON body
    public static String performPostRequest(String requestUrl, String jsonBody, String authToken) throws Exception {
        return performRequest(requestUrl, "POST", jsonBody, authToken);
    }
    
    // Helper method to perform GET request
    public static String performGetRequest(String requestUrl, String authToken) throws Exception {
        return performRequest(requestUrl, "GET", null, authToken);
    }

    // Helper method to perform DELETE request
    public static String performDeleteRequest(String requestUrl, String jsonBody, String authToken) throws Exception {
        return performRequest(requestUrl, "DELETE", jsonBody, authToken);
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        int responseCode = conn.getResponseCode();

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        if (responseCode >= 200 && responseCode < 300) {
            return response.toString();
        } else {
            throw new Exception("Error: " + responseCode + " " + response.toString());
        }
    }
}
