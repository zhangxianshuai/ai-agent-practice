package com.zhang.aiagentpractice;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatModel chatModel;

    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody String prompt) {
        // 1. 输入长度拦截（防爆成本/防刷）
        if (prompt.length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of("error", "输入内容过长，请控制在2000字以内"));
        }

        // 2. 构建 Prompt 并调用模型
        Prompt aiPrompt = new Prompt(prompt);
        ChatResponse response = chatModel.call(aiPrompt);

        // 3. 提取回复内容与 Token 消耗统计
        String content = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();

        // 4. 组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("reply", content);
        result.put("tokenUsage", Map.of(
                "promptTokens", usage.getPromptTokens(),
                "completionTokens", usage.getCompletionTokens(),
                "totalTokens", usage.getTotalTokens()
        ));

        // 5. 生产环境建议：在这里接入日志或计费系统
        System.out.println("本次消耗 Token: " + usage.getTotalTokens());

        return ResponseEntity.ok(result);
    }
}