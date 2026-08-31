package com.zhang.aiagentpractice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Service
public class DifyService {

    @Value("${dify.api-key}")
    private String apiKey;

    @Value("${dify.base-url}")
    private String baseUrl;

    // 1. 普通调用（等待完整结果返回）
    public String chat(String query, String user) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", new HashMap<>());
        requestBody.put("query", query);
        requestBody.put("user", user);
        requestBody.put("response_mode", "blocking"); // 阻塞模式

        return WebClient.create()
                .post()
                .uri(baseUrl + "/chat-messages")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("answer"))
                .block(); // 阻塞等待结果
    }

    // 流式调用（打字机效果）
    public Flux<String> streamChat(String query, String user) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", new HashMap<>());
        requestBody.put("query", query);
        requestBody.put("user", user);
        requestBody.put("response_mode", "streaming"); // 关键：开启流式模式

        return WebClient.create()
                .post()
                .uri(baseUrl + "/chat-messages")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                // 1. 接收 SSE 事件流
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                // 2. 过滤并提取有效数据
                .filter(sse -> sse.data() != null && !sse.data().isBlank())
                // 3. 解析 JSON，提取 answer 字段（这里简单演示，实际建议用 Jackson）
                .map(sse -> {
                    String data = sse.data();
                    // Dify 返回格式: {"event": "message", "answer": "你"}
                    if (data.contains("\"answer\"")) {
                        return data.split("\"answer\":\"")[1].split("\",\"")[0];
                    }
                    return "";
                });
    }

    // 辅助方法：解析 Dify 返回的 SSE 格式数据
    private String parseSseData(String sseData) {
        // Dify 返回格式类似: data: {"event": "message", "answer": "你好"}
        if (sseData.startsWith("data:")) {
            String json = sseData.substring(5).trim();
            // 这里可以用 Jackson 解析 JSON，为了演示，简单截取
            // 实际项目中建议使用 ObjectMapper 解析
            return json;
        }
        return "";
    }
}
