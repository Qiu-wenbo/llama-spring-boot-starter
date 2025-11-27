package com.tal.ollama.ollamaspringbootstarter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    private String baseUrl;
    private String model;

}
