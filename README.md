# MCP Core Java SDK

A high-performance Java SDK for MCP (Model Context Protocol) registry server integration, designed for agent/tool management, heartbeat monitoring, and easy Spring Boot integration.

## Features
- **Java 8+ Compatible** - Works with Java 8 through Java 21
- **Spring Boot Integration** - Compatible with Spring Boot 2.x and 3.x
- **Spring Boot auto-registration** of applications and endpoints
- **Batch endpoint registration** for modern MCP registry servers
- **Annotation-driven** endpoint registration with `@AutogentTool`
- **Automatic deduction** of HTTP method, path/query/body parameters from method signatures and Spring annotations
- **LLM-friendly metadata** for tool discovery and prompt engineering
- **Robust error handling** and logging
- **Extensible** for advanced metadata, examples, and security

## Quick Start

### 1. Add Dependency
Add to your Maven `pom.xml`:
```xml
<dependency>
  <groupId>com.autogentmcp</groupId>
  <artifactId>mcp-core-java</artifactId>
  <version>0.0.3</version>
</dependency>
```

### 2. Configure Properties
Add to your `src/main/resources/application.properties` (or YAML):
```properties
# MCP Registry URL (required)
autogentmcp.registry-url=https://your-mcp-registry/api/registry

# MCP API Key (required)
autogentmcp.api-key=YOUR_API_KEY

# Application health check endpoint (required) 
# This is the endpoint that MCP will use to check if your app is alive
autogentmcp.app-healthcheck-endpoint=/actuator/health

# Environment (optional, default: production)
autogentmcp.environment=production
```

### 3. Annotate Your Application
```java
@EnableAutogentMcp(key = "demo-app", description = "Demo Spring Boot Application")
@SpringBootApplication
public class DemoApplication { ... }
```

### 4. Register Endpoints with @AutogentTool
```java
@RestController
public class MathController {
    @AutogentTool(
        name = "Add Numbers",
        uri = "/math/add/{a}/{b}",
        description = "Adds two numbers and returns the sum.",
        method = "GET",  // Optional, deduced from @GetMapping if not specified
        isPublic = true  // Whether this tool should be publicly accessible
        // pathParams, queryParams, requestBody are deduced automatically from method signature
    )
    @GetMapping("/math/add/{a}/{b}")
    public int add(@PathVariable int a, @PathVariable int b) {
        return a + b;
    }
}
```

## How It Works
- On startup, the SDK auto-registers your application (PUT `/applications/{app_key}`) and all endpoints in a single batch (POST `/register/endpoints`) with the MCP registry.
- The `@AutogentTool` annotation provides metadata that helps MCP understand your endpoints and make them available to AI agents.
- The SDK automatically deduces HTTP method, path parameters, query parameters, and request body structure from Spring annotations and method signatures.

## Advanced Usage

### Direct Registry Client Usage

For non-Spring applications or advanced use cases, you can use the `RegistryClient` directly:

```java
RegistryClient client = new RegistryClient("https://your-registry-url", "your-api-key");

// Register an application
Map<String, Object> appData = new HashMap<>();
appData.put("name", "My App");
appData.put("description", "Description of my app");
appData.put("healthCheckUrl", "/health");
client.updateApplication("my-app-key", appData);

// Register endpoints in batch
List<Map<String, Object>> endpoints = new ArrayList<>();
Map<String, Object> endpoint = new HashMap<>();
endpoint.put("name", "My Endpoint");
endpoint.put("path", "/api/data");
endpoint.put("method", "GET");
endpoint.put("description", "Gets some data");
endpoints.add(endpoint);
client.registerEndpointsBatch("my-app-key", "production", endpoints);
```

### Compatibility Notes

- **Java Compatibility**: Works with Java 8+
- **Spring Boot Compatibility**: Compatible with Spring Boot 2.x and 3.x
- **Dependencies**:
  - Spring Boot (optional, for auto-configuration)
  - Jackson for JSON processing
  - Apache HttpClient for HTTP communication

## Example Application

See the `ExampleMcpApp.java` in the examples package for a complete Spring Boot application that demonstrates how to use the MCP SDK.

## Contributing
Pull requests and issues are welcome!

## License
Apache License 2.0