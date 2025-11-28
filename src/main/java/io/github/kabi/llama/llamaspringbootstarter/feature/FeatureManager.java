package io.github.kabi.llama.llamaspringbootstarter.feature;

import io.github.kabi.llama.llamaspringbootstarter.aiservice.AiServices;
import io.github.kabi.llama.llamaspringbootstarter.memory.ChatMemory;
import io.github.kabi.llama.llamaspringbootstarter.rag.KnowledgeBase;
import io.github.kabi.llama.llamaspringbootstarter.tool.Tool;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 特性管理器
 * 用于统一管理所有功能注册器
 */
@Getter
public class FeatureManager {
    
    private final Map<String, FeatureRegistry> featureRegistryMap = new HashMap<>();
    
    /**
     * 添加功能注册器
     */
    public FeatureManager registerFeature(FeatureRegistry featureRegistry) {
        if (featureRegistry != null) {
            featureRegistryMap.put(featureRegistry.getFeatureName(), featureRegistry);
        }
        return this;
    }
    
    /**
     * 启用记忆功能
     */
    public FeatureManager enableMemory(ChatMemory chatMemory) {
        MemoryFeatureRegistry memoryFeature = new MemoryFeatureRegistry(chatMemory);
        memoryFeature.setEnabled(true);
        registerFeature(memoryFeature);
        return this;
    }
    
    /**
     * 添加知识库
     */
    public FeatureManager addKnowledgeBase(KnowledgeBase knowledgeBase) {
        RagFeatureRegistry ragFeature = (RagFeatureRegistry) featureRegistryMap.computeIfAbsent("rag", 
                k -> new RagFeatureRegistry());
        if (!(ragFeature instanceof RagFeatureRegistry)) {
            ragFeature = new RagFeatureRegistry();
            featureRegistryMap.put("rag", ragFeature);
        }
        ragFeature.addKnowledgeBase(knowledgeBase);
        ragFeature.setEnabled(true);
        return this;
    }
    
    /**
     * 添加工具
     */
    public FeatureManager addTool(Tool tool) {
        ToolFeatureRegistry toolFeature = (ToolFeatureRegistry) featureRegistryMap.computeIfAbsent("tool", 
                k -> new ToolFeatureRegistry());
        if (!(toolFeature instanceof ToolFeatureRegistry)) {
            toolFeature = new ToolFeatureRegistry();
            featureRegistryMap.put("tool", toolFeature);
        }
        toolFeature.addTool(tool);
        toolFeature.setEnabled(true);
        return this;
    }
    
    /**
     * 启用或禁用指定功能
     */
    public FeatureManager setFeatureEnabled(String featureName, boolean enabled) {
        FeatureRegistry registry = featureRegistryMap.get(featureName);
        if (registry != null) {
            registry.setEnabled(enabled);
        }
        return this;
    }
    
    /**
     * 获取指定功能注册器
     */
    @SuppressWarnings("unchecked")
    public <T extends FeatureRegistry> T getFeatureRegistry(String featureName) {
        return (T) featureRegistryMap.get(featureName);
    }
    
    /**
     * 为智能体注册所有功能
     */
    public void registerFeaturesTo(AiServices aiServices) {
        if (aiServices == null) {
            return;
        }
        
        for (FeatureRegistry registry : featureRegistryMap.values()) {
            registry.registerTo(aiServices);
        }
    }
}