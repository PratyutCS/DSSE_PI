package com.example.pi.network;

import java.io.BufferedReader;
import java.io.File;
import java.util.List;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NetworkUtils {
    
    public interface ProgressListener {
        void onProgress(int progress);
    }

    // Generic Request helper
    public static String performRequest(String requestUrl, String method, String jsonBody, String authToken) throws Exception {
        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(120000);

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

    // Helper method to perform multipart POST request for file uploads
    public static String performMultipartRequest(String requestUrl, String dbName, List<String> filePaths, String authToken) throws Exception {
        String boundary = "Boundary-" + System.currentTimeMillis();
        URL url = new URL(requestUrl + "?dbName=" + dbName);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (authToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }

        try (OutputStream outputStream = conn.getOutputStream();
             java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {
            
            for (String filePath : filePaths) {
                File file = new File(filePath);
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"files\"; filename=\"").append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: text/plain\r\n\r\n");
                writer.flush();

                try (java.io.FileInputStream inputStream = new java.io.FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }
                writer.append("\r\n");
                writer.flush();
            }
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
        }

        return readResponse(conn);
    }
    public static String downloadFile(String requestUrl, String destinationDir, String authToken, ProgressListener listener) throws Exception {
        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (authToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        conn.connect();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP error code: " + conn.getResponseCode());
        }

        // Try to get filename from Content-Disposition header
        String fileName = null;
        String disposition = conn.getHeaderField("Content-Disposition");
        if (disposition != null) {
            String[] params = disposition.split(";");
            for (String param : params) {
                param = param.trim();
                if (param.startsWith("filename=")) {
                    fileName = param.substring(9).replace("\"", "");
                    break;
                } else if (param.startsWith("filename*=")) {
                    // Handle RFC 5987 encoded filenames if needed
                    String[] parts = param.split("''");
                    if (parts.length == 2) fileName = parts[1];
                }
            }
        }

        // Fallback to URL's name or fileId if header fails
        if (fileName == null) {
            android.net.Uri uri = android.net.Uri.parse(requestUrl);
            fileName = uri.getQueryParameter("fileId");
            if (fileName == null) {
                fileName = "downloaded_file_" + System.currentTimeMillis();
            }
        }

        int fileLength = conn.getContentLength();
        
        File destinationFile = new File(destinationDir, fileName);
        try (java.io.InputStream is = conn.getInputStream();
             java.io.FileOutputStream os = new java.io.FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (listener != null && fileLength > 0) {
                    listener.onProgress((int) (totalRead * 100 / fileLength));
                }
            }
        }
        return fileName;
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
