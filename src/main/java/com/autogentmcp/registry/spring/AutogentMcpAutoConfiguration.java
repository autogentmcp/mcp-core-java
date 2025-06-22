package com.autogentmcp.registry.spring;

import com.autogentmcp.registry.AutogentTool;
import com.autogentmcp.registry.EnableAutogentMcp;
import com.autogentmcp.registry.RegistryClient;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class AutogentMcpAutoConfiguration implements ApplicationContextAware, BeanPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(AutogentMcpAutoConfiguration.class);
    private ApplicationContext applicationContext;
    private boolean appRegistered = false;
    private String appKey = null;
    private RegistryClient registryClient;

    @Value("${autogentmcp.base-domain}")
    private String baseDomain;

    @Value("${autogentmcp.app-healthcheck-endpoint}")
    private String appHealthcheckEndpoint;

    @Value("${autogentmcp.security}")
    private String securityJson;

    @Value("${autogentmcp.registry-url}")
    private String registryUrl;

    @Value("${autogentmcp.api-key}")
    private String apiKey;

    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (appRegistered && appKey != null) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                AutogentTool toolAnn = AnnotationUtils.findAnnotation(method, AutogentTool.class);
                if (toolAnn != null) {
                    Map<String, Object> endpointData = new HashMap<>();
                    endpointData.put("app_key", appKey);
                    endpointData.put("endpoint_uri", toolAnn.endpointUri());
                    endpointData.put("endpoint_description", toolAnn.endpointDescription());
                    try {
                        if (!toolAnn.parameterDetails().isEmpty()) {
                            endpointData.put("parameter_details", new com.fasterxml.jackson.databind.ObjectMapper()
                                    .readValue(toolAnn.parameterDetails(), Map.class));
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse parameterDetails JSON in @AutogentTool for method {}",
                                method.getName(), e);
                        continue; // Skip this endpoint, continue with others
                    }
                    try {
                        log.info("Registering endpoint with MCP: {}", endpointData);
                        String response = registryClient.registerEndpoint(endpointData);
                        log.info("MCP registerEndpoint response: {}", response);
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("401")) {
                            log.error(
                                    "Unauthorized (401) when registering endpoint with MCP for method {}. Check your API key.",
                                    method.getName(), e);
                        } else {
                            log.error("Failed to register endpoint with MCP for method {}", method.getName(), e);
                        }
                        // Do not throw, continue with other endpoints
                    }
                }
            }
        }
        return bean;
    }

    @PostConstruct
    public boolean registerApp() {
        Object mainBean = findMainAppBean();
        if (mainBean != null) {
            EnableAutogentMcp ann = AnnotationUtils.findAnnotation(mainBean.getClass(), EnableAutogentMcp.class);
            if (ann != null) {
                appKey = ann.appKey();
                registryClient = new RegistryClient(registryUrl, apiKey);
                Map<String, Object> appData = new HashMap<>();
                appData.put("app_key", ann.appKey());
                appData.put("app_description", ann.appDescription());
                appData.put("base_domain", baseDomain);
                appData.put("app_healthcheck_endpoint", appHealthcheckEndpoint);
                try {
                    if (securityJson != null && !securityJson.isEmpty()) {
                        appData.put("security",
                                new com.fasterxml.jackson.databind.ObjectMapper().readValue(securityJson, Map.class));
                    }
                    log.info("Registering application with MCP: {}", appData);
                    String response = registryClient.registerApplication(appData);
                    log.info("MCP registerApplication response: {}", response);
                    appRegistered = true;
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("401")) {
                        log.error("Unauthorized (401) when registering application with MCP. Check your API key.", e);
                    } else {
                        log.error("Failed to register application with MCP", e);
                    }
                    // Do not throw, allow application to continue
                }
            }
        }
        return false;
    }


    private Object findMainAppBean() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String name : beanNames) {
            Object bean = applicationContext.getBean(name);
            if (AnnotationUtils.findAnnotation(bean.getClass(), EnableAutogentMcp.class) != null) {
                return bean;
            }
        }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
