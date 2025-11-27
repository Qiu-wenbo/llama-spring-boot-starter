package com.tal.ollama.ollamaspringbootstarter.autoconfigure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaAutoConfiguration {
    
}
