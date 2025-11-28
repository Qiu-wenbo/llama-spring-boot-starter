package io.github.kabi.llama.llamaspringbootstarter.aiservice;

import io.github.kabi.llama.llamaspringbootstarter.client.ChatModelClient;
import io.github.kabi.llama.llamaspringbootstarter.feature.FeatureManager;
import io.github.kabi.llama.llamaspringbootstarter.memory.ChatMemory;
import io.github.kabi.llama.llamaspringbootstarter.rag.KnowledgeBase;
import io.github.kabi.llama.llamaspringbootstarter.tool.Tool;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI服务工具类
 * 用于构建和管理智能体代理，支持配置各种功能如记忆、RAG、工具等
 */
@Getter
@Builder
@Slf4j
public class AiServices {
    
    private final ChatModelClient chatModelClient;
    private final String model;
    private ChatMemory chatMemory;
    private boolean enableMemory;
    private boolean enableRAG;
    private List<KnowledgeBase> knowledgeBases;
    private List<Tool> tools;
    private Map<String, Object> attributes;
    
    /**
     * 代理实例缓存
     */
    private static final Map<String, Object> agentCache = new ConcurrentHashMap<>();
    
    /**
     * 设置记忆组件
     */
    public AiServices withMemory(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.enableMemory = true;
        return this;
    }
    
    /**
     * 设置是否启用记忆功能
     */
    public AiServices enableMemory(boolean enableMemory) {
        this.enableMemory = enableMemory;
        return this;
    }
    
    /**
     * 设置是否启用RAG功能
     */
    public AiServices enableRAG(boolean enableRAG) {
        this.enableRAG = enableRAG;
        return this;
    }
    
    /**
     * 添加知识库
     */
    public AiServices addKnowledgeBase(KnowledgeBase knowledgeBase) {
        if (this.knowledgeBases == null) {
            this.knowledgeBases = new ArrayList<>();
        }
        this.knowledgeBases.add(knowledgeBase);
        return this;
    }
    
    /**
     * 添加工具
     */
    public AiServices addTool(Tool tool) {
        if (this.tools == null) {
            this.tools = new ArrayList<>();
        }
        this.tools.add(tool);
        return this;
    }
    
    /**
     * 设置属性
     */
    public AiServices setAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new ConcurrentHashMap<>();
        }
        this.attributes.put(key, value);
        return this;
    }
    
    /**
     * 获取属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        if (this.attributes == null) {
            return null;
        }
        return (T) this.attributes.get(key);
    }
    
    /**
     * 使用特性管理器注册功能
     */
    public AiServices withFeatures(FeatureManager featureManager) {
        if (featureManager != null) {
            featureManager.registerFeaturesTo(this);
        }
        return this;
    }
    
    /**
     * 创建智能体代理
     */
    @SuppressWarnings("unchecked")
    public <T> T createAgent(Class<T> agentInterface) {
        if (agentInterface == null) {
            throw new IllegalArgumentException("Agent interface cannot be null");
        }
        
        String cacheKey = model + ":" + agentInterface.getName();
        
        // 检查缓存
        return (T) agentCache.computeIfAbsent(cacheKey, k -> {
            log.info("Creating new agent proxy for: {}", agentInterface.getName());
            return Proxy.newProxyInstance(
                    agentInterface.getClassLoader(),
                    new Class<?>[]{agentInterface},
                    new AgentInvocationHandler(this)
            );
        });
    }
    
    /**
     * 创建新的代理实例
     */
    private <T> T createNewAgent(Class<T> agentInterface) {
        InvocationHandler handler = new AgentInvocationHandler(this);
        
        @SuppressWarnings("unchecked")
        T proxy = (T) Proxy.newProxyInstance(
                agentInterface.getClassLoader(),
                new Class<?>[] { agentInterface },
                handler
        );
        
        return proxy;
    }
    
    /**
     * 智能体调用处理器
     */
    private static class AgentInvocationHandler implements InvocationHandler {
        
        private final AiServices aiServices;
        
        public AgentInvocationHandler(AiServices aiServices) {
            this.aiServices = aiServices;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 如果调用的是Object类的方法，直接调用
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            // 获取方法名作为提示词
            String prompt = method.getName();
            
            // 构建完整提示词，包含参数信息
            StringBuilder fullPrompt = new StringBuilder(prompt);
            if (args != null && args.length > 0) {
                fullPrompt.append("\n参数: ");
                for (int i = 0; i < args.length; i++) {
                    fullPrompt.append("param").append(i).append(": ");
                    fullPrompt.append(args[i] != null ? args[i].toString() : "null");
                    if (i < args.length - 1) {
                        fullPrompt.append(", ");
                    }
                }
            }
            
            // 生成会话ID（如果启用记忆功能）
            String sessionId = aiServices.enableMemory ? generateSessionId() : null;
            
            // 调用模型客户端
            ChatModelClient client = aiServices.chatModelClient;
            if (client != null) {
                // 根据配置选择合适的方法
                if (aiServices.enableRAG && aiServices.knowledgeBases != null && !aiServices.knowledgeBases.isEmpty()) {
                    // 使用RAG功能
                    String[] kbNames = aiServices.knowledgeBases.stream()
                            .map(KnowledgeBase::getName)
                            .toArray(String[]::new);
                    
                    if (aiServices.enableMemory) {
                        return client.ragChat(sessionId, fullPrompt.toString(), kbNames);
                    } else {
                        return client.ragChat(fullPrompt.toString(), kbNames);
                    }
                } else {
                    // 普通对话
                    if (aiServices.enableMemory) {
                        return client.chat(sessionId, fullPrompt.toString());
                    } else {
                        return client.chat(fullPrompt.toString());
                    }
                }
            }
            
            // 如果没有客户端，返回默认响应
            return "AI代理响应: " + fullPrompt;
        }
        
        /**
         * 生成会话ID
         */
        private String generateSessionId() {
            return "agent-session-" + System.currentTimeMillis() + "-" + Math.random();
        }
    }
}
