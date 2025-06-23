package com.autogentmcp.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AutogentTool {
    String uri();
    String description() default "";
    /**
     * If empty, path/query/requestBody params will be deduced from method signature and Spring annotations.
     * If non-empty, this value will override the deduced value.
     */
    String pathParams() default ""; // Optional override for path parameter details
    String queryParams() default ""; // Optional override for query parameter details
    String requestBody() default ""; // Optional override for request body schema
    String method() default "POST"; // HTTP method, default POST
}
