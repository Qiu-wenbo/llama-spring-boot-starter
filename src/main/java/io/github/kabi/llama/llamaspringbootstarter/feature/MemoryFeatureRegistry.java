package io.github.kabi.llama.llamaspringbootstarter.feature;

import io.github.kabi.llama.llamaspringbootstarter.aiservice.AiServices;
import io.github.kabi.llama.llamaspringbootstarter.memory.ChatMemory;

/**
 * 记忆功能注册器
 * 用于为智能体添加记忆功能
 */
public class MemoryFeatureRegistry extends AbstractFeatureRegistry {
    
    private final ChatMemory chatMemory;
    
    public MemoryFeatureRegistry(ChatMemory chatMemory) {
        super("memory");
        this.chatMemory = chatMemory;
    }
    
    @Override
    public void registerTo(AiServices aiServices) {
        if (!isEnabled()) {
            return;
        }
        
        if (chatMemory != null) {
            aiServices.withMemory(chatMemory);
            aiServices.enableMemory(true);
        }
    }
    
    public ChatMemory getChatMemory() {
        return chatMemory;
    }
}