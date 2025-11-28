package io.github.kabi.llama.llamaspringbootstarter.client;

/**
 * 响应监听器接口，用于异步和同步处理模型响应结果
 */
public interface ResponseListener {
    
    /**
     * 当接收到部分响应时调用（用于流式响应）
     * @param partialResponse 部分响应内容
     * @param conversationId 会话ID
     */
    void onPartialResponse(String partialResponse, String conversationId);
    
    /**
     * 当完整响应接收完成时调用
     * @param fullResponse 完整响应内容
     * @param conversationId 会话ID
     */
    void onCompleteResponse(String fullResponse, String conversationId);
    
    /**
     * 当发生错误时调用
     * @param error 错误信息
     * @param conversationId 会话ID
     */
    void onError(Exception error, String conversationId);
    
    /**
     * 当响应处理完成时调用（无论成功或失败）
     * @param conversationId 会话ID
     */
    void onFinish(String conversationId);
}