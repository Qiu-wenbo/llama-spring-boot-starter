package io.github.kabi.llama.llamaspringbootstarter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * AI服务代理注解，用于标记接口为AI服务代理接口
 * 框架会自动为标记了此注解的接口创建代理实现
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component // 标记为组件，方便 Spring 扫描和管理
public @interface AiService {
    
    /**
     * 指定使用的模型名称，用于在框架中查找对应的模型客户端
     * 如果不指定，则使用默认模型
     */
    String model() default "";
    
    /**
     * 是否启用会话记忆功能
     */
    boolean enableMemory() default false;
    
    /**
     * 是否启用RAG功能
     */
    boolean enableRAG() default false;
}