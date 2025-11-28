package io.github.kabi.llama.llamaspringbootstarter.example;

import io.github.kabi.llama.llamaspringbootstarter.annotation.AiMethod;
import io.github.kabi.llama.llamaspringbootstarter.annotation.AiService;
import io.github.kabi.llama.llamaspringbootstarter.model.ChatMessage;
import io.github.kabi.llama.llamaspringbootstarter.model.ChatResponse;
import reactor.core.publisher.Flux;
import java.util.List;

/**
 * 文档处理器示例接口
 * 演示高级功能如流式响应和ChatMessage使用
 */
@AiService(model = "llama3", enableMemory = true)
public interface DocumentProcessor {
    
    /**
     * 总结文档内容
     */
    @AiMethod(prompt = "总结以下文档内容，保持核心观点：{document}")
    String summarizeDocument(String document);
    
    /**
     * 流式处理文档问答
     */
    @AiMethod(prompt = "基于文档内容回答问题：\n文档：{document}\n问题：{question}")
    Flux<String> streamAnswerQuestion(String document, String question);
    
    /**
     * 使用ChatMessage进行高级交互
     */
    @AiMethod
    ChatResponse processWithMessages(List<ChatMessage> messages);
    
    /**
     * 使用ChatMessage进行流式高级交互
     */
    @AiMethod
    Flux<ChatResponse> streamProcessWithMessages(List<ChatMessage> messages);
}