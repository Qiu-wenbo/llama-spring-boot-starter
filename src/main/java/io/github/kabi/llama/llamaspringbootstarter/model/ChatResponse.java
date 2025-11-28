package io.github.kabi.llama.llamaspringbootstarter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天响应类
 * 表示AI模型的聊天响应结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    
    /**
     * 响应内容
     */
    private String content;
    
    /**
     * 响应消息对象
     */
    private ChatMessage message;
    
    /**
     * 响应ID
     */
    private String responseId;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 处理时间（毫秒）
     */
    private long processingTime;
    
    /**
     * 是否为流式响应的最后一部分
     */
    private boolean isLast;
    
    /**
     * 错误信息（如果有）
     */
    private String error;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 创建成功响应的静态方法
     */
    public static ChatResponse success(String content, String model) {
        return ChatResponse.builder()
                .content(content)
                .message(ChatMessage.assistant(content))
                .responseId(generateResponseId())
                .model(model)
                .success(true)
                .isLast(true)
                .build();
    }
    
    /**
     * 创建错误响应的静态方法
     */
    public static ChatResponse error(String errorMessage) {
        return ChatResponse.builder()
                .error(errorMessage)
                .responseId(generateResponseId())
                .success(false)
                .build();
    }
    
    /**
     * 生成唯一响应ID
     */
    private static String generateResponseId() {
        return "resp_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
}