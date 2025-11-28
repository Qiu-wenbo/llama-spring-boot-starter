package io.github.kabi.llama.test;

import io.github.kabi.llama.llamaspringbootstarter.annotation.AiService;
import io.github.kabi.llama.llamaspringbootstarter.annotation.UserMessage;

@AiService
public interface TestAiService {

    @UserMessage(prompt = "请回复用户的问题: {{prompt}}")
    String generateResponse(String prompt);
}
