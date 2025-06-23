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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

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
                    endpointData.put("uri", toolAnn.uri());
                    endpointData.put("description", toolAnn.description());

                    // Deduce HTTP method if not set in annotation
                    String httpMethod = toolAnn.method();
                    boolean methodOverridden = toolAnn.method() != null && !toolAnn.method().equals("POST");
                    if (!methodOverridden || httpMethod.isEmpty() || httpMethod.equals("POST")) {
                        if (method.isAnnotationPresent(GetMapping.class)) {
                            httpMethod = "GET";
                        } else if (method.isAnnotationPresent(PostMapping.class)) {
                            httpMethod = "POST";
                        } else if (method.isAnnotationPresent(PutMapping.class)) {
                            httpMethod = "PUT";
                        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                            httpMethod = "DELETE";
                        } else if (method.isAnnotationPresent(PatchMapping.class)) {
                            httpMethod = "PATCH";
                        } else if (method.isAnnotationPresent(RequestMapping.class)) {
                            RequestMapping reqMapping = method.getAnnotation(RequestMapping.class);
                            RequestMethod[] reqMethods = reqMapping.method();
                            if (reqMethods.length > 0) {
                                httpMethod = reqMethods[0].name();
                            }
                        }
                    }
                    endpointData.put("method", httpMethod);

                    // Deduce Content-Type (prioritize mapping annotation consumes)
                    String contentType = toolAnn.contentType();
                    String mappingConsumes = null;
                    if (method.isAnnotationPresent(PostMapping.class)) {
                        PostMapping ann = method.getAnnotation(PostMapping.class);
                        if (ann.consumes().length > 0) mappingConsumes = ann.consumes()[0];
                    } else if (method.isAnnotationPresent(PutMapping.class)) {
                        PutMapping ann = method.getAnnotation(PutMapping.class);
                        if (ann.consumes().length > 0) mappingConsumes = ann.consumes()[0];
                    } else if (method.isAnnotationPresent(PatchMapping.class)) {
                        PatchMapping ann = method.getAnnotation(PatchMapping.class);
                        if (ann.consumes().length > 0) mappingConsumes = ann.consumes()[0];
                    } else if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping ann = method.getAnnotation(RequestMapping.class);
                        if (ann.consumes().length > 0) mappingConsumes = ann.consumes()[0];
                    }
                    if (mappingConsumes != null && !mappingConsumes.isEmpty()) {
                        contentType = mappingConsumes;
                    }
                    endpointData.put("contentType", contentType);

                    // Deduce pathParams, queryParams, requestBody if not set in annotation
                    String pathParams = toolAnn.pathParams();
                    String queryParams = toolAnn.queryParams();
                    String requestBody = toolAnn.requestBody();
                    if (pathParams.isEmpty() || queryParams.isEmpty() || requestBody.isEmpty()) {
                        Map<String, String> deducedPathParams = new HashMap<>();
                        Map<String, String> deducedQueryParams = new HashMap<>();
                        Map<String, String> deducedRequestBody = new HashMap<>();
                        java.lang.reflect.Parameter[] params = method.getParameters();
                        for (java.lang.reflect.Parameter param : params) {
                            if (param.isAnnotationPresent(PathVariable.class)) {
                                deducedPathParams.put(param.getName(), param.getType().getSimpleName());
                            } else if (param.isAnnotationPresent(RequestParam.class)) {
                                deducedQueryParams.put(param.getName(), param.getType().getSimpleName());
                            } else if (param.isAnnotationPresent(RequestBody.class)) {
                                // For request body, if it's a Map, just note as 'object', else use class fields
                                Class<?> type = param.getType();
                                if (Map.class.isAssignableFrom(type)) {
                                    deducedRequestBody.put(param.getName(), "object");
                                } else {
                                    for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                                        deducedRequestBody.put(field.getName(), field.getType().getSimpleName());
                                    }
                                }
                            }
                        }
                        // FIX: Put the actual Map, not a JSON string, into endpointData
                        if (pathParams.isEmpty() && !deducedPathParams.isEmpty()) {
                            endpointData.put("pathParams", deducedPathParams);
                        } else if (!pathParams.isEmpty()) {
                            endpointData.put("pathParams", pathParams);
                        }
                        if (queryParams.isEmpty() && !deducedQueryParams.isEmpty()) {
                            endpointData.put("queryParams", deducedQueryParams);
                        } else if (!queryParams.isEmpty()) {
                            endpointData.put("queryParams", queryParams);
                        }
                        if (requestBody.isEmpty() && !deducedRequestBody.isEmpty()) {
                            endpointData.put("requestBody", deducedRequestBody);
                        } else if (!requestBody.isEmpty()) {
                            endpointData.put("requestBody", requestBody);
                        }
                    } else {
                        if (!pathParams.isEmpty()) {
                            endpointData.put("pathParams", pathParams);
                        }
                        if (!queryParams.isEmpty()) {
                            endpointData.put("queryParams", queryParams);
                        }
                        if (!requestBody.isEmpty()) {
                            endpointData.put("requestBody", requestBody);
                        }
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
                appKey = ann.key();
                registryClient = new RegistryClient(registryUrl, apiKey);
                Map<String, Object> appData = new HashMap<>();
                appData.put("app_key", ann.key());
                appData.put("app_description", ann.description());
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
