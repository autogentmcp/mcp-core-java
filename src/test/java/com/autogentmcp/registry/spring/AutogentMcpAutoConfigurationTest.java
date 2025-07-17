package com.autogentmcp.registry.spring;

import com.autogentmcp.registry.AutogentTool;
import com.autogentmcp.registry.EnableAutogentMcp;
import com.autogentmcp.registry.RegistryClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {AutogentMcpAutoConfigurationTest.TestConfig.class})
@TestPropertySource(properties = {
        "autogentmcp.app-healthcheck-endpoint=/health",
        "autogentmcp.registry-url=http://localhost:8000",
        "autogentmcp.api-key=test-key",
        "autogentmcp.environment=test"
})
public class AutogentMcpAutoConfigurationTest {
    @Configuration
    static class TestConfig {
        @Bean
        public RegistryClient registryClient() {
            // Create a real instance instead of mocking it since Mockito has issues with Java 21
            return new RegistryClient("http://localhost:8000", "test-key") {
                @Override
                public String registerEndpointsBatch(String appKey, String environment, List<Map<String, Object>> endpoints) throws IOException {
                    return "batch-ok";
                }
                @Override
                public String updateApplication(String appKey, Map<String, Object> updateData) throws IOException {
                    return "update-ok";
                }
            };
        }
        @Bean
        public TestController testController() { return new TestController(); }
    }

    static class TestController {
        @AutogentTool(name = "TestEndpoint", uri = "/test", description = "desc", method = "GET", isPublic = true)
        public void testMethod() {}
    }

    @Value("${autogentmcp.app-healthcheck-endpoint}")
    String healthEndpoint;
    @Value("${autogentmcp.registry-url}")
    String registryUrl;
    @Value("${autogentmcp.api-key}")
    String apiKey;
    @Value("${autogentmcp.environment}")
    String environment;

    @Test
    public void testEndpointCollectionAndRegistration() throws Exception {
        AutogentMcpAutoConfiguration config = new AutogentMcpAutoConfiguration();
        
        // Create a mock ApplicationContext that safely returns empty arrays for getBeanDefinitionNames
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        Mockito.when(mockContext.getBeanDefinitionNames()).thenReturn(new String[]{"testController"});
        
        config.setApplicationContext(mockContext);
        config.appRegistered = true;
        config.appKey = "app-key";
        
        // Set environment field via reflection as it's private
        java.lang.reflect.Field envField = AutogentMcpAutoConfiguration.class.getDeclaredField("environment");
        envField.setAccessible(true);
        envField.set(config, environment);
        
        // Use our custom RegistryClient implementation for testing
        config.registryClient = new RegistryClient(registryUrl, apiKey) {
            @Override
            public String registerEndpointsBatch(String appKey, String env, List<Map<String, Object>> endpoints) throws IOException {
                assertEquals("app-key", appKey);
                assertEquals(environment, env);
                assertEquals(1, endpoints.size());
                Map<String, Object> ep = endpoints.get(0);
                assertEquals("TestEndpoint", ep.get("name"));
                assertEquals("/test", ep.get("path"));
                assertEquals("GET", ep.get("method"));
                assertEquals("desc", ep.get("description"));
                assertEquals(true, ep.get("isPublic"));
                return "batch-ok";
            }
        };
        
        TestController bean = new TestController();
        config.postProcessAfterInitialization(bean, "testController");
    }

    @Test
    public void testAppUpdatePayload() throws Exception {
        AutogentMcpAutoConfiguration config = new AutogentMcpAutoConfiguration();
        
        // Mock the ApplicationContext to handle getBean calls
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        Mockito.when(mockContext.getBeanDefinitionNames()).thenReturn(new String[]{"testController"});
        
        // Mock a bean with the EnableAutogentMcp annotation
        class TestAppBean {
            // This class will be used to get the annotation
        }
        
        EnableAutogentMcp ann = Mockito.mock(EnableAutogentMcp.class);
        Mockito.when(ann.key()).thenReturn("app-key");
        Mockito.when(ann.description()).thenReturn("desc");
        
        TestAppBean testBean = new TestAppBean();
        Mockito.when(mockContext.getBean("testApp")).thenReturn(testBean);
        Mockito.when(mockContext.getBeanDefinitionNames()).thenReturn(new String[]{"testApp"});
        
        // Setup ApplicationContext to find our mock bean with annotation
        config.setApplicationContext(mockContext);
        
        // Set required fields
        config.appKey = "app-key";
        
        // Set environment field via reflection as it's private
        java.lang.reflect.Field envField = AutogentMcpAutoConfiguration.class.getDeclaredField("environment");
        envField.setAccessible(true);
        envField.set(config, environment);
        
        // Set healthCheckEndpoint field via reflection
        java.lang.reflect.Field healthcheckField = AutogentMcpAutoConfiguration.class.getDeclaredField("appHealthcheckEndpoint");
        healthcheckField.setAccessible(true);
        healthcheckField.set(config, healthEndpoint);
        
        // Create our test registry client
        config.registryClient = new RegistryClient(registryUrl, apiKey) {
            @Override
            public String updateApplication(String appKey, Map<String, Object> updateData) throws IOException {
                assertEquals("app-key", appKey);
                assertEquals("app-key", updateData.get("name"));
                assertEquals("desc", updateData.get("description"));
                assertEquals(healthEndpoint, updateData.get("healthCheckUrl"));
                return "update-ok";
            }
        };
        
        // Test app registration without relying on Spring context
        config.registerApp();
    }
}
