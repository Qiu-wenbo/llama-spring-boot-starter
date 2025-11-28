package io.github.kabi.llama.llamaspringbootstarter.example;

import io.github.kabi.llama.llamaspringbootstarter.autoconfigure.LlamaProperties;
import io.github.kabi.llama.llamaspringbootstarter.aiservice.AiServices;
import io.github.kabi.llama.llamaspringbootstarter.client.ChatModelClient;
import io.github.kabi.llama.llamaspringbootstarter.feature.FeatureManager;
import io.github.kabi.llama.llamaspringbootstarter.rag.Document;
import io.github.kabi.llama.llamaspringbootstarter.rag.KnowledgeBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Llama Spring Boot Starter示例配置类
 * 演示框架的配置和高级使用方式
 */
@Configuration
public class LlamaExampleConfig {
    
    @Autowired
    private LlamaProperties properties;
    
    /**
     * 初始化功能管理器
     */
    @Bean
    public FeatureManager initFeatureManager() {
        FeatureManager featureManager = FeatureManager.getInstance();
        
        // 注册自定义功能
        featureManager.registerFeature("customWeather", context -> {
            // 自定义天气功能实现
            System.out.println("自定义天气功能已激活");
            return true;
        });
        
        featureManager.registerFeature("advancedRAG", context -> {
            // 高级RAG功能实现
            System.out.println("高级RAG功能已激活");
            return true;
        });
        
        return featureManager;
    }
    
    /**
     * 配置知识图谱
     * 注意：这里返回的是一个示例实现，实际使用时应注入框架提供的KnowledgeBase实现类
     */
    @Bean
    public KnowledgeBase weatherKnowledgeBase() {
        // 由于KnowledgeBase是接口，这里返回一个简单的实现示例
        // 在实际应用中，Spring会注入正确的实现类
        return new KnowledgeBase() {
            @Override
            public String getName() {
                return "weather_knowledge_base";
            }
            
            @Override
            public String getType() {
                return "memory";
            }
            
            @Override
            public String getPath() {
                return "/memory/weather";
            }
            
            @Override
            public List<Document> search(String query, int topK, double similarityThreshold) {
                // 简单实现，实际应用中应根据查询返回相关文档
                System.out.println("搜索知识库: " + query);
                return java.util.Collections.emptyList();
            }
            
            @Override
            public void addDocument(Document document) {
                System.out.println("添加文档到知识库: " + document.getId());
            }
            
            @Override
            public void addDocuments(List<Document> documents) {
                for (Document doc : documents) {
                    addDocument(doc);
                }
            }
            
            @Override
            public void removeDocument(String documentId) {
                System.out.println("从知识库移除文档: " + documentId);
            }
            
            @Override
            public void clear() {
                System.out.println("清空知识库");
            }
        };
    }
    
    /**
     * 自定义天气助手Bean配置
     */
    @Bean
    public WeatherAssistant weatherAssistant(ChatModelClient chatModelClient, 
                                          FeatureManager featureManager,
                                          KnowledgeBase weatherKnowledgeBase) {
        return AiServices.builder()
                .withClient(chatModelClient)
                .withMemory(properties.isEnableMemory())
                .withFeatures(featureManager)
                .addKnowledgeBase(weatherKnowledgeBase)
                .withMaxTokens(2048)
                .withTemperature(0.7)
                .build()
                .create(WeatherAssistant.class);
    }
    
    /**
     * 打印当前配置信息
     */
    @Bean
    public String printConfigInfo() {
        System.out.println("=== Llama Spring Boot Starter配置信息 ===");
        System.out.println("模型名称: " + properties.getModel());
        System.out.println("是否启用记忆功能: " + properties.isEnableMemory());
        System.out.println("最大token数: " + properties.getMaxTokens());
        System.out.println("温度参数: " + properties.getTemperature());
        return "Config info printed";
    }
}