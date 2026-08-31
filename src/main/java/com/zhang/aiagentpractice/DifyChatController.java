package com.zhang.aiagentpractice;

import com.zhang.aiagentpractice.service.DifyService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/dify")
public class DifyChatController {

    private final DifyService difyService;

    public DifyChatController(DifyService difyService) {
        this.difyService = difyService;
    }

    // 普通问答接口
    @GetMapping("/chat")
    public String chat(@RequestParam String query) {
        return difyService.chat(query, "user_001");
    }

    // 流式问答接口
    @GetMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> difyStreamChat(@RequestParam String query) {
        return difyService.streamChat(query, "user_001");
    }
}
