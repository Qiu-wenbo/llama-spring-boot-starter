package io.github.kabi.llama.llamaspringbootstarter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息类
 * 表示AI模型的输入和输出消息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    
    /**
     * 消息角色：system、user、assistant
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息ID，用于唯一标识一条消息
     */
    private String messageId;
    
    /**
     * 时间戳
     */
    private long timestamp;
    
    /**
     * 创建系统消息的静态方法
     */
    public static ChatMessage system(String content) {
        return ChatMessage.builder()
                .role("system")
                .content(content)
                .messageId(generateMessageId())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建用户消息的静态方法
     */
    public static ChatMessage user(String content) {
        return ChatMessage.builder()
                .role("user")
                .content(content)
                .messageId(generateMessageId())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建助手消息的静态方法
     */
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder()
                .role("assistant")
                .content(content)
                .messageId(generateMessageId())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 生成唯一消息ID
     */
    private static String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
}