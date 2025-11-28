package io.github.kabi.llama.llamaspringbootstarter.processor;

import io.github.kabi.llama.llamaspringbootstarter.annotation.UserMessage;
import io.github.kabi.llama.llamaspringbootstarter.annotation.AiService;
import io.github.kabi.llama.llamaspringbootstarter.autoconfigure.LlamaProperties;
import io.github.kabi.llama.llamaspringbootstarter.memory.ChatMemory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AiService注解的后置处理器，用于为标记了@AiService的接口创建代理实现
 */
public class AiServicePostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor, InitializingBean {

    // 存储模型名称到客户端的映射
    private final Map<String, Object> modelClients = new ConcurrentHashMap<>();
    
    // 存储已处理的代理接口
    private final Map<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();
    
    // Spring容器
    private ConfigurableListableBeanFactory beanFactory;
    
    // 模型配置和知识库配置
    private Map<String, LlamaProperties.ModelConfig> modelConfigs;
    private Map<String, LlamaProperties.KnowledgeBaseConfig> knowledgeBaseConfigs;
    
    // 会话记忆组件
    private ChatMemory chatMemory;
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 从Spring容器中获取模型配置
        this.modelConfigs = beanFactory.getBean("modelConfigs", Map.class);
        this.knowledgeBaseConfigs = beanFactory.getBean("knowledgeBaseConfigs", Map.class);
        
