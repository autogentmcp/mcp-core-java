package com.autogentmcp.registry;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Component
@ComponentScan(basePackageClasses = {EnableAutogentMcpModule.class})
public class EnableAutogentMcpModule {
    
}
