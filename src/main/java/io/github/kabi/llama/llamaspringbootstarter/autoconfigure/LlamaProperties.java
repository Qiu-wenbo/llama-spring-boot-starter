package io.github.kabi.llama.llamaspringbootstarter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "llama")
public class LlamaProperties {

    private String baseUrl;
    private String model;

}