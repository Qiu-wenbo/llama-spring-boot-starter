package io.github.kabi.llama.llamaspringbootstarter.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Llama模型配置类
 * 从yaml文件中读取llama相关配置
 */
@Data
@ConfigurationProperties(prefix = "llama")
public class LlamaProperties {
    
    /**
     * 模型服务基础URL
     */
    private String baseUrl;
    
    /**
     * 默认模型名称
     */
    private String model;
    
    /**
     * 温度参数，控制生成内容的随机性
     */
    private double temperature = 0.7;
    
    /**
     * 最大生成token数
     */
    private int maxTokens = 1000;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 认证用户名
     */
    private String username;
    
    /**
     * 认证密码
     */
    private String password;
    
    /**
     * 是否启用记忆功能
     */
    private boolean enableMemory = false;
    
    /**
     * 是否启用RAG功能
     */
    private boolean enableRAG = false;
    
    /**
     * 多模型配置，支持配置多个不同的模型
     */
    private Map<String, ModelConfig> models;
    
    /**
     * RAG配置
     */
    private RagConfig rag;
    
    /**
     * 单个模型的详细配置
     */
    @Data
    public static class ModelConfig {
        private String baseUrl;
        private double temperature = 0.7;
        private int maxTokens = 1000;
        private String apiKey;
        private String username;
        private String password;
    }
    
    /**
     * RAG配置类
     */
    @Data
    public static class RagConfig {
        /**
         * 默认知识库
         */
        private String defaultKnowledgeBase;
        
        /**
         * 知识库配置
         */
        private Map<String, KnowledgeBaseConfig> knowledgeBases;
        
        /**
         * 检索相似度阈值
         */
        private double similarityThreshold = 0.7;
        
        /**
         * 每次检索的文档数量
         */
        private int topK = 3;
    }
    
    /**
     * 知识库配置
     */
    @Data
    public static class KnowledgeBaseConfig {
        /**
         * 知识库路径
         */
        private String path;
        
        /**
         * 知识库类型
         */
        private String type = "vector";
        
        /**
         * 自定义相似度阈值
         */
        private Double similarityThreshold;
        
        /**
         * 自定义topK值
         */
        private Integer topK;
    }
}