package io.github.kabi.llama.llamaspringbootstarter.listener;

import java.util.List;

import io.github.kabi.llama.llamaspringbootstarter.memory.ChatMemory;

public interface ModelListener {

    void successfully(List<String> message, ChatMemory chatMemory);

    void failed(List<String> message, ChatMemory chatMemory, Throwable throwable);

}