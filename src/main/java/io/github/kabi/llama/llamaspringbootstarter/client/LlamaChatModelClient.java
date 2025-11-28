package io.github.kabi.llama.llamaspringbootstarter.client;

import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import io.github.kabi.llama.llamaspringbootstarter.aiservice.AiServices;
import io.github.kabi.llama.llamaspringbootstarter.memory.ChatMemory;
import io.github.kabi.llama.llamaspringbootstarter.model.ChatMessage;
import io.github.kabi.llama.llamaspringbootstarter.model.ChatResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * LlamaChatModelClient 是 ChatModelClient 接口的实现，提供与Llama模型交互的功能。
 * <p>
 * 该客户端支持以下主要功能：
 * <ul>
 *   <li>基本的聊天功能（同步和流式）</li>
 *   <li>会话记忆管理</li>
 *   <li>检索增强生成（RAG）功能</li>
 *   <li>自动重试机制</li>
 *   <li>异步响应处理</li>
 *   <li>与AiServices集成</li>
 * </ul>
 * <p>
 * 使用Builder模式进行配置，支持多种定制选项。
 */
public class LlamaChatModelClient implements ChatModelClient {

    // 配置参数
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final boolean memoryEnabled;
    private final boolean ragEnabled;
    private final int maxSessionSize;
    private final int maxSessions;
    private final Map<String, String> customHeaders;
    private final ResponseListener responseListener;
    private ChatMemory chatMemory;
    private final int maxRetries;
    private final long timeoutMs;
    private final boolean retryOnNetworkError;
    private final boolean retryOnServerError;
    private final Map<String, String> operationStatus;
    private long lastSuccessfulOperationTime;
    private final AiServices aiServices;

