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
    String parameters() default ""; // JSON string or key for parameter details
}
