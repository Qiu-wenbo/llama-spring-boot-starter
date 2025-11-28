package io.github.kabi.llama.llamaspringbootstarter.example;

import io.github.kabi.llama.llamaspringbootstarter.aiservice.AiServices;
import io.github.kabi.llama.llamaspringbootstarter.feature.FeatureManager;
import io.github.kabi.llama.llamaspringbootstarter.model.ChatMessage;
import io.github.kabi.llama.llamaspringbootstarter.model.ChatResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import java.util.Arrays;
import java.util.List;

/**
 * Llama Spring Boot Starter示例应用
 * 展示框架的完整使用方式
 */
@SpringBootApplication
public class LlamaSpringBootExampleApplication {
    
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(LlamaSpringBootExampleApplication.class, args);
        
        // 演示1：使用注解方式的AI服务
        WeatherAssistant weatherAssistant = context.getBean(WeatherAssistant.class);
        System.out.println("=== 天气助手演示 ===");
        System.out.println("北京天气：" + weatherAssistant.getWeather("北京"));
        System.out.println("上海3天预报：" + weatherAssistant.getWeatherForecast("上海", 3));
        
        // 演示2：使用文档处理器
        DocumentProcessor documentProcessor = context.getBean(DocumentProcessor.class);
        String document = "Spring Boot是由Pivotal团队提供的全新框架，其设计目的是用来简化新Spring应用的初始搭建以及开发过程。该框架使用了特定的方式来进行配置，从而使开发人员不再需要定义样板化的配置。";
        System.out.println("\n=== 文档处理器演示 ===");
        System.out.println("文档摘要：" + documentProcessor.summarizeDocument(document));
        
        // 演示3：流式处理
        System.out.println("\n=== 流式响应演示 ===");
        Flux<String> stream = documentProcessor.streamAnswerQuestion(document, "Spring Boot的主要目的是什么？");
        stream.subscribe(chunk -> System.out.print(chunk));
        
        // 演示4：直接使用AiServices API
        System.out.println("\n\n=== AiServices API演示 ===");
        FeatureManager featureManager = FeatureManager.getInstance();
        WeatherAssistant directClient = AiServices.builder()
                .withMemory(true)
                .withFeatures(featureManager)
                .build()
                .create(WeatherAssistant.class);
        System.out.println("直接API调用结果：" + directClient.getWeather("广州"));
        
        // 演示5：使用ChatMessage进行高级交互
        System.out.println("\n=== ChatMessage高级交互演示 ===");
        List<ChatMessage> messages = Arrays.asList(
                ChatMessage.system("你是一个专业的技术顾问"),
                ChatMessage.user("请简要介绍Spring Boot框架")
        );
        ChatResponse response = documentProcessor.processWithMessages(messages);
        System.out.println("高级交互结果：" + response.getContent());
    }
    
    /**
     * 注册文档处理器示例Bean
     */
    @Bean
    public DocumentProcessor documentProcessor() {
        FeatureManager featureManager = FeatureManager.getInstance();
        return AiServices.builder()
                .withMemory(true)
                .withFeatures(featureManager)
                .build()
                .create(DocumentProcessor.class);
    }
}