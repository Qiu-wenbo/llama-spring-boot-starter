package io.github.kabi.llama.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class LlamaSpringBootTestApplication {

    @Autowired
    private TestAiService testAiService;

    public static void main(String[] args) {
        SpringApplication.run(LlamaSpringBootTestApplication.class, args);
    }

    @GetMapping("/test-ai")
    public String testAi(@RequestParam(defaultValue = "Hello") String prompt) {
        return testAiService.generateResponse(prompt);
    }
}