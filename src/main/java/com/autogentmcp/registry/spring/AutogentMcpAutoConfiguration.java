package com.autogentmcp.registry.spring;

import com.autogentmcp.registry.AutogentTool;
import com.autogentmcp.registry.EnableAutogentMcp;
import com.autogentmcp.registry.RegistryClient;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.SmartInitializingSingleton;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class AutogentMcpAutoConfiguration implements ApplicationContextAware, BeanPostProcessor, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(AutogentMcpAutoConfiguration.class);
    private ApplicationContext applicationContext;
    // Make these fields package-private for test access
    boolean appRegistered = false;
    String appKey = null;
    RegistryClient registryClient;

    @Value("${autogentmcp.app-healthcheck-endpoint}")
    private String appHealthcheckEndpoint;

    @Value("${autogentmcp.registry-url}")
    private String registryUrl;

    @Value("${autogentmcp.api-key}")
    private String apiKey;

    @Value("${autogentmcp.environment:production}")
    private String environment;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (appRegistered && appKey != null) {
            // Use a static list to collect endpoints across beans
            String[] beanProcessors = applicationContext.getBeanNamesForType(BeanPostProcessor.class);
            if (beanProcessors != null && beanProcessors.length > 0 && beanProcessors[0].equals(beanName)) {
                // Only clear on first bean
                log.info("First bean encountered: {}, clearing endpoint collector", beanName);
                EndpointCollector.clear();
            }
            for (Method method : bean.getClass().getDeclaredMethods()) {
                AutogentTool toolAnn = AnnotationUtils.findAnnotation(method, AutogentTool.class);
                if (toolAnn != null) {
                    log.info("Found @AutogentTool annotation on method: {}.{}", bean.getClass().getSimpleName(), method.getName());
                    Map<String, Object> endpointData = new HashMap<>();
                    endpointData.put("name", toolAnn.name().isEmpty() ? method.getName() : toolAnn.name());
                    endpointData.put("path", toolAnn.uri());
                    endpointData.put("method", deduceHttpMethod(method, toolAnn));
                    endpointData.put("description", toolAnn.description());
                    endpointData.put("isPublic", toolAnn.isPublic());
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
                    // Add to collector
                    EndpointCollector.add(endpointData);
                    log.info("Added endpoint to collector: name={}, path={}, method={}", 
                             endpointData.get("name"), endpointData.get("path"), endpointData.get("method"));
                }
            }
            // On last bean, log that we're done processing but don't register yet
            if (isLastBean(beanName)) {
                log.info("Last bean processed: {}, collected {} endpoints so far", 
                         beanName, EndpointCollector.getAll().size());
                log.info("Will register endpoints when all singletons are instantiated");
            }
        }
        return bean;
    }

    @PostConstruct
    public boolean registerApp() {
        Object mainBean = findMainAppBean();
        log.info("Starting registerApp method, mainBean found: {}", (mainBean != null));
        
        if (mainBean != null) {
            EnableAutogentMcp ann = AnnotationUtils.findAnnotation(mainBean.getClass(), EnableAutogentMcp.class);
            log.info("Found @EnableAutogentMcp annotation: {}, class: {}", (ann != null), mainBean.getClass().getName());
            
            if (ann != null) {
                appKey = ann.key();
                log.info("AppKey from annotation: {}", appKey);
                log.info("Registry URL: {}, API Key length: {}", registryUrl, (apiKey != null ? apiKey.length() : 0));
                
                registryClient = new RegistryClient(registryUrl, apiKey);
                Map<String, Object> appData = new HashMap<>();
                appData.put("name", ann.key());
                appData.put("description", ann.description());
                appData.put("healthCheckUrl", appHealthcheckEndpoint);
                
                try {
                    log.info("Updating application with MCP: {}", appData);
                    String response = registryClient.updateApplication(appKey, appData);
                    log.info("MCP updateApplication response: {}", response);
                    appRegistered = true;
                    log.info("Application registered successfully, appRegistered set to true");
                    return true;
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("401")) {
                        log.error("Unauthorized (401) when updating application with MCP. Check your API key.", e);
                    } else {
                        log.error("Failed to update application with MCP", e);
                    }
                    // Do not throw, allow application to continue
                    log.info("Application registration failed, appRegistered remains false");
                }
            }
        } else {
            log.warn("No bean with @EnableAutogentMcp annotation found in application context");
        }
        return false;
    }


    private Object findMainAppBean() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        log.info("Searching for @EnableAutogentMcp annotation in {} beans", beanNames.length);
        
        // First, try looking for the main application class
        for (String name : beanNames) {
            try {
                Object bean = applicationContext.getBean(name);
                Class<?> beanClass = bean.getClass();
                log.debug("Checking bean: {} ({})", name, beanClass.getName());
                
                if (name.toLowerCase().contains("application")) {
                    log.info("Found potential main application bean: {}", name);
                    
                    EnableAutogentMcp ann = AnnotationUtils.findAnnotation(beanClass, EnableAutogentMcp.class);
                    if (ann != null) {
                        log.info("Found @EnableAutogentMcp on main application bean: {}", name);
                        return bean;
                    }
                }
            } catch (Exception e) {
                log.debug("Error checking bean {}: {}", name, e.getMessage());
            }
        }
        
        // If not found on main application class, check all beans
        for (String name : beanNames) {
            try {
                Object bean = applicationContext.getBean(name);
                EnableAutogentMcp ann = AnnotationUtils.findAnnotation(bean.getClass(), EnableAutogentMcp.class);
                if (ann != null) {
                    log.info("Found @EnableAutogentMcp on bean: {}", name);
                    return bean;
                }
            } catch (Exception e) {
                log.debug("Error checking bean {}: {}", name, e.getMessage());
            }
        }
        
        log.warn("No bean with @EnableAutogentMcp annotation found");
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        log.debug("Application context set: {}", applicationContext);
    }

    // This method gets called once all singletons are initialized
    @Override
    public void afterSingletonsInstantiated() {
        log.info("All singletons instantiated, ensuring endpoints are registered");
        registerEndpointsBatch();
    }
    
    // Method to register all collected endpoints in a batch
    private void registerEndpointsBatch() {
        if (!appRegistered || appKey == null) {
            log.warn("Cannot register endpoints because app is not registered or appKey is null");
            return;
        }
        
        try {
            int endpointCount = EndpointCollector.getAll().size();
            log.info("Final registration check: Found {} endpoints to register", endpointCount);
            
            if (endpointCount > 0) {
                log.info("Registering all endpoints in batch with MCP");
                String response = registryClient.registerEndpointsBatch(appKey, environment, EndpointCollector.getAll());
                log.info("MCP registerEndpointsBatch response: {}", response);
            } else {
                log.info("No endpoints found to register with MCP");
            }
        } catch (Exception e) {
            log.error("Failed to register endpoints batch with MCP", e);
        }
    }
    
    // Helper to check if this is the last bean to process
    private boolean isLastBean(String beanName) {
        String[] allBeans = applicationContext.getBeanDefinitionNames();
        if (allBeans == null || allBeans.length == 0) {
            log.warn("No beans found in application context");
            return true; // If no beans or null, consider it the last bean
        }

        // Get the last non-infrastructure bean name
        String lastBeanName = null;
        for (int i = allBeans.length - 1; i >= 0; i--) {
            String currentBean = allBeans[i];
            if (!currentBean.startsWith("org.springframework") && 
                !currentBean.startsWith("scopedTarget.") && 
                !currentBean.startsWith("java.") &&
                !currentBean.equals("autogentMcpAutoConfiguration")) {
                lastBeanName = currentBean;
                break;
            }
        }
        
        if (lastBeanName == null) {
            log.warn("Could not find a non-infrastructure bean");
            return true; // Default to true if we can't find a suitable bean
        }

        boolean isLast = beanName.equals(lastBeanName);
        log.debug("isLastBean check: current={}, last={}, result={}", beanName, lastBeanName, isLast);
        
        return isLast;
    }

    // Helper to deduce HTTP method
    private String deduceHttpMethod(Method method, AutogentTool toolAnn) {
        String httpMethod = toolAnn.method();
        boolean methodOverridden = toolAnn.method() != null && !toolAnn.method().equals("POST");
        if (!methodOverridden || httpMethod.isEmpty() || httpMethod.equals("POST")) {
            if (method.isAnnotationPresent(GetMapping.class)) return "GET";
            if (method.isAnnotationPresent(PostMapping.class)) return "POST";
            if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
            if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
            if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping reqMapping = method.getAnnotation(RequestMapping.class);
                RequestMethod[] reqMethods = reqMapping.method();
                if (reqMethods.length > 0) return reqMethods[0].name();
            }
        }
        return httpMethod;
    }

    // EndpointCollector: static inner class for collecting endpoints
    public static class EndpointCollector {
        private static final Logger collectorLog = LoggerFactory.getLogger(EndpointCollector.class);
        private static final java.util.List<Map<String, Object>> endpoints = Collections.synchronizedList(new java.util.ArrayList<>());
        
        static void add(Map<String, Object> endpoint) { 
            if (endpoint == null) {
                collectorLog.warn("Attempted to add null endpoint to collector");
                return;
            }
            
            synchronized (endpoints) {
                // Check if this endpoint already exists to avoid duplicates
                boolean isDuplicate = endpoints.stream()
                    .anyMatch(e -> e.get("name") != null && e.get("name").equals(endpoint.get("name")) &&
                              e.get("path") != null && e.get("path").equals(endpoint.get("path")) &&
                              e.get("method") != null && e.get("method").equals(endpoint.get("method")));
                
                if (isDuplicate) {
                    collectorLog.warn("Duplicate endpoint detected and ignored: {}", endpoint.get("name"));
                    return;
                }
                
                endpoints.add(endpoint);
                collectorLog.info("Added endpoint: name={}, path={}, method={}", 
                           endpoint.get("name"), endpoint.get("path"), endpoint.get("method"));
            }
        }
        
        static java.util.List<Map<String, Object>> getAll() { 
            synchronized (endpoints) {
                collectorLog.info("Getting all endpoints, current count: {}", endpoints.size());
                if (endpoints.isEmpty()) {
                    collectorLog.warn("No endpoints collected!");
                } else {
                    for (int i = 0; i < endpoints.size(); i++) {
                        Map<String, Object> endpoint = endpoints.get(i);
                        collectorLog.info("Endpoint [{}]: name={}, path={}, method={}", 
                                  i, endpoint.get("name"), endpoint.get("path"), endpoint.get("method"));
                    }
                }
                return new java.util.ArrayList<>(endpoints);
            }
        }
        
        static void clear() { 
            synchronized (endpoints) {
                collectorLog.info("Clearing endpoint collector, removing {} endpoints", endpoints.size());
                endpoints.clear();
            }
        }
    }
}