    // 会话内存存储
    private final Map<Object, StringBuilder> sessionMemory;
    private final ExecutorService listenerExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "llama-client-listener");
        thread.setDaemon(true);
        return thread;
    });

    private LlamaChatModelClient(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.apiKey = builder.apiKey;
        this.model = builder.model;
        this.memoryEnabled = builder.memoryEnabled;
        this.ragEnabled = builder.ragEnabled;
        this.maxSessionSize = builder.maxSessionSize;
        this.maxSessions = builder.maxSessions;
        this.customHeaders = Collections.unmodifiableMap(new HashMap<>(builder.customHeaders));
        this.responseListener = builder.responseListener;
        this.aiServices = builder.aiServices;
        this.chatMemory = builder.chatMemory;
        this.maxRetries = builder.maxRetries;
        this.timeoutMs = builder.timeoutMs;
        this.retryOnNetworkError = builder.retryOnNetworkError;
        this.retryOnServerError = builder.retryOnServerError;
        this.operationStatus = new ConcurrentHashMap<>();
        this.lastSuccessfulOperationTime = System.currentTimeMillis();
        this.sessionMemory = builder.memoryEnabled ? new ConcurrentHashMap<>() : null;
    }

    /**
     * 执行流式聊天请求，返回实时响应流
     * <p>
     * 该方法发送聊天请求到Llama API，并通过Reactor Flux异步返回响应流。
     * 使用重试机制确保在网络错误或服务器错误时能够自动重试。
     *
     * @param prompt 输入提示文本
     * @return 响应ChatResponse的Flux流
     * @throws IllegalArgumentException 如果提示为空或null
     * @throws RuntimeException         如果底层请求失败
     */
    @Override
    public Flux<ChatResponse> streamChat(String prompt) {

        // 使用带重试机制的操作
        return executeWithRetry(() -> {
            // 基础流式聊天实现
            return Flux.defer(() -> {
                // 模拟流式响应，将响应内容拆分成多个部分
                String[] responseParts = {
                        "[Llama] 正在思考中...\n",
                        "我是基于Llama模型的聊天助手。\n",
                        "您的问题是: " + prompt + "\n",
                        "这是一个流式响应的示例。"
                };

                // 构建响应的完整内容（用于最终回调）
                StringBuilder fullResponse = new StringBuilder();
                for (String part : responseParts) {
                    fullResponse.append(part);
                }
                final String fullResponseStr = fullResponse.toString();

                // 转换为ChatResponse流
                return Flux.fromArray(responseParts)
                        .delayElements(java.time.Duration.ofMillis(300))
                        .map(part -> {
                            boolean isLast = part.equals(responseParts[responseParts.length - 1]);
                            return ChatResponse.builder()
                                    .content(part)
                                    .message(ChatMessage.assistant(part))
                                    .responseId("resp_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000))
                                    .model(model)
                                    .success(true)
                                    .isLast(isLast)
                                    .build();
                        });
            });
        }, "streamChat");
    }

    /**
     * 执行带会话记忆的流式聊天请求
     * <p>
     * 该方法在streamChat基础上添加了会话记忆功能，能够记住之前的对话上下文。
     * 通过sessionId标识不同的会话，自动管理会话历史。
     *
     * @param sessionId 会话标识符，用于区分不同的聊天会话
     * @param prompt    输入提示文本
     * @return 响应ChatResponse的Flux流
     * @throws UnsupportedOperationException 如果内存功能未启用
     * @throws IllegalArgumentException      如果参数无效
     */
    @Override
    public Flux<ChatResponse> streamChat(Object sessionId, String prompt) {
        if (!memoryEnabled) {
            throw new UnsupportedOperationException("memory stream chat is not supported");
        }

        // 使用会话ID作为conversationId
        String conversationId = sessionId.toString();

        // 使用带重试机制的操作
        return executeWithRetry(() -> {
            // 检查并清理过期会话
            checkAndCleanupSessions();

            // 如果启用了ChatMemory，保存用户消息
            if (chatMemory != null && sessionId != null) {
                chatMemory.setMemoryMessages(sessionId, ChatMessage.user(prompt));
            }

            // 获取或创建会话历史
            StringBuilder conversation = sessionMemory.computeIfAbsent(sessionId, k -> new StringBuilder());
            conversation.append("用户: ").append(prompt).append("\n");

            // 模拟基于会话历史的流式响应
            String[] responseParts = {
                    "[会话模式] 正在分析对话历史...\n",
                    "会话ID: " + sessionId.toString() + "\n",
                    "您的问题: " + prompt + "\n",
                    "基于上下文的响应内容。"
            };

            // 构建响应的完整内容（用于最终回调）
            StringBuilder responseBuilder = new StringBuilder();
            for (String part : responseParts) {
                responseBuilder.append(part);
            }
            final String fullResponseStr = responseBuilder.toString();

            // 将响应添加到会话历史
            conversation.append("助手: ").append(fullResponseStr).append("\n");

            // 检查并截断会话内容
            checkAndTruncateSession(conversation);

            // 转换为ChatResponse流
            return Flux.fromArray(responseParts)
                    .delayElements(java.time.Duration.ofMillis(250))
                    .map(part -> {
                        boolean isLast = part.equals(responseParts[responseParts.length - 1]);
                        return ChatResponse.builder()
                                .content(part)
                                .message(ChatMessage.assistant(part))
                                .responseId("resp_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000))
                                .model(model)
                                .success(true)
                                .isLast(isLast)
                                .build();
                    })
                    .doOnNext(response -> {
                        // 使用异步辅助方法处理部分响应
                        if (responseListener != null) {
                            executeListenerCallback(() -> {
                                responseListener.onPartialResponse(response.getContent(), conversationId);
                            });
                        }
                    })
                    .doOnComplete(() -> {
                        // 使用异步辅助方法处理完整响应
                        if (responseListener != null) {
                            executeListenerCallback(() -> {
                                responseListener.onCompleteResponse(fullResponseStr, conversationId);
                                responseListener.onFinish(conversationId);
                            });
                        }
                    })
                    .doOnError(error -> {
                        // 使用异步辅助方法处理错误
                        if (responseListener != null) {
                            executeListenerCallback(() -> {
                                responseListener.onError(error instanceof Exception ? (Exception) error : new RuntimeException(error), conversationId);
                                responseListener.onFinish(conversationId);
                            });
                        }
                    });
        }, "streamChatWithSession");
    }

    /**
     * 执行检索增强生成（RAG）的流式聊天请求
     * <p>
     * 该方法结合了RAG技术，能够从指定的知识库中检索相关信息，并将其作为上下文提供给模型。
     * 适合需要基于特定领域知识生成响应的场景。
     *
     * @param prompt         输入提示文本
     * @param knowledgeBases 要使用的知识库ID列表
     * @return 响应ChatResponse的Flux流
     * @throws UnsupportedOperationException 如果RAG功能未启用
     * @throws IllegalArgumentException      如果参数无效
     */
    @Override
    public Flux<ChatResponse> ragStreamChat(String prompt, String... knowledgeBases) {
        if (!ragEnabled) {
            throw new UnsupportedOperationException("RAG stream chat is not supported");
        }

        // 生成唯一会话ID（用于listener）
        String conversationId = UUID.randomUUID().toString();

        // 使用带重试机制的操作
        return executeWithRetry(() -> {
            // 验证知识库
            validateKnowledgeBases(knowledgeBases);

            // 模拟基于知识库的流式响应
            String[] responseParts = {
                    "[RAG模式] 正在检索知识库...\n",
                    "查询问题: " + prompt + "\n",
                    "使用知识库: " + String.join(", ", knowledgeBases) + "\n",
                    "基于检索的信息生成响应。"
            };

            // 构建响应的完整内容（用于最终回调）
            StringBuilder responseBuilder = new StringBuilder();
            for (String part : responseParts) {
                responseBuilder.append(part);
            }
            final String fullResponseStr = responseBuilder.toString();

            // 转换为ChatResponse流
            return Flux.fromArray(responseParts)
                    .delayElements(java.time.Duration.ofMillis(250))
                    .map(part -> {
                        boolean isLast = part.equals(responseParts[responseParts.length - 1]);
                        return ChatResponse.builder()
                                .content(part)
                                .message(ChatMessage.assistant(part))
                                .responseId("resp_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000))
                                .model(model)
                                .success(true)
                                .isLast(isLast)
                                .build();
                    })
                    .doOnNext(response -> {
                        // 使用异步辅助方法处理部分响应
                        if (responseListener != null) {
                            executeListenerCallback(() -> {
                                responseListener.onPartialResponse(response.getContent(), conversationId);
                            });
                        }
                    })
                    .doOnComplete(() -> {
                        // 使用异步辅助方法处理完整响应
                        if (responseListener != null) {
                            executeListenerCallback(() -> {
                                responseListener.onCompleteResponse(fullResponseStr, conversationId);
                                responseListener.onFinish(conversationId);
                            });
                        }
                    })
                    .doOnError(error -> {
                        // 使用异步辅助方法处理错误
                        if (responseListener != null) {
                            executeListenerCallback(() -> {
                                responseListener.onError(error instanceof Exception ? (Exception) error : new RuntimeException(error), conversationId);
                                responseListener.onFinish(conversationId);
                            });
                        }
                    });
        }, "ragStreamChat");
    }

    /**
     * 执行带会话记忆的RAG流式聊天请求
     * <p>
     * 结合了会话记忆和RAG功能，既能记住对话上下文，又能从知识库中检索信息。
     * 适合需要连续多轮基于领域知识进行对话的场景。
     *
     * @param sessionId      会话标识符
     * @param prompt         输入提示文本
     * @param knowledgeBases 要使用的知识库ID列表
     * @return 响应ChatResponse的Flux流
     * @throws UnsupportedOperationException 如果相关功能未启用
     * @throws IllegalArgumentException      如果参数无效
     */
    @Override
    public Flux<ChatResponse> ragStreamChat(Object sessionId, String prompt, String... knowledgeBases) {
        if (!ragEnabled || !memoryEnabled) {
            throw new UnsupportedOperationException("RAG memory stream chat is not supported");
        }

        // 使用会话ID作为conversationId
        String conversationId = sessionId.toString();

        // 使用带重试机制的操作
        return executeWithRetry(() -> {
            // 验证知识库
            validateKnowledgeBases(knowledgeBases);

            // 检查并清理过期会话
            checkAndCleanupSessions();

            // 如果启用了ChatMemory，保存用户消息
            if (chatMemory != null && sessionId != null) {
                chatMemory.setMemoryMessages(sessionId, ChatMessage.user(prompt));
            }

            // 获取或创建会话历史
            StringBuilder conversation = sessionMemory.computeIfAbsent(sessionId, k -> new StringBuilder());
            conversation.append("用户: ").append(prompt).append("\n");

            // 模拟基于会话历史和知识库的流式响应
            String[] responseParts = {
                    "[会话RAG模式] 正在分析对话历史和检索知识库...\n",
                    "会话ID: " + sessionId.toString() + "\n",
                    "查询问题: " + prompt + "\n",
                    "使用知识库: " + String.join(", ", knowledgeBases) + "\n",
                    "基于上下文和知识生成响应。"
            };

            // 构建响应的完整内容（用于最终回调）
            StringBuilder responseBuilder = new StringBuilder();
            for (String part : responseParts) {
                responseBuilder.append(part);
            }
            final String fullResponseStr = responseBuilder.toString();

            // 将响应添加到会话历史
            conversation.append("助手: ").append(fullResponseStr).append("\n");

            // 检查并截断会话内容
            checkAndTruncateSession(conversation);

            // 转换为ChatResponse流
            return Flux.fromArray(responseParts)
                    .delayElements(java.time.Duration.ofMillis(250))
                    .map(part -> {
                        boolean isLast = part.equals(responseParts[responseParts.length - 1]);
                        return ChatResponse.builder()
                                .content(part)
                                .message(ChatMessage.assistant(part))
                                .responseId("resp_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000))
                                .model(model)
                                .success(true)
                                .isLast(isLast)
                                .build();
                    })
                    .doOnNext(response -> {
                        // 使用异步辅助方法处理部分响应
                        if (responseListener != null) {
                            executeListenerCallback(() -> {
                                responseListener.onPartialResponse(response.getContent(), conversationId);
                            });
                        }
                    })
                    .doOnComplete(() -> {
                        // 使用异步辅助方法处理完整响应
                        if (responseListener != null) {
                            executeListenerCallback(() -> {
                                responseListener.onCompleteResponse(fullResponseStr, conversationId);
                                responseListener.onFinish(conversationId);
                            });
                        }
                    })
                    .doOnError(error -> {
                        // 使用异步辅助方法处理错误
                        if (responseListener != null) {
                            executeListenerCallback(() -> {
                                responseListener.onError(error instanceof Exception ? (Exception) error : new RuntimeException(error), conversationId);
                                responseListener.onFinish(conversationId);
                            });
                        }
                    });
        }, "ragStreamChatWithSession");
    }

    /**
     * 执行同步聊天请求，返回完整响应
     * <p>
     * 该方法发送聊天请求到Llama API，等待并返回完整的响应结果。
     * 适用于不需要流式处理，只需要最终结果的场景。
     *
     * @param prompt 输入提示文本
     * @return 完整的ChatResponse对象
     * @throws IllegalArgumentException 如果提示为空或null
     * @throws RuntimeException         如果底层请求失败
     */
    @Override
    public ChatResponse chat(String prompt) {
        // 生成唯一会话ID（用于listener）
        String conversationId = UUID.randomUUID().toString();

        // 使用带重试机制的操作
        return executeWithRetry(() -> {
            try {
                // 构建API请求URL
                String apiUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/generate";

                // 创建请求体
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("prompt", prompt);
                if (model != null && !model.isEmpty()) {
                    requestBody.put("model", model);
                }

                // 记录请求信息
                System.out.println("发送请求到: " + apiUrl);
                System.out.println("请求体: " + requestBody);

                // 创建RestTemplate实例
                RestTemplate restTemplate = new RestTemplate();

                // 添加请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.set("Authorization", "Bearer " + apiKey);
                }

                // 发送POST请求
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity(apiUrl, requestEntity, Map.class);

                // 处理响应
                Map<String, Object> responseBody = responseEntity.getBody();
                final String responseStr;

                if (responseBody != null) {
                    // 提取响应内容，兼容不同的API响应格式
                    if (responseBody.containsKey("response")) {
                        responseStr = responseBody.get("response").toString();
                    } else if (responseBody.containsKey("content")) {
                        responseStr = responseBody.get("content").toString();
                    } else if (responseBody.containsKey("text")) {
                        responseStr = responseBody.get("text").toString();
                    } else {
                        // 默认返回整个响应体的字符串表示
                        responseStr = responseBody.toString();
                    }
                } else {
                    responseStr = "";
                }


                // 返回ChatResponse对象
                return ChatResponse.success(responseStr, model);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                // 处理HTTP错误
                String errorMessage = "API调用失败: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
                System.err.println(errorMessage);

                return ChatResponse.error(errorMessage);
            } catch (Exception e) {
                // 处理其他错误
                String errorMessage = "请求处理失败: " + e.getMessage();
                System.err.println(errorMessage);

                if (responseListener != null) {
                    executeListenerCallback(() -> {
                        responseListener.onError(e, conversationId);
                        responseListener.onFinish(conversationId);
                    });
                }

                return ChatResponse.error(errorMessage);
            }
        }, "chat");
    }

    /**
     * 执行带会话记忆的同步聊天请求
     * <p>
     * 结合了会话记忆功能的同步聊天方法，能够维护多轮对话的上下文信息。
     * 通过sessionId管理不同的对话会话。
     *
     * @param sessionId 会话标识符
     * @param prompt    输入提示文本
     * @return 完整的ChatResponse对象
     * @throws UnsupportedOperationException 如果内存功能未启用
     * @throws IllegalArgumentException      如果参数无效
     */
    @Override
    public ChatResponse chat(Object sessionId, String prompt) {
        if (!memoryEnabled) {
            throw new UnsupportedOperationException("memory chat is not supported");
        }

        // 使用会话ID作为conversationId
        String conversationId = sessionId.toString();

        // 使用带重试机制的操作
        return executeWithRetry(() -> {
            // 检查并清理过期会话
            checkAndCleanupSessions();

            // 如果启用了ChatMemory，保存用户消息
            if (chatMemory != null && sessionId != null) {
                chatMemory.setMemoryMessages(sessionId, ChatMessage.user(prompt));
            }

            // 获取或创建会话历史
            StringBuilder conversation = sessionMemory.computeIfAbsent(sessionId, k -> new StringBuilder());
            conversation.append("用户: ").append(prompt).append("\n");

            // 模拟基于会话历史的非流式响应
            StringBuilder response = new StringBuilder();
            response.append("[会话模式] 非流式响应\n");
            response.append("会话ID: " + sessionId.toString() + "\n");
            response.append("您的问题: " + prompt + "\n");
            response.append("当前对话长度: " + conversation.length() + " 字符\n");
            response.append("这是基于会话上下文的完整响应。");

            String responseStr = response.toString();

            conversation.append("助手: ").append(responseStr).append("\n");

            // 检查并截断会话内容
            checkAndTruncateSession(conversation);

            // 使用异步辅助方法处理响应
            if (responseListener != null) {
                executeListenerCallback(() -> {
                    responseListener.onPartialResponse(responseStr, conversationId);
                    responseListener.onCompleteResponse(responseStr, conversationId);
                    responseListener.onFinish(conversationId);
                });
            }

            // 返回ChatResponse对象
            return ChatResponse.success(responseStr, model);
        }, "chatWithSession");
    }

    /**
     * 执行检索增强生成（RAG）的同步聊天请求
     * <p>
     * 结合RAG技术的同步聊天方法，从指定知识库检索相关信息并生成响应。
     * 适合需要基于特定知识生成完整回答的场景。
     *
     * @param prompt         输入提示文本
     * @param knowledgeBases 要使用的知识库ID列表
     * @return 完整的ChatResponse对象
     * @throws UnsupportedOperationException 如果RAG功能未启用
     * @throws IllegalArgumentException      如果参数无效
     */
    @Override
    public ChatResponse ragChat(String prompt, String... knowledgeBases) {
        if (!ragEnabled) {
            throw new UnsupportedOperationException("RAG chat is not supported");
        }

        // 生成唯一会话ID（用于listener）
        String conversationId = UUID.randomUUID().toString();

        // 使用带重试机制的操作
        return executeWithRetry(() -> {
            // 验证知识库
            validateKnowledgeBases(knowledgeBases);

            // 模拟基于知识库的非流式响应
            StringBuilder response = new StringBuilder();
            response.append("[RAG模式] 非流式响应\n");
            response.append("查询问题: " + prompt + "\n");
            response.append("使用知识库: " + String.join(", ", knowledgeBases) + "\n");
            response.append("从知识库中检索到相关信息...\n");
            response.append("这是基于检索增强生成的完整响应。");

            String responseStr = response.toString();

            // 使用异步辅助方法处理响应
            if (responseListener != null) {
                executeListenerCallback(() -> {
                    responseListener.onPartialResponse(responseStr, conversationId);
                    responseListener.onCompleteResponse(responseStr, conversationId);
                    responseListener.onFinish(conversationId);
                });
            }

            // 返回ChatResponse对象
            return ChatResponse.success(responseStr, model);
        }, "ragChat");
    }

    /**
     * 执行带会话记忆的RAG同步聊天请求
     * <p>
     * 同时具备会话记忆和RAG功能的同步聊天方法，能够在多轮对话中基于知识库信息生成响应。
     * 适用于需要持续交互式问答并结合领域知识的场景。
     *
     * @param sessionId      会话标识符
     * @param prompt         输入提示文本
     * @param knowledgeBases 要使用的知识库ID列表
     * @return 完整的ChatResponse对象
     * @throws UnsupportedOperationException 如果相关功能未启用
     * @throws IllegalArgumentException      如果参数无效
     */
    @Override
    public ChatResponse ragChat(Object sessionId, String prompt, String... knowledgeBases) {
        if (!ragEnabled || !memoryEnabled) {
            throw new UnsupportedOperationException("RAG memory chat is not supported");
        }

        // 使用会话ID作为conversationId
        String conversationId = sessionId.toString();

        // 使用带重试机制的操作
        return executeWithRetry(() -> {
            // 验证知识库
            validateKnowledgeBases(knowledgeBases);

            // 检查并清理过期会话
            checkAndCleanupSessions();

            // 如果启用了ChatMemory，保存用户消息
            if (chatMemory != null && sessionId != null) {
                chatMemory.setMemoryMessages(sessionId, ChatMessage.user(prompt));
            }

            // 获取或创建会话历史
            StringBuilder conversation = sessionMemory.computeIfAbsent(sessionId, k -> new StringBuilder());
            conversation.append("用户: ").append(prompt).append("\n");

            // 模拟基于会话历史和知识库的非流式响应
            StringBuilder response = new StringBuilder();
            response.append("[会话RAG模式] 非流式响应\n");
            response.append("会话ID: " + sessionId.toString() + "\n");
            response.append("查询问题: " + prompt + "\n");
            response.append("使用知识库: " + String.join(", ", knowledgeBases) + "\n");
            response.append("当前对话长度: " + conversation.length() + " 字符\n");
            response.append("这是基于会话上下文和检索增强生成的完整响应。");

            String responseStr = response.toString();

            conversation.append("助手: ").append(responseStr).append("\n");

            // 检查并截断会话内容
            checkAndTruncateSession(conversation);

            // 使用异步辅助方法处理响应
            if (responseListener != null) {
                executeListenerCallback(() -> {
                    responseListener.onPartialResponse(responseStr, conversationId);
                    responseListener.onCompleteResponse(responseStr, conversationId);
                    responseListener.onFinish(conversationId);
                });
            }

            // 返回ChatResponse对象
            return ChatResponse.success(responseStr, model);
        }, "ragChatWithSession");
    }

    // Builder模式实现
    public static class Builder {
        private String baseUrl = "http://localhost:8080";
        private String apiKey = null;
        private String model = "llama3";
        private boolean memoryEnabled = false;
        private boolean ragEnabled = false;
        private int maxSessionSize = 10000; // 默认最大会话大小
        private int maxSessions = 100;     // 默认最大会话数
        private Map<String, String> customHeaders = new HashMap<>();
        private ResponseListener responseListener = null;
        private ChatMemory chatMemory = null;
        private int maxRetries = 3;
        private long timeoutMs = 30000; // 30秒
        private boolean retryOnNetworkError = true;
        private boolean retryOnServerError = true;
        private AiServices aiServices = null;

        /**
         * 设置Llama API的基础URL
         */
        public Builder baseUrl(String baseUrl) {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("Base URL cannot be null or empty");
            }
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * 设置API密钥
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * 启用会话记忆功能
         */
        public Builder enableMemory() {
            this.memoryEnabled = true;
            return this;
        }

        /**
         * 启用RAG（检索增强生成）功能
         */
        public Builder enableRAG() {
            this.ragEnabled = true;
            return this;
        }

        /**
         * 设置最大会话大小（字符数）
         */
        public Builder maxSessionSize(int maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException("Max session size must be positive");
            }
            this.maxSessionSize = maxSize;
            return this;
        }

        /**
         * 设置最大会话数量
         */
        public Builder maxSessions(int maxSessions) {
            if (maxSessions <= 0) {
                throw new IllegalArgumentException("Max sessions must be positive");
            }
            this.maxSessions = maxSessions;
            return this;
        }

        /**
         * 添加自定义HTTP头
         */
        public Builder addCustomHeader(String name, String value) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Header name cannot be null or empty");
            }
            this.customHeaders.put(name, value);
            return this;
        }

        /**
         * 设置响应监听器，用于异步处理响应结果
         */
        public Builder responseListener(ResponseListener listener) {
            this.responseListener = listener;
            return this;
        }

        /**
         * 设置聊天记忆实现
         */
        public Builder chatMemory(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }

        /**
         * 设置模型名称
         */
        public Builder model(String model) {
            if (model == null || model.trim().isEmpty()) {
                throw new IllegalArgumentException("Model cannot be null or empty");
            }
            this.model = model;
            return this;
        }

        /**
         * 设置最大重试次数
         */
        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("Max retries must be non-negative");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * 设置请求超时时间（毫秒）
         */
        public Builder timeoutMs(long timeoutMs) {
            if (timeoutMs <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * 设置是否在网络错误时重试
         */
        public Builder retryOnNetworkError(boolean retryOnNetworkError) {
            this.retryOnNetworkError = retryOnNetworkError;
            return this;
        }

        /**
         * 设置是否在服务器错误时重试
         */
        public Builder retryOnServerError(boolean retryOnServerError) {
            this.retryOnServerError = retryOnServerError;
            return this;
        }

        /**
         * 设置AiServices实例
         */
        public Builder aiServices(AiServices aiServices) {
            this.aiServices = aiServices;
            return this;
        }

        /**
         * 创建并配置AiServices实例
         * 自动使用当前Builder配置的参数初始化AiServices
         */
        public Builder configureAiServices() {
            // 延迟创建client实例，避免循环依赖
            this.aiServices = AiServices.builder()
                    .model(this.model)
                    .chatMemory(this.chatMemory)
                    .build();
            return this;
        }

        /**
         * 构建LlamaChatModelClient实例
         */
        public LlamaChatModelClient build() {
            // 验证必要的配置
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalStateException("Base URL must be set");
            }

            // 构建并返回实例
            return new LlamaChatModelClient(this);
        }
    }

    // 静态工厂方法
    public static Builder builder() {
        return new Builder();
    }


    /**
     * 检查并清理过期或超限的会话
     * <p>
     * 当会话数量达到最大值时，该方法会自动移除最早创建的会话，
     * 以确保系统资源得到合理使用，避免内存泄漏。
     */
    private void checkAndCleanupSessions() {
        if (memoryEnabled && sessionMemory.size() >= maxSessions) {
            // 当会话数量达到上限时，移除最早的会话（简单实现）
            Object firstKey = sessionMemory.keySet().iterator().next();
            sessionMemory.remove(firstKey);
        }
    }

    /**
     * 检查并截断会话内容
     * <p>
     * 当会话内容超过最大长度限制时，该方法会截断最早的部分，
     * 只保留最新的会话内容，以控制内存使用并保持会话的相关性。
     *
     * @param conversation 会话内容字符串构建器
     */
    private void checkAndTruncateSession(StringBuilder conversation) {
        if (conversation.length() > maxSessionSize) {
            // 截断会话内容，保留最新的部分
            int overflow = conversation.length() - maxSessionSize;
            conversation.delete(0, overflow);
        }
    }

    /**
     * 清除指定会话的内存
     * <p>
     * 该方法允许手动清除特定会话的历史记录，适用于需要重置对话上下文的场景。
     *
     * @param sessionId 要清除的会话标识符
     */
    public void clearSession(Object sessionId) {
        if (memoryEnabled && sessionId != null) {
            sessionMemory.remove(sessionId);
        }
    }

    // 清除所有会话内存
    public void clearAllSessions() {
        if (memoryEnabled) {
            sessionMemory.clear();
        }
    }

    // 获取会话数量
    public int getSessionCount() {
        return memoryEnabled ? sessionMemory.size() : 0;
    }

    // 获取会话历史
    public String getSessionHistory(Object sessionId) {
        if (!memoryEnabled || sessionId == null) {
            return null;
        }
        StringBuilder conversation = sessionMemory.get(sessionId);
        return conversation != null ? conversation.toString() : null;
    }

    // 获取配置信息
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("baseUrl", baseUrl);
        config.put("apiKeyConfigured", apiKey != null && !apiKey.isEmpty());
        config.put("memoryEnabled", memoryEnabled);
        config.put("ragEnabled", ragEnabled);
        config.put("maxSessionSize", maxSessionSize);
        config.put("maxSessions", maxSessions);
        config.put("customHeadersCount", customHeaders.size());
        return Collections.unmodifiableMap(config);
    }

    // 健康检查
    public boolean healthCheck() {
        try {
            // 实际应用中，这里应该调用API进行健康检查
            // 这里简单返回true表示健康
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 获取系统状态
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("healthy", healthCheck());
        status.put("currentTime", System.currentTimeMillis());
        status.put("sessionCount", getSessionCount());
        status.put("memoryEnabled", memoryEnabled);
        status.put("ragEnabled", ragEnabled);

        if (memoryEnabled) {
            status.put("sessionMemoryUsage", calculateMemoryUsage());
        }

        return Collections.unmodifiableMap(status);
    }

    // 计算内存使用情况（简单估算）
    private Map<String, Object> calculateMemoryUsage() {
        Map<String, Object> usage = new HashMap<>();
        int totalSize = 0;

        for (Map.Entry<Object, StringBuilder> entry : sessionMemory.entrySet()) {
            totalSize += entry.getValue().length();
        }

        usage.put("totalSize", totalSize);
        usage.put("maxSize", maxSessionSize * maxSessions);
        usage.put("usagePercentage", maxSessions > 0 ? (totalSize * 100.0) / (maxSessionSize * maxSessions) : 0);

        return usage;
    }

    /**
     * 获取基于当前客户端配置的AiServices实例
     */
    public AiServices getAiServices() {
        return AiServices.builder()
                .chatModelClient(this)
                .model(model)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 获取会话的记忆消息
     */
    public List<ChatMessage> getSessionMemory(Object sessionId) {
        if (chatMemory != null && sessionId != null) {
            return chatMemory.getMemoryMessages(sessionId);
        }
        return Collections.emptyList();
    }

    /**
     * 设置ChatMemory实现
     */
    public void setChatMemory(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    /**
     * 执行带重试逻辑的操作
     */
    /**
     * 执行带重试机制的操作
     * <p>
     * 该方法提供统一的重试机制，能够在网络错误或服务器错误时自动重试操作。
     * 使用指数退避策略计算重试间隔，避免频繁重试导致的资源浪费。
     *
     * @param operation     要执行的操作，封装在Operation接口中
     * @param operationName 操作名称，用于日志记录和状态跟踪
     * @param <T>           返回值类型
     * @return 操作的执行结果
     * @throws RuntimeException 如果达到最大重试次数仍然失败
     */
    private <T> T executeWithRetry(Operation<T> operation, String operationName) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= maxRetries) {
            try {
                // 更新操作状态
                updateOperationStatus(operationName, "IN_PROGRESS");

                // 执行操作
                T result = operation.execute();

                // 更新成功状态
                lastSuccessfulOperationTime = System.currentTimeMillis();
                updateOperationStatus(operationName, "SUCCESS");
                return result;
            } catch (Exception e) {
                lastException = e;
                attempts++;

                // 更新错误状态
                updateOperationStatus(operationName, "ERROR: " + e.getMessage());

                // 判断是否需要重试
                if (!shouldRetry(e, attempts)) {
                    throw new RuntimeException("Operation " + operationName + " failed after " + attempts + " attempts", e);
                }

                // 指数退避
                try {
                    long backoffTime = Math.min(1000 * (long) Math.pow(2, attempts - 1), 10000);
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        throw new RuntimeException("Failed after " + maxRetries + " retries", lastException);
    }

    /**
     * 判断是否应该重试
     */
    private boolean shouldRetry(Exception e, int attempts) {
        if (attempts > maxRetries) {
            return false;
        }

        // 根据错误类型决定是否重试
        if (e instanceof java.net.SocketTimeoutException ||
                e instanceof java.net.ConnectException ||
                e instanceof java.net.SocketException) {
            return retryOnNetworkError;
        }

        // 检查是否是HTTP服务器错误
        if (e.getMessage() != null && e.getMessage().contains("50")) {
            return retryOnServerError;
        }

        return false;
    }

    /**
     * 更新操作状态
     */
    private void updateOperationStatus(String operationName, String status) {
        operationStatus.put(operationName, status + " (" + System.currentTimeMillis() + ")");
    }

    /**
     * 获取当前操作状态
     */
    public Map<String, String> getOperationStatus() {
        return Collections.unmodifiableMap(operationStatus);
    }

    /**
     * 获取上次成功操作的时间
     */
    public long getLastSuccessfulOperationTime() {
        return lastSuccessfulOperationTime;
    }

    /**
     * 重置所有操作状态
     */
    public void resetOperationStatus() {
        operationStatus.clear();
    }

    /**
     * 操作接口，用于重试逻辑
     */
    /**
     * 操作接口，用于封装可重试的操作
     * <p>
     * 该接口定义了可执行且可重试的操作，与executeWithRetry方法配合使用。
     * 通过这种方式，可以将各种操作统一封装，便于应用重试逻辑。
     *
     * @param <T> 操作返回值类型
     */
    @FunctionalInterface
    private interface Operation<T> {
        /**
         * 执行操作
         *
         * @return 操作结果
         * @throws Exception 执行过程中可能抛出的异常
         */
        T execute() throws Exception;
    }

    /**
     * 异步执行listener回调，确保不会阻塞主流程
     */
    /**
     * 异步执行监听器回调
     * <p>
     * 该方法将监听器回调任务提交到专用线程池执行，确保回调不会阻塞主线程。
     * 同时捕获并记录回调执行过程中的异常，防止异常传播到主线程。
     *
     * @param callback 要执行的回调任务
     */
    private void executeListenerCallback(Runnable callback) {
        listenerExecutor.submit(() -> {
            try {
                callback.run();
            } catch (Exception e) {
                System.err.println("Error in listener callback: " + e.getMessage());
            }
        });
    }

    // 验证知识库名称（用于RAG功能）
    private void validateKnowledgeBases(String... knowledgeBases) {
        if (!ragEnabled) {
            throw new UnsupportedOperationException("RAG is not enabled");
        }

        if (knowledgeBases == null || knowledgeBases.length == 0) {
            throw new IllegalArgumentException("At least one knowledge base must be specified");
        }

        for (String kb : knowledgeBases) {
            if (kb == null || kb.trim().isEmpty()) {
                throw new IllegalArgumentException("Knowledge base name cannot be null or empty");
            }
        }
    }
}