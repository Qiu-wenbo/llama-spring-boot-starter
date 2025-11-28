package io.github.kabi.llama.llamaspringbootstarter.autoconfigure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlamaProperties.class)
public class LlamaAutoConfiguration {
    
}