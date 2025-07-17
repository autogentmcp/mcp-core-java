package com.autogentmcp.registry;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class AutogentToolTest {

    @AutogentTool(
        name = "TestTool",
        uri = "/test/uri",
        description = "Test description",
        pathParams = "{id}",
        queryParams = "{q}",
        requestBody = "{body}",
        method = "GET",
        isPublic = true,
        contentType = "application/json"
    )
    public void annotatedMethod() {}

    @Test
    public void testAnnotationPresence() throws Exception {
        Method method = this.getClass().getMethod("annotatedMethod");
        assertTrue(method.isAnnotationPresent(AutogentTool.class));
    }

    @Test
    public void testAnnotationValues() throws Exception {
        Method method = this.getClass().getMethod("annotatedMethod");
        AutogentTool tool = method.getAnnotation(AutogentTool.class);
        assertEquals("TestTool", tool.name());
        assertEquals("/test/uri", tool.uri());
        assertEquals("Test description", tool.description());
        assertEquals("{id}", tool.pathParams());
        assertEquals("{q}", tool.queryParams());
        assertEquals("{body}", tool.requestBody());
        assertEquals("GET", tool.method());
        assertTrue(tool.isPublic());
        assertEquals("application/json", tool.contentType());
    }

    @AutogentTool(uri = "/default")
    public void defaultMethod() {}

    @Test
    public void testDefaultValues() throws Exception {
        Method method = this.getClass().getMethod("defaultMethod");
        AutogentTool tool = method.getAnnotation(AutogentTool.class);
        assertEquals("", tool.name());
        assertEquals("/default", tool.uri());
        assertEquals("", tool.description());
        assertEquals("", tool.pathParams());
        assertEquals("", tool.queryParams());
        assertEquals("", tool.requestBody());
        assertEquals("POST", tool.method());
        assertFalse(tool.isPublic());
        assertEquals("", tool.contentType());
    }
}
