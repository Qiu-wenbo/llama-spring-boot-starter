// package io.github.kabi.llama.llamaspringbootstarter.client;

// import reactor.core.publisher.Flux;
// import reactor.core.scheduler.Schedulers;
// import java.util.Map;
// import java.util.concurrent.CompletableFuture;

// /**
//  * 响应监听器使用示例，展示异步和同步响应处理的方法
//  */
// public class ResponseListenerExample {
    
//     /**
//      * 演示使用监听器进行异步响应处理
//      * @param client LlamaChatModelClient实例
//      * @param prompt 用户提示词
//      */
//     public static void demonstrateAsyncProcessing(LlamaChatModelClient client, String prompt) {
//         System.out.println("\n===== 异步响应处理演示 =====");
        
//         // 创建自定义监听器
//         DefaultResponseListener listener = new DefaultResponseListener();
        
//         // 设置监听器到客户端
//         LlamaChatModelClient configuredClient = new LlamaChatModelClient.Builder()
//                 .baseUrl((String)client.getConfiguration().get("baseUrl"))
//                 .apiKey((String)client.getConfiguration().get("apiKey"))
//                 .responseListener(listener)
//                 .enableMemory(client.getConfiguration().containsKey("memoryEnabled") && 
//                             Boolean.parseBoolean((String)client.getConfiguration().get("memoryEnabled")))
//                 .enableRAG(client.getConfiguration().containsKey("ragEnabled") && 
//                          Boolean.parseBoolean((String)client.getConfiguration().get("ragEnabled")))
//                 .build();
        
//         // 生成会话ID
//         String conversationId = "example-sync-" + System.currentTimeMillis();
        
//         System.out.println("发起同步请求，监听器将异步处理响应...");
        
//         // 发起同步请求（监听器在后台异步处理）
//         String response = configuredClient.chat(conversationId, prompt);
        
//         System.out.println("\n同步响应已返回，内容长度: " + response.length() + " 字符");
//         System.out.println("监听器可能仍在异步处理中...");
        
//         // 检查监听器收集的部分响应
//         System.out.println("\n监听器收集的部分响应数量: " + listener.getPartialResponses(conversationId).size());
        
//         // 演示如何从监听器获取完整响应
//         try {
//             String completeResponseFromListener = listener.getCompleteResponseSync(conversationId, 1000);
//             System.out.println("从监听器获取的完整响应长度: " + completeResponseFromListener.length() + " 字符");
//         } catch (Exception e) {
//             System.err.println("获取监听器响应失败: " + e.getMessage());
//         }
        
//         // 清理会话数据
//         listener.cleanupConversation(conversationId);
//     }
    
//     /**
//      * 演示使用监听器处理流式响应
//      * @param client LlamaChatModelClient实例
//      * @param prompt 用户提示词
//      */
//     public static void demonstrateStreamingWithListener(LlamaChatModelClient client, String prompt) {
//         System.out.println("\n===== 流式响应与监听器演示 =====");
        
//         // 创建自定义监听器
//         DefaultResponseListener listener = new DefaultResponseListener();
        
//         // 设置监听器到客户端
//         LlamaChatModelClient configuredClient = new LlamaChatModelClient.Builder()
//                 .baseUrl((String)client.getConfiguration().get("baseUrl"))
//                 .apiKey((String)client.getConfiguration().get("apiKey"))
//                 .responseListener(listener)
//                 .enableMemory(client.getConfiguration().containsKey("memoryEnabled") && 
//                             Boolean.parseBoolean((String)client.getConfiguration().get("memoryEnabled")))
//                 .enableRAG(client.getConfiguration().containsKey("ragEnabled") && 
//                          Boolean.parseBoolean((String)client.getConfiguration().get("ragEnabled")))
//                 .build();
        
//         // 生成会话ID
//         String conversationId = "example-stream-" + System.currentTimeMillis();
        
//         // 在单独的线程中处理流式响应
//         CompletableFuture.runAsync(() -> {
//             try {
//                 // 发起流式请求
//                 Flux<String> flux = configuredClient.streamChat(conversationId, prompt);
                
//                 // 处理流式响应
//                 flux.subscribe(
//                     part -> System.out.println("\n流接收: " + part.substring(0, Math.min(50, part.length())) + (part.length() > 50 ? "..." : "")),
//                     error -> System.err.println("流式处理错误: " + error.getMessage()),
//                     () -> System.out.println("\n流式处理完成")
//                 );
                
//                 // 等待流处理完成
//                 Thread.sleep(3000);
                
//             } catch (Exception e) {
//                 System.err.println("处理流式请求失败: " + e.getMessage());
//             }
//         });
        
//         // 主线程演示从监听器同步获取完整响应
//         try {
//             System.out.println("\n等待监听器收集完整响应...");
//             String completeResponse = listener.getCompleteResponseSync(conversationId, 5000);
//             System.out.println("\n从监听器同步获取的完整响应:");
//             System.out.println(completeResponse);
            
//             // 显示监听器收集的部分响应数量
//             System.out.println("\n监听器收集的部分响应数量: " + listener.getPartialResponses(conversationId).size());
            
