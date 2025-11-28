package io.github.kabi.llama.llamaspringbootstarter.client;

import reactor.core.publisher.Flux;

public interface ChatModelClient {

    // 流式
    Flux<String> streamChat(String prompt);

    // 会话流式
    Flux<String> streamChat(Object sessionId, String prompt);

    // RAG流式
    Flux<String> ragStreamChat(String prompt, String... knowledgeBases);

    // 会话RAG流式
    Flux<String> ragStreamChat(Object sessionId, String prompt, String... knowledgeBases);

    // 非流式
    String chat(String prompt);

    // 会话非流式
    String chat(Object sessionId, String prompt);

    // RAG非流式
    String ragChat(String prompt, String... knowledgeBases);

    // 会话RAG非流式
    String ragChat(Object sessionId, String prompt, String... knowledgeBases);

}