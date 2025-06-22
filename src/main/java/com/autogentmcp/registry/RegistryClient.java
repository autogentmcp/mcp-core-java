package com.autogentmcp.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class RegistryClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public RegistryClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String registerApplication(Map<String, Object> appData) throws Exception {
        String body = objectMapper.writeValueAsString(appData);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/register/application"))
                .header("Content-Type", "application/json")
                .header("X-API-KEY", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public String registerEndpoint(Map<String, Object> endpointData) throws Exception {
        String body = objectMapper.writeValueAsString(endpointData);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/register/endpoint"))
                .header("Content-Type", "application/json")
                .header("X-API-KEY", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public String sendHeartbeat(Map<String, Object> heartbeatData) throws Exception {
        String body = objectMapper.writeValueAsString(heartbeatData);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/heartbeat"))
                .header("Content-Type", "application/json")
                .header("X-API-KEY", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
   
}
