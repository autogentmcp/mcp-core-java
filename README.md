# MCP Core Java SDK

A high-performance Java SDK for MCP (Model Context Protocol) registry server integration, designed for agent/tool management, heartbeat monitoring, and easy Spring Boot integration.

## Features
- **Spring Boot auto-registration** of applications and endpoints
- **Annotation-driven** endpoint registration with `@AutogentTool`
- **Automatic deduction** of HTTP method, path/query/body parameters from method signatures and Spring annotations
- **LLM-friendly metadata** for tool discovery and prompt engineering
- **Secure configuration** via `application.properties`
- **Robust error handling** and logging
- **Extensible** for advanced metadata, examples, and security

## Quick Start

### 1. Add Dependency
Add to your Maven `pom.xml`:
```xml
<dependency>
  <groupId>com.autogentmcp</groupId>
  <artifactId>mcp-core-java</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Properties
Add to your `src/main/resources/application.properties`:
```
autogentmcp.base-domain=http://localhost:8080
autogentmcp.app-healthcheck-endpoint=/health
autogentmcp.registry-url=https://your-mcp-registry/api/registry
autogentmcp.api-key=YOUR_API_KEY
autogentmcp.security={"type":"apiKey"}
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
        uri = "/math/add/{a}/{b}",
        description = "Adds two numbers and returns the sum."
        // method, pathParams, queryParams, requestBody are deduced automatically
    )
    @GetMapping("/math/add/{a}/{b}")
    public int add(@PathVariable int a, @PathVariable int b) {
        return a + b;
    }
}
```

## How It Works
- On startup, the SDK auto-registers your application and all `@AutogentTool` endpoints with the MCP registry.
- Parameter schemas and HTTP methods are deduced from Spring annotations, but can be overridden in the annotation.
- All configuration is managed via `application.properties`.

## Advanced Usage
- Override deduced values in `@AutogentTool` for custom schemas or documentation.
- Add security, tags, or examples as needed for richer metadata.

## Contributing
Pull requests and issues are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License
MIT