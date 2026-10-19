package com.hsg.coffee.domain.tasteSpace.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OpenRouterEmbeddingClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String modelUrl;
    private final String modelId;
    private final Duration timeout;

    public OpenRouterEmbeddingClient(
            ObjectMapper objectMapper,
            @Value("${brewlog.llm.openrouter.api-key:}") String apiKey,
            @Value("${brewlog.llm.embedding.model-url:https://openrouter.ai/api/v1/embeddings}") String modelUrl,
            @Value("${brewlog.llm.embedding.model-id:nvidia/nemotron-3-embed-1b:free}") String modelId,
            @Value("${brewlog.llm.embedding.timeout-seconds:90}") long timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.modelUrl = modelUrl;
        this.modelId = modelId;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    public String modelId() {
        return modelId;
    }

    public List<double[]> embed(List<String> input) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OpenRouter API 키가 설정되지 않았습니다.");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of("model", modelId, "input", input));
            HttpRequest request = HttpRequest.newBuilder(URI.create(modelUrl))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenRouter 임베딩 요청에 실패했습니다. (HTTP " + response.statusCode() + ")");
            }
            JsonNode data = objectMapper.readTree(response.body()).path("data");
            if (!data.isArray() || data.size() != input.size()) {
                throw new IllegalStateException("OpenRouter 임베딩 응답 개수가 일치하지 않습니다.");
            }
            return data.valueStream()
                    .map(item -> toVector(item.path("embedding")))
                    .toList();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenRouter 임베딩 요청이 중단되었습니다.", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("OpenRouter 임베딩 응답을 처리하지 못했습니다.", exception);
        }
    }

    private double[] toVector(JsonNode embedding) {
        double[] vector = new double[embedding.size()];
        for (int index = 0; index < embedding.size(); index++) {
            vector[index] = embedding.get(index).asDouble();
        }
        return vector;
    }
}
