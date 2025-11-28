package io.github.kabi.llama.llamaspringbootstarter.feature;

import io.github.kabi.llama.llamaspringbootstarter.aiservice.AiServices;
import lombok.Data;
import lombok.Getter;

/**
 * 功能注册器抽象基类
 * 提供功能注册器的基本实现
 */
@Data
public abstract class AbstractFeatureRegistry implements FeatureRegistry {
    
    private final String featureName;
    private boolean enabled = true;
    
    public AbstractFeatureRegistry(String featureName) {
        this.featureName = featureName;
    }
    
    @Override
    public abstract void registerTo(AiServices aiServices);
}