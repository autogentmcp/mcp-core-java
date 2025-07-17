package com.autogentmcp.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;

public class RegistryClient {
    private static final Logger log = LoggerFactory.getLogger(RegistryClient.class);
    protected final String baseUrl;
    protected final CloseableHttpClient httpClient;
    protected final ObjectMapper objectMapper;
    protected final String apiKey;

    public RegistryClient(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, HttpClients.createDefault());
    }

    public RegistryClient(String baseUrl, String apiKey, CloseableHttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    protected CloseableHttpResponse executePost(HttpPost post) throws IOException {
        log.debug("Executing POST request to URL: {}", post.getURI());
        return httpClient.execute(post);
    }

    protected CloseableHttpResponse executePut(HttpPut put) throws IOException {
        log.debug("Executing PUT request to URL: {}", put.getURI());
        return httpClient.execute(put);
    }

    public String registerEndpointsBatch(String appKey, String environment, java.util.List<Map<String, Object>> endpoints) throws IOException {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("app_key", appKey);
        payload.put("environment", environment);
        payload.put("endpoints", endpoints);
        String body = objectMapper.writeValueAsString(payload);
        HttpPost post = new HttpPost(baseUrl + "/register/endpoints");
        post.setHeader("Content-Type", "application/json");
        post.setHeader("X-API-Key", apiKey);
        post.setHeader("X-App-Key", appKey);
        post.setEntity(new StringEntity(body, "UTF-8"));
        try (CloseableHttpResponse response = executePost(post)) {
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        }
    }

    public String updateApplication(String appKey, Map<String, Object> updateData) throws IOException {
        String body = objectMapper.writeValueAsString(updateData);
        HttpPut put = new HttpPut(baseUrl + "/applications/" + appKey);
        put.setHeader("Content-Type", "application/json");
        put.setHeader("X-API-Key", apiKey);
        put.setHeader("X-App-Key", appKey);
        put.setEntity(new StringEntity(body, "UTF-8"));
        try (CloseableHttpResponse response = executePut(put)) {
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        }
    }
}