//         } catch (Exception e) {
//             System.err.println("从监听器获取响应失败: " + e.getMessage());
//         } finally {
//             // 清理会话数据
//             listener.cleanupConversation(conversationId);
//         }
//     }
    
//     /**
//      * 演示RAG模式下的异步和同步处理
//      * @param client LlamaChatModelClient实例
//      * @param prompt 用户提示词
//      * @param knowledgeBases 知识库列表
//      */
//     public static void demonstrateRAGProcessing(LlamaChatModelClient client, String prompt, String... knowledgeBases) {
//         System.out.println("\n===== RAG模式响应处理演示 =====");
        
//         // 确保启用RAG
//         if (!client.getConfiguration().containsKey("ragEnabled") || 
//             !Boolean.parseBoolean((String)client.getConfiguration().get("ragEnabled"))) {
//             System.out.println("客户端未启用RAG功能，跳过演示");
//             return;
//         }
        
//         // 创建自定义监听器
//         DefaultResponseListener listener = new DefaultResponseListener();
        
//         // 设置监听器到客户端
//         LlamaChatModelClient configuredClient = new LlamaChatModelClient.Builder()
//                 .baseUrl((String)client.getConfiguration().get("baseUrl"))
//                 .apiKey((String)client.getConfiguration().get("apiKey"))
//                 .responseListener(listener)
//                 .enableMemory(client.getConfiguration().containsKey("memoryEnabled") && 
//                             Boolean.parseBoolean((String)client.getConfiguration().get("memoryEnabled")))
//                 .enableRAG(true)
//                 .build();
        
//         // 执行RAG同步请求
//         String ragResponse = configuredClient.ragChat(prompt, knowledgeBases);
        
//         // 演示如何从监听器异步存储中获取相同的响应
        // 注意：这里应该使用实际的会话ID，而不是尝试从监听器中获取
        // String conversationId = "rag-test-" + System.currentTimeMillis();
//                 .findFirst()
//                 .orElse(null);
        
//         if (conversationId != null) {
//             try {
//                 String listenerResponse = listener.getCompleteResponseSync(conversationId, 1000);
//                 System.out.println("\n同步返回的RAG响应长度: " + ragResponse.length() + " 字符");
//                 System.out.println("监听器存储的RAG响应长度: " + listenerResponse.length() + " 字符");
                
//                 // 清理会话数据
//                 listener.cleanupConversation(conversationId);
//             } catch (Exception e) {
//                 System.err.println("获取监听器RAG响应失败: " + e.getMessage());
//             }
//         }
//     }
    
//     /**
//      * 演示错误处理场景
//      * @param client LlamaChatModelClient实例
//      */
//     public static void demonstrateErrorHandling(LlamaChatModelClient client) {
//         System.out.println("\n===== 错误处理演示 =====");
        
//         // 创建自定义监听器
//         DefaultResponseListener listener = new DefaultResponseListener();
        
//         // 设置监听器到客户端
//         LlamaChatModelClient configuredClient = new LlamaChatModelClient.Builder()
//                 .baseUrl((String)client.getConfiguration().get("baseUrl"))
//                 .apiKey((String)client.getConfiguration().get("apiKey"))
//                 .responseListener(listener)
//                 .build();
        
//         // 尝试在未启用内存的情况下使用会话功能，这将触发错误
//         try {
//             configuredClient.chat("error-test", "测试错误处理");
//         } catch (Exception e) {
//             System.out.println("\n预期的错误已触发: " + e.getMessage());
//         }
        
//         // 检查监听器是否捕获了错误
//         String conversationId = "error-test"; // 注意：这可能不是实际的conversationId
//         if (listener.getError(conversationId) != null) {
//             System.out.println("监听器成功捕获了错误: " + listener.getError(conversationId).getMessage());
//         } else {
//             System.out.println("监听器未捕获到预期的错误");
//         }
//     }
    
//     /**
//      * 辅助方法，获取DefaultResponseListener的内部状态
//      */
//     public static Map<String, ?> getListenerState(DefaultResponseListener listener) {
//         // 反射获取内部状态，实际使用时可能需要适当的getter方法
//         Map<String, Object> state = new java.util.HashMap<>();
//         try {
//             java.lang.reflect.Field partialField = listener.getClass().getDeclaredField("partialResponses");
//             partialField.setAccessible(true);
//             Map<String, ?> partialResponses = (Map<String, ?>) partialField.get(listener);
//             state.put("partialResponsesCount", partialResponses.size());
            
//             java.lang.reflect.Field completeField = listener.getClass().getDeclaredField("completeResponses");
//             completeField.setAccessible(true);
//             Map<String, ?> completeResponses = (Map<String, ?>) completeField.get(listener);
//             state.put("completeResponsesCount", completeResponses.size());
            
//             java.lang.reflect.Field errorField = listener.getClass().getDeclaredField("errors");
//             errorField.setAccessible(true);
//             Map<String, ?> errors = (Map<String, ?>) errorField.get(listener);
//             state.put("errorsCount", errors.size());
            
//         } catch (Exception e) {
//             state.put("error", e.getMessage());
//         }
//         return state;
//     }
// }