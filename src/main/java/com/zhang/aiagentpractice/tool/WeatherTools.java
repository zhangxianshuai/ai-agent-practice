package com.zhang.aiagentpractice.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class WeatherTools {

    @Tool(description = "获取指定城市的当前天气信息")
    public String getWeather(String city) {
        // 这里模拟真实的后端业务逻辑（实际中你会查数据库或调第三方API）
        return String.format("""
            {
                "city": "%s",
                "temperature": "26℃",
                "weather": "暴雨",
                "humidity": "45%%"
            }
            """, city);
    }
}