        // 获取ChatMemory实例
        try {
            this.chatMemory = beanFactory.getBean(ChatMemory.class);
        } catch (BeansException e) {
            // 如果ChatMemory不存在，则忽略
        }
    }
    
    /**
     * 注册模型客户端
     * @param modelName 模型名称
     * @param client 客户端实例
     */
    public void registerModelClient(String modelName, Object client) {
        modelClients.put(modelName, client);
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 对于接口类型的Bean，检查是否有@AiService注解
        if (bean instanceof Class<?> && ((Class<?>) bean).isInterface()) {
            Class<?> interfaceType = (Class<?>) bean;
            AiService aiService = AnnotationUtils.findAnnotation(interfaceType, AiService.class);
            
            if (aiService != null) {
                // 如果缓存中已有此接口的代理，则直接返回
                if (proxyCache.containsKey(interfaceType)) {
                    return proxyCache.get(interfaceType);
                }
                
                // 创建代理实现
                Object proxy = createProxy(interfaceType, aiService);
                proxyCache.put(interfaceType, proxy);
                return proxy;
            }
        }
        return bean;
    }
    
    /**
     * 为接口创建代理实现
     */
    private Object createProxy(Class<?> interfaceType, AiService aiService) {
        String modelName = aiService.model();
        boolean enableMemory = aiService.enableMemory();
        boolean enableRAG = aiService.enableRAG();
        
        // 获取对应的模型客户端
        Object modelClient = getModelClient(modelName);
        
        // 创建动态代理
        AiServiceInvocationHandler handler = new AiServiceInvocationHandler(modelClient, enableMemory, enableRAG, chatMemory);
        handler.setModelConfigs(modelConfigs);
        handler.setKnowledgeBaseConfigs(knowledgeBaseConfigs);
        
        return Proxy.newProxyInstance(
            interfaceType.getClassLoader(),
            new Class<?>[] { interfaceType },
            handler
        );
    }
    
    /**
     * 获取模型客户端
     */
    private Object getModelClient(String modelName) {
        if (modelName != null && !modelName.isEmpty() && modelClients.containsKey(modelName)) {
            return modelClients.get(modelName);
        }
        // 返回默认客户端
        return modelClients.getOrDefault("default", null);
    }
    
    /**
     * AI服务代理的调用处理器
     */
    private static class AiServiceInvocationHandler implements InvocationHandler {
        
        private final Object modelClient;
        private final boolean enableMemory;
        private final boolean enableRAG;
        private final ChatMemory chatMemory;
        private Map<String, LlamaProperties.ModelConfig> modelConfigs;
        private Map<String, LlamaProperties.KnowledgeBaseConfig> knowledgeBaseConfigs;
        
        public AiServiceInvocationHandler(Object modelClient, boolean enableMemory, boolean enableRAG, ChatMemory chatMemory) {
            this.modelClient = modelClient;
            this.enableMemory = enableMemory;
            this.enableRAG = enableRAG;
            this.chatMemory = chatMemory;
        }
        
        public void setModelConfigs(Map<String, LlamaProperties.ModelConfig> modelConfigs) {
            this.modelConfigs = modelConfigs;
        }
        
        public void setKnowledgeBaseConfigs(Map<String, LlamaProperties.KnowledgeBaseConfig> knowledgeBaseConfigs) {
            this.knowledgeBaseConfigs = knowledgeBaseConfigs;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 如果调用的是Object类的方法，直接调用
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            // 获取方法上的@userMessage注解
                UserMessage userMessage = AnnotationUtils.findAnnotation(method, UserMessage.class);
                
                if (userMessage != null) {
                    // 根据注解配置构建提示词
                    String prompt = buildPrompt(userMessage.prompt(), method, args);
                    
                    // 确定是否使用会话记忆和RAG
                    boolean useMemory = userMessage.useMemory() || this.enableMemory;
                    boolean useRAG = userMessage.useRAG() || this.enableRAG;
                    String[] knowledgeBases = userMessage.knowledgeBases();
                    
                    // 生成会话ID（如果启用记忆功能）
                    String sessionId = useMemory && chatMemory != null ? generateSessionId(method, args) : null;
                    
                    // 根据配置调用模型客户端的相应方法
                    if (modelClient != null && modelClient instanceof io.github.kabi.llama.llamaspringbootstarter.client.ChatModelClient) {
                        io.github.kabi.llama.llamaspringbootstarter.client.ChatModelClient chatClient = 
                            (io.github.kabi.llama.llamaspringbootstarter.client.ChatModelClient) modelClient;
                        
                        if (userMessage.streaming()) {
                        // 流式响应
                        if (useRAG) {
                            if (useMemory) {
                                return chatClient.ragStreamChat(sessionId, prompt, knowledgeBases);
                            } else {
                                return chatClient.ragStreamChat(prompt, knowledgeBases);
                            }
                        } else {
                            if (useMemory) {
                                return chatClient.streamChat(sessionId, prompt);
                            } else {
                                return chatClient.streamChat(prompt);
                            }
                        }
                    } else {
                        // 非流式响应
                        if (useRAG) {
                            if (useMemory) {
                                return chatClient.ragChat(sessionId, prompt, knowledgeBases);
                            } else {
                                return chatClient.ragChat(prompt, knowledgeBases);
                            }
                        } else {
                            if (useMemory) {
                                return chatClient.chat(sessionId, prompt);
                            } else {
                                return chatClient.chat(prompt);
                            }
                        }
                    }
                }
            }
            
            // 默认返回
            return "AI代理响应: " + method.getName();
        }
        
        /**
         * 构建提示词，替换占位符
         */
        private String buildPrompt(String template, Method method, Object[] args) {
            if (template.isEmpty()) {
                // 如果没有提供模板，则使用方法名作为提示词
                return method.getName();
            }
            
            // 获取方法参数名
            String[] paramNames = new String[method.getParameterCount()];
            // 这里简化处理，实际项目中可以使用Spring的ParameterNameDiscoverer或AspectJ来获取参数名
            for (int i = 0; i < paramNames.length; i++) {
                paramNames[i] = "param" + i;
            }
            
            // 替换模板中的占位符
            String prompt = template;
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                String placeholder = "{" + paramNames[i] + "}";
                prompt = prompt.replace(placeholder, args[i] != null ? args[i].toString() : "");
            }
            
            return prompt;
        }
        
        /**
         * 生成会话ID
         */
        private String generateSessionId(Method method, Object[] args) {
            StringBuilder sb = new StringBuilder("session-").append(method.getName());
            if (args != null && args.length > 0) {
                // 使用第一个参数作为会话标识符的一部分
                sb.append("-")
                  .append(args[0] != null ? args[0].hashCode() : "empty");
            }
            return sb.toString();
        }
    }
}
