package com.hsg.coffee.domain.llmparsing.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "brewlog.llm.provider", havingValue = "openrouter")
public class OpenRouterChatClient {

    @org.springframework.beans.factory.annotation.Autowired
    private com.hsg.coffee.domain.apiusage.service.ApiUsageService apiUsage;

    @Value("${brewlog.llm.openrouter.reasoning-enabled:false}")
    private boolean reasoningEnabled = false;

    @Value("${brewlog.llm.openrouter.reasoning-effort:minimal}")
    private String reasoningEffort = "minimal";
    private final String apiKey;
    private final String modelUrl;
    private final String modelId;
    private final int timeoutSeconds;
    private final int defaultMaxTokens;
    private final double temperature;
    private final String siteUrl;
    private final String appName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenRouterChatClient(
            @Value("${brewlog.llm.openrouter.api-key:${OPENROUTER_API_KEY:}}") String apiKey,
            @Value("${brewlog.llm.openrouter.model-url:https://openrouter.ai/api/v1/chat/completions}") String modelUrl,
            @Value("${brewlog.llm.openrouter.model-id:google/gemma-4-31b-it:free}") String modelId,
            @Value("${brewlog.llm.openrouter.timeout-seconds:60}") int timeoutSeconds,
            @Value("${brewlog.llm.openrouter.max-tokens:2048}") int defaultMaxTokens,
            @Value("${brewlog.llm.openrouter.temperature:0.0}") double temperature,
            @Value("${brewlog.llm.openrouter.site-url:}") String siteUrl,
            @Value("${brewlog.llm.openrouter.app-name:BLANK}") String appName,
            ObjectMapper objectMapper
    ) {
        this(
                apiKey,
                modelUrl,
                modelId,
                timeoutSeconds,
                defaultMaxTokens,
                temperature,
                siteUrl,
                appName,
                objectMapper,
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                        .build()
        );
    }

    OpenRouterChatClient(
            String apiKey,
            String modelUrl,
            String modelId,
            int timeoutSeconds,
            int defaultMaxTokens,
            double temperature,
            String siteUrl,
            String appName,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.apiKey = apiKey;
        this.modelUrl = modelUrl;
        this.modelId = modelId;
        this.timeoutSeconds = timeoutSeconds;
        this.defaultMaxTokens = defaultMaxTokens;
        this.temperature = temperature;
        this.siteUrl = siteUrl;
        this.appName = appName;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public Completion complete(String prompt) {
        return complete(prompt, defaultMaxTokens);
    }

    public Completion complete(String prompt, int maxTokens) {
        if (!isConfigured()) {
            throw new IllegalStateException(providerName() + " API 키 또는 모델 설정이 비어 있습니다.");
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", modelId.trim());
        request.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        request.put("max_tokens", maxTokens);
        request.put("temperature", temperature);
        request.put("response_format", Map.of("type", "json_object"));
        request.put("stream", false);
        configureReasoning(request);
        com.hsg.coffee.global.logging.ExtractionTrace.record("llm.request.settings",
                Map.of("provider", providerName(), "model", modelId, "maxTokens", maxTokens,
                        "reasoning", request.getOrDefault("reasoning", request.getOrDefault("reasoning_effort", "disabled"))));


        try (var permit = apiUsage == null ? null : apiUsage.reserveChat(providerName(), modelId, prompt, maxTokens)) {
            String requestBody = objectMapper.writeValueAsString(request);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(modelUrl))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (StringUtils.hasText(siteUrl)) {
                requestBuilder.header("HTTP-Referer", siteUrl.trim());
            }
            if (StringUtils.hasText(appName)) {
                requestBuilder.header("X-OpenRouter-Title", appName.trim());
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            com.hsg.coffee.global.logging.ExtractionTrace.record("llm.http.response", java.util.Map.of("provider", getClass().getSimpleName(), "status", response.statusCode(), "body", response.body()));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        providerName() + " API responded with status "
                                + response.statusCode()
                                + ": "
                                + abbreviate(response.body())
                );
            }

            if (permit != null) permit.completeChat(response.body());
            return parseCompletion(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    providerName() + " API 요청에 실패했습니다: "
                            + exception.getClass().getSimpleName()
                            + " - "
                            + exception.getMessage(),
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(providerName() + " API 요청이 중단되었습니다.", exception);
        }
    }

    protected void configureReasoning(Map<String, Object> request) {
        request.put("reasoning", reasoningEnabled
                ? Map.of("enabled", true, "effort", reasoningEffort, "exclude", true)
                : Map.of("enabled", false, "exclude", true));
    }

    protected String providerName() {
        return "OpenRouter";
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey)
                && StringUtils.hasText(modelUrl)
                && StringUtils.hasText(modelId);
    }

    public String modelUrl() {
        return modelUrl;
    }

    public String modelId() {
        return modelId;
    }

    private Completion parseCompletion(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException(providerName() + " 응답에 choices가 없습니다.");
            }

            String finish = choices.get(0).path("finish_reason").asText("");
            if ("length".equals(finish) || "max_tokens".equals(choices.get(0).path("native_finish_reason").asText())) {
                throw new IllegalStateException("AI 응답이 출력 한도에 도달해 중단되었습니다.");
            }
            JsonNode contentNode = choices.get(0).path("message").path("content");
            String content = extractContent(contentNode);
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException(providerName() + " 응답 내용이 비어 있습니다.");
            }

            return new Completion(
                    responseBody,
                    content,
                    root.path("model").asText(modelId)
            );
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(providerName() + " 응답을 해석할 수 없습니다.", exception);
        }
    }

    private String extractContent(JsonNode contentNode) {
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (!contentNode.isArray()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (JsonNode part : contentNode) {
            String text = part.path("text").asText();
            if (StringUtils.hasText(text)) {
                parts.add(text);
            }
        }
        return String.join("\n", parts);
    }

    private String abbreviate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
    }

    public record Completion(
            String rawResponse,
            String content,
            String modelId
    ) {
    }
}
