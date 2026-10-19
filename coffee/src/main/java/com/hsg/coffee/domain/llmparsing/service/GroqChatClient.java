package com.hsg.coffee.domain.llmparsing.service;

import java.net.http.HttpClient;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "brewlog.llm.provider", havingValue = "groq")
public class GroqChatClient extends OpenRouterChatClient {

    @Autowired
    public GroqChatClient(
            @Value("${brewlog.llm.groq.api-key:${GROQ_API_KEY:}}") String apiKey,
            @Value("${brewlog.llm.groq.model-url:https://api.groq.com/openai/v1/chat/completions}") String modelUrl,
            @Value("${brewlog.llm.groq.model-id:qwen/qwen3.8-27b}") String modelId,
            @Value("${brewlog.llm.groq.timeout-seconds:60}") int timeoutSeconds,
            @Value("${brewlog.llm.groq.max-tokens:768}") int maxTokens,
            @Value("${brewlog.llm.groq.temperature:0.0}") double temperature,
            ObjectMapper mapper
    ) {
        super(apiKey, modelUrl, modelId, timeoutSeconds, maxTokens, temperature, "", "", mapper);
    }

    GroqChatClient(String key, String url, ObjectMapper mapper, HttpClient httpClient) {
        super(key, url, "qwen/qwen3.8-27b", 5, 768, 0.0, "", "", mapper, httpClient);
    }

    @Override
    protected void configureReasoning(Map<String, Object> request) {
        request.put("reasoning_effort", "none");
    }

    @Override
    protected String providerName() {
        return "Groq";
    }
}
