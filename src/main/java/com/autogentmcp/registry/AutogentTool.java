package com.autogentmcp.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AutogentTool {
    String endpointUri();
    String endpointDescription() default "";
    String parameterDetails() default ""; // JSON string or key for parameter details
}
