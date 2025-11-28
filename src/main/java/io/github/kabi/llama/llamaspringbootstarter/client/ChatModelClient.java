package io.github.kabi.llama.llamaspringbootstarter.client;

import io.github.kabi.llama.llamaspringbootstarter.model.ChatMessage;
import io.github.kabi.llama.llamaspringbootstarter.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天模型客户端接口
 * 定义与AI模型交互的标准方法
 */
public interface ChatModelClient {

    /**
     * 基础流式聊天 - 单条提示词
     */
    Flux<ChatResponse> streamChat(String prompt);

    /**
     * 带会话的流式聊天
     */
    default Flux<ChatResponse> streamChat(Object sessionId, String prompt){
        throw new UnsupportedOperationException("memory stream chat is not supported");
    }

    /**
     * RAG流式聊天
     */
    default Flux<ChatResponse> ragStreamChat(String prompt, String... knowledgeBases){
        throw new UnsupportedOperationException("RAG stream chat is not supported");
    }

    /**
     * 带会话的RAG流式聊天
     */
    default Flux<ChatResponse> ragStreamChat(Object sessionId, String prompt, String... knowledgeBases){
        throw new UnsupportedOperationException("RAG stream chat is not supported");
    }

    /**
     * 基础非流式聊天 - 单条提示词
     */
    ChatResponse chat(String prompt);

    /**
     * 带会话的非流式聊天
     */
    default ChatResponse chat(Object sessionId, String prompt){
        throw new UnsupportedOperationException("memory chat is not supported");
    }

    /**
     * RAG非流式聊天
     */
    default ChatResponse ragChat(String prompt, String... knowledgeBases){
        throw new UnsupportedOperationException("RAG chat is not supported");
    }

    /**
     * 带会话的RAG非流式聊天
     */
    default ChatResponse ragChat(Object sessionId, String prompt, String... knowledgeBases){
        throw new UnsupportedOperationException("RAG chat is not supported");
    }

    /**
     * 高级接口 - 消息列表流式聊天
     */
    default Flux<ChatResponse> streamChat(List<ChatMessage> messages, String model, Double temperature) {
        throw new UnsupportedOperationException("streamChat with messages is not supported");
    }

    /**
     * 高级接口 - 消息列表非流式聊天
     */
    default ChatResponse chat(List<ChatMessage> messages, String model, Double temperature) {
        throw new UnsupportedOperationException("chat with messages is not supported");
    }

    /**
     * 高级接口 - 带上下文的消息列表流式聊天
     */
    default Flux<ChatResponse> streamChatWithContext(List<ChatMessage> messages, String model, Double temperature, String contextId) {
        throw new UnsupportedOperationException("streamChatWithContext is not supported");
    }

    /**
     * 高级接口 - 带上下文的消息列表非流式聊天
     */
    default ChatResponse chatWithContext(List<ChatMessage> messages, String model, Double temperature, String contextId) {
        throw new UnsupportedOperationException("chatWithContext is not supported");
    }

    /**
     * 高级接口 - RAG消息列表流式聊天
     */
    default Flux<ChatResponse> streamRagChat(List<ChatMessage> messages, String model, Double temperature, List<String> knowledgeBaseIds) {
        throw new UnsupportedOperationException("streamRagChat is not supported");
    }

    /**
     * 高级接口 - RAG消息列表非流式聊天
     */
    default ChatResponse ragChat(List<ChatMessage> messages, String model, Double temperature, List<String> knowledgeBaseIds) {
        throw new UnsupportedOperationException("ragChat is not supported");
    }
}