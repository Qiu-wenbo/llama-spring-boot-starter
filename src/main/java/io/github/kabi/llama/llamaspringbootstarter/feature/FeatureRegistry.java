package io.github.kabi.llama.llamaspringbootstarter.feature;

import io.github.kabi.llama.llamaspringbootstarter.aiservice.AiServices;

/**
 * 功能注册器接口
 * 用于向智能体注册各种功能
 */
public interface FeatureRegistry {
    
    /**
     * 注册功能到智能体
     * @param aiServices 智能体服务
     */
    void registerTo(AiServices aiServices);
    
    /**
     * 获取功能名称
     */
    String getFeatureName();
    
    /**
     * 检查功能是否已启用
     */
    boolean isEnabled();
    
    /**
     * 设置功能是否启用
     */
    void setEnabled(boolean enabled);
}