package com.zhang.aiagentpractice;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class AiTestController {

    private final ChatClient chatClient;

    public AiTestController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String msg) {
        return chatClient.prompt()
                .user(msg)
                .call()
                .content();
    }

    @GetMapping(value = "/stream/chat", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> streamChat(@RequestParam String msg) {
        return chatClient.prompt()
                .user(msg)
                .stream()
                .content();
    }

}

