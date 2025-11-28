package io.github.kabi.llama.llamaspringbootstarter.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * 默认的响应监听器实现，提供异步和同步响应处理能力
 */
public class DefaultResponseListener implements ResponseListener {
    
    // 存储每个会话的部分响应
    private final Map<String, CopyOnWriteArrayList<String>> partialResponses = new ConcurrentHashMap<>();
    
    // 存储每个会话的完整响应
    private final Map<String, String> completeResponses = new ConcurrentHashMap<>();
    
    // 存储每个会话的完成状态
    private final Map<String, CompletableFuture<String>> responseFutures = new ConcurrentHashMap<>();
    
    // 存储每个会话的错误信息
    private final Map<String, Exception> errors = new ConcurrentHashMap<>();
    
    @Override
    public void onPartialResponse(String partialResponse, String conversationId) {
        // 存储部分响应
        partialResponses.computeIfAbsent(conversationId, k -> new CopyOnWriteArrayList<>())
                        .add(partialResponse);
        
        System.out.println("[异步存储] 收到部分响应 - 会话ID: " + conversationId + ", 内容: " + partialResponse.substring(0, Math.min(100, partialResponse.length())) + (partialResponse.length() > 100 ? "..." : ""));
    }
    
    @Override
    public void onCompleteResponse(String completeResponse, String conversationId) {
        // 存储完整响应
        completeResponses.put(conversationId, completeResponse);
        
        // 如果有对应的Future，完成它
        if (responseFutures.containsKey(conversationId)) {
            responseFutures.get(conversationId).complete(completeResponse);
        }
        
        System.out.println("[异步存储] 收到完整响应 - 会话ID: " + conversationId + ", 长度: " + completeResponse.length() + " 字符");
    }
    
    @Override
    public void onError(Exception e, String conversationId) {
        // 存储错误信息
        errors.put(conversationId, e);
        
        // 如果有对应的Future，完成它
        if (responseFutures.containsKey(conversationId)) {
            responseFutures.get(conversationId).completeExceptionally(e);
        }
        
        System.err.println("[异步存储] 处理出错 - 会话ID: " + conversationId + ", 错误: " + e.getMessage());
    }
    
    @Override
    public void onFinish(String conversationId) {
        System.out.println("[异步存储] 会话完成 - 会话ID: " + conversationId);
    }
    
    // ============ 同步获取方法 ============
    
    /**
     * 同步获取完整响应（如果尚未完成则等待）
     * @param conversationId 会话ID
     * @param timeoutMillis 超时时间（毫秒）
     * @return 完整响应
     * @throws Exception 处理过程中的异常
     */
    public String getCompleteResponseSync(String conversationId, long timeoutMillis) throws Exception {
        // 检查是否已有错误
        if (errors.containsKey(conversationId)) {
            throw errors.get(conversationId);
        }
        
        // 检查是否已有完整响应
        if (completeResponses.containsKey(conversationId)) {
            return completeResponses.get(conversationId);
        }
        
        // 创建或获取Future并等待
        CompletableFuture<String> future = responseFutures.computeIfAbsent(
            conversationId, k -> new CompletableFuture<>());
        
        try {
            return future.get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new RuntimeException("获取响应超时: " + timeoutMillis + "ms", e);
        }
    }
    
    /**
     * 获取部分响应列表
     * @param conversationId 会话ID
     * @return 部分响应列表
     */
    public java.util.List<String> getPartialResponses(String conversationId) {
        return partialResponses.getOrDefault(conversationId, new CopyOnWriteArrayList<>());
    }
    
    /**
     * 获取错误信息
     * @param conversationId 会话ID
     * @return 错误信息，如果没有则返回null
     */
    public Exception getError(String conversationId) {
        return errors.get(conversationId);
    }
    
    /**
     * 检查会话是否已完成
     * @param conversationId 会话ID
     * @return 是否完成
     */
    public boolean isConversationCompleted(String conversationId) {
        return completeResponses.containsKey(conversationId) || errors.containsKey(conversationId);
    }
    
    /**
     * 清理会话数据
     * @param conversationId 会话ID
     */
    public void cleanupConversation(String conversationId) {
        partialResponses.remove(conversationId);
        completeResponses.remove(conversationId);
        responseFutures.remove(conversationId);
        errors.remove(conversationId);
        System.out.println("[数据清理] 已清理会话数据 - 会话ID: " + conversationId);
    }
}