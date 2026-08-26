package com.zhang.aiagentpractice;

import com.zhang.aiagentpractice.tool.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatModel chatModel;
    private final ChatClient chatClient;
    private final WeatherTools weatherTools;

    // 注入 ChatClient 构建器和工具类
    public ChatController(ChatClient.Builder builder, WeatherTools weatherTools, ChatModel chatModel) {
        this.weatherTools = weatherTools;
        this.chatClient = builder.build();
        this.chatModel = chatModel;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody String prompt) {
        // 0. 输入长度拦截（防爆成本/防刷）
        if (prompt.length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of("error", "输入内容过长，请控制在2000字以内"));
        }

        // 1. 设定系统提示词（给模型立规矩）
        SystemMessage systemMessage = new SystemMessage(
                "你是一个专业的企业数据分析师。无论用户问什么，你必须且只能返回一个标准的 JSON 对象，包含 'analysis' 和 'sentiment' 两个字段。不要输出任何多余的解释文字。"
        );

        // 2. 构建包含系统提示词和用户提示词的 Prompt（将用户问题包装成 Prompt）
        UserMessage userMessage = new UserMessage(prompt);
        Prompt aiPrompt = new Prompt(List.of(systemMessage, userMessage));
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

    //调用工具
    @PostMapping("/toolchat")
    public String toolchat(@RequestBody String prompt) {
        // 核心：通过 .tools() 方法将工具注入给大模型
        return chatClient.prompt()
                .user(prompt)
                .tools(weatherTools) // <--- 赋予大模型调用工具的能力
                .call()
                .content();
    }

    // 流式输出
    @GetMapping("/stream-toolchat")
    public Flux<String> streamChat(@RequestParam String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .tools(weatherTools) // 依然保留工具调用能力
                .stream()            // <--- 核心：改为 stream() 流式调用
                .content();          // 只提取文本内容，以流的形式返回
    }
}