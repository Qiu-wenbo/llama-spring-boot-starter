package io.github.kabi.llama.llamaspringbootstarter.feature;

import io.github.kabi.llama.llamaspringbootstarter.aiservice.AiServices;
import io.github.kabi.llama.llamaspringbootstarter.tool.Tool;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具功能注册器
 * 用于为智能体添加工具功能
 */
public class ToolFeatureRegistry extends AbstractFeatureRegistry {
    
    private final List<Tool> tools = new ArrayList<>();
    
    public ToolFeatureRegistry() {
        super("tool");
    }
    
    /**
     * 添加工具
     */
    public ToolFeatureRegistry addTool(Tool tool) {
        if (tool != null) {
            this.tools.add(tool);
        }
        return this;
    }
    
    /**
     * 获取所有工具
     */
    public List<Tool> getTools() {
        return new ArrayList<>(tools);
    }
    
    @Override
    public void registerTo(AiServices aiServices) {
        if (!isEnabled()) {
            return;
        }
        
        // 注册所有工具
        for (Tool tool : tools) {
            aiServices.addTool(tool);
        }
    }
}