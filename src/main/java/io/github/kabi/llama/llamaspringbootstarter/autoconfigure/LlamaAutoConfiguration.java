package io.github.kabi.llama.llamaspringbootstarter.autoconfigure;

import io.github.kabi.llama.llamaspringbootstarter.annotation.AiService;
import io.github.kabi.llama.llamaspringbootstarter.processor.AiServicePostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Llama自动配置类
 * 负责自动配置Llama相关的Bean
 */
@Configuration
@EnableConfigurationProperties(LlamaProperties.class)
public class LlamaAutoConfiguration {
    
    @Autowired
    private LlamaProperties llamaProperties;
    
    /**
     * 注册模型配置映射
     * 用于在运行时根据模型名称获取对应的配置
     */
    @Bean
    public Map<String, LlamaProperties.ModelConfig> modelConfigs() {
        Map<String, LlamaProperties.ModelConfig> configs = new HashMap<>();
        
        // 注册默认模型配置
        if (llamaProperties.getModel() != null && llamaProperties.getBaseUrl() != null) {
            LlamaProperties.ModelConfig defaultConfig = new LlamaProperties.ModelConfig();
            defaultConfig.setBaseUrl(llamaProperties.getBaseUrl());
            defaultConfig.setTemperature(llamaProperties.getTemperature());
            defaultConfig.setMaxTokens(llamaProperties.getMaxTokens());
            defaultConfig.setApiKey(llamaProperties.getApiKey());
            defaultConfig.setUsername(llamaProperties.getUsername());
            defaultConfig.setPassword(llamaProperties.getPassword());
            
            configs.put(llamaProperties.getModel(), defaultConfig);
        }
        
        // 注册自定义模型配置
        if (llamaProperties.getModels() != null && !llamaProperties.getModels().isEmpty()) {
            configs.putAll(llamaProperties.getModels());
        }
        
        return configs;
    }
    
    /**
     * 注册知识库配置映射
     */
    @Bean
    public Map<String, LlamaProperties.KnowledgeBaseConfig> knowledgeBaseConfigs() {
        Map<String, LlamaProperties.KnowledgeBaseConfig> configs = new HashMap<>();
        
        if (llamaProperties.getRag() != null && llamaProperties.getRag().getKnowledgeBases() != null) {
            configs.putAll(llamaProperties.getRag().getKnowledgeBases());
        }
        
        return configs;
    }
    
    /**
     * 注册AI服务后处理器
     * 用于处理@AiService注解的接口，创建动态代理
     */
    @Bean
    @ConditionalOnMissingBean
    public AiServicePostProcessor aiServicePostProcessor() {
        return new AiServicePostProcessor();
    }
}