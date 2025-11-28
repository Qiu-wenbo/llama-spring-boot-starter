package io.github.kabi.llama.llamaspringbootstarter.tool;

import java.util.Map;

/**
 * 工具接口
 * 定义智能体可调用的工具
 */
public interface Tool {
    
    /**
     * 获取工具名称
     */
    String getName();
    
    /**
     * 获取工具描述
     */
    String getDescription();
    
    /**
     * 获取工具参数信息
     * 返回参数名到参数描述的映射
     */
    Map<String, String> getParameters();
    
    /**
     * 执行工具
     * @param parameters 工具参数
     * @return 执行结果
     */
    Object execute(Map<String, Object> parameters);
    
    /**
     * 获取工具的JSON Schema描述
     * 用于向大模型描述工具的参数结构
     */
    String getJsonSchema();
}