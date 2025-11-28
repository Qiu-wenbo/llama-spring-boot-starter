package io.github.kabi.llama.llamaspringbootstarter.example;

import io.github.kabi.llama.llamaspringbootstarter.annotation.AiMethod;
import io.github.kabi.llama.llamaspringbootstarter.annotation.AiService;

/**
 * 天气助手示例接口
 * 演示如何使用@AiService注解定义AI服务接口
 */
@AiService(model = "llama3", enableMemory = true)
public interface WeatherAssistant {
    
    /**
     * 获取天气信息
     */
    @AiMethod(prompt = "查询{city}的当前天气状况")
    String getWeather(String city);
    
    /**
     * 获取天气预报
     */
    @AiMethod(prompt = "提供{city}未来{days}天的天气预报", useMemory = true)
    String getWeatherForecast(String city, int days);
    
    /**
     * 获取穿衣建议
     */
    @AiMethod(prompt = "根据{city}的天气，给出今天的穿衣建议", useMemory = true, useRAG = true)
    String getClothingSuggestion(String city);
}