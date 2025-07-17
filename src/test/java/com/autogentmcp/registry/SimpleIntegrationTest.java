package com.autogentmcp.registry;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Simple integration test that doesn't use Spring Boot Test
 * This provides a basic verification that the core functionality works without relying on complex mocks
 */
public class SimpleIntegrationTest {
    
    @Test
    public void testRegistryClientBasic() {
        // Create a simple RegistryClient with mocked implementation for testing
        RegistryClient client = new RegistryClient("http://localhost:8000", "test-key") {
            @Override
            protected org.apache.http.client.methods.CloseableHttpResponse executePost(
                    org.apache.http.client.methods.HttpPost post) throws IOException {
                return new RegistryClientTest.MockHttpResponse(
                        new RegistryClientTest.MockHttpEntity("Success"));
            }
            
            @Override
            protected org.apache.http.client.methods.CloseableHttpResponse executePut(
                    org.apache.http.client.methods.HttpPut put) throws IOException {
                return new RegistryClientTest.MockHttpResponse(
                        new RegistryClientTest.MockHttpEntity("Success"));
            }
        };
        
        try {
            // Test basic endpoint registration
            List<Map<String, Object>> endpoints = new ArrayList<>();
            Map<String, Object> endpoint = new HashMap<>();
            endpoint.put("name", "TestEndpoint");
            endpoint.put("path", "/test");
            endpoint.put("method", "GET");
            endpoint.put("description", "Test endpoint");
            endpoints.add(endpoint);
            
            String result = client.registerEndpointsBatch("test-app", "dev", endpoints);
            assertNotNull(result);
            
            // Test application update
            Map<String, Object> appData = new HashMap<>();
            appData.put("name", "TestApp");
            appData.put("description", "Test application");
            appData.put("healthCheckUrl", "/health");
            
            result = client.updateApplication("test-app", appData);
            assertNotNull(result);
        } catch (Exception e) {
            // If an exception occurs, fail the test
            throw new RuntimeException("Integration test failed", e);
        }
    }
}
