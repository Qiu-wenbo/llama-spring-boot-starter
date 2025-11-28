package io.github.kabi.llama.llamaspringbootstarter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI方法注解，用于标记AI服务接口中的方法
 * 定义方法的提示词模板、参数映射等配置
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiMethod {
    
    /**
     * 方法的提示词模板，支持使用{参数名}作为占位符
     * 例如："请分析以下文本：{content}"
     */
    String prompt() default "";
    
    /**
     * 是否使用流式响应
     */
    boolean streaming() default false;
    
    /**
     * 是否使用会话记忆
     * 如果不指定，则继承@AiService的配置
     */
    boolean useMemory() default false;
    
    /**
     * 是否使用RAG功能
     * 如果不指定，则继承@AiService的配置
     */
    boolean useRAG() default false;
    
    /**
     * 使用的知识库列表
     * 仅在useRAG=true时有效
     */
    String[] knowledgeBases() default {};
}
