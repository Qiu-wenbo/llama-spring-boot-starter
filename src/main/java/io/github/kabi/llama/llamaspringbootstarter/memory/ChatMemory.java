package io.github.kabi.llama.llamaspringbootstarter.memory;

import io.github.kabi.llama.llamaspringbootstarter.model.ChatMessage;
import java.util.List;

/**
 * 聊天记忆接口
 * 用于管理和存储智能体的会话记忆
 */
public interface ChatMemory {
    
    /**
     * 设置记忆消息
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     */
    void setMemoryMessages(Object sessionId, ChatMessage userMessage);

    /**
     * 获取记忆消息
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> getMemoryMessages(Object sessionId);

}