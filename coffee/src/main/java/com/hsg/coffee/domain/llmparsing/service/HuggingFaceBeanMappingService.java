package com.hsg.coffee.domain.llmparsing.service;

import java.time.Duration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.hsg.coffee.domain.llmparsing.dto.LlmParsingDebugResponse;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;
import com.hsg.coffee.domain.llmparsing.dto.OcrPreprocessResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "brewlog.llm.provider", havingValue = "huggingface")
public class HuggingFaceBeanMappingService implements BeanMappingService {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceBeanMappingService.class);
    private static final String DEFAULT_MODEL_URL = "https://router.huggingface.co/v1/chat/completions";

    private final String apiKey;
    private final String modelUrl;
    private final String modelId;
    private final int timeoutSeconds;
    private final int maxNewTokens;
    private final double temperature;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final BeanOcrMappingValidator validator;
    private final BeanMappingPromptFactory promptFactory;

    public HuggingFaceBeanMappingService(
            @Value("${huggingface.api-key:${brewlog.llm.huggingface.api-key:}}") String apiKey,
            @Value("${huggingface.model-url:${brewlog.llm.huggingface.model-url:" + DEFAULT_MODEL_URL + "}}") String modelUrl,
            @Value("${huggingface.model-id:${brewlog.llm.huggingface.model-id:}}") String modelId,
            @Value("${huggingface.timeout-seconds:${brewlog.llm.huggingface.timeout-seconds:15}}") int timeoutSeconds,
            @Value("${huggingface.max-new-tokens:${brewlog.llm.huggingface.max-new-tokens:700}}") int maxNewTokens,
            @Value("${huggingface.temperature:${brewlog.llm.huggingface.temperature:0.1}}") double temperature,
            ObjectMapper objectMapper,
            BeanOcrMappingValidator validator,
            BeanMappingPromptFactory promptFactory
    ) {
        this.apiKey = apiKey;
        this.modelUrl = normalizeModelUrl(modelUrl);
        this.modelId = resolveModelId(modelId, modelUrl);
        this.timeoutSeconds = timeoutSeconds;
        this.maxNewTokens = maxNewTokens;
        this.temperature = temperature;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.promptFactory = promptFactory;
    }

    @Override
    public LlmParsingResponse parseOcrText(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return LlmParsingResponse.empty();
        }

        if ((requiresApiKey() && (apiKey == null || apiKey.isBlank()))
                || modelUrl == null || modelUrl.isBlank() || modelId.isBlank()) {
            log.warn("Hugging Face 설정이 비어 있어 LLM 매핑을 건너뜁니다.");
            return LlmParsingResponse.empty();
        }

        try {
            OcrPreprocessResult preprocessResult = promptFactory.preprocess(ocrText);
            String response = callHuggingFace(preprocessResult);
            String generatedText = extractGeneratedText(response);
            String json = extractJson(generatedText);
            if (json.isBlank()) {
                log.warn("Hugging Face 응답에서 JSON을 찾지 못했습니다. response={}", abbreviate(response));
                return LlmParsingResponse.empty();
            }

            LlmParsingResponse parsed = objectMapper.readValue(json, LlmParsingResponse.class);
            return validator.sanitize(parsed, preprocessResult);
        } catch (HuggingFaceApiException exception) {
            log.warn("Hugging Face API 호출 실패: status={}, body={}",
                    exception.getStatusCode(),
                    abbreviate(exception.getResponseBody()));
            return LlmParsingResponse.empty();
        } catch (RuntimeException exception) {
            log.warn("Hugging Face OCR 매핑 처리 실패: {}", exception.getMessage());
            return LlmParsingResponse.empty();
        } catch (Exception exception) {
            log.warn("Hugging Face OCR 매핑 JSON 파싱 실패: {}", exception.getMessage());
            return LlmParsingResponse.empty();
        }
    }

    @Override
    public LlmParsingDebugResponse debugParseOcrText(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return new LlmParsingDebugResponse(
                    modelUrl,
                    modelId,
                    "SKIPPED",
                    "ocrText is blank",
                    "",
                    "",
                    "",
                    LlmParsingResponse.empty()
            );
        }

        if ((requiresApiKey() && (apiKey == null || apiKey.isBlank()))
                || modelUrl == null || modelUrl.isBlank() || modelId.isBlank()) {
            return new LlmParsingDebugResponse(
                    modelUrl,
                    modelId,
                    "SKIPPED",
                    "Hugging Face configuration is empty",
                    "",
                    "",
                    "",
                    LlmParsingResponse.empty()
            );
        }

        try {
            OcrPreprocessResult preprocessResult = promptFactory.preprocess(ocrText);
            String response = callHuggingFace(preprocessResult);
            String generatedText = extractGeneratedText(response);
            String json = extractJson(generatedText);
            LlmParsingResponse parsedResponse = parseAndSanitize(json, preprocessResult);

            return new LlmParsingDebugResponse(
                    modelUrl,
                    modelId,
                    "OK",
                    "",
                    response,
                    generatedText,
                    json,
                    parsedResponse
            );
        } catch (HuggingFaceApiException exception) {
            return new LlmParsingDebugResponse(
                    modelUrl,
                    modelId,
                    String.valueOf(exception.getStatusCode()),
                    abbreviate(exception.getResponseBody()),
                    exception.getResponseBody(),
                    "",
                    "",
                    LlmParsingResponse.empty()
            );
        } catch (RuntimeException exception) {
            return new LlmParsingDebugResponse(
                    modelUrl,
                    modelId,
                    "ERROR",
                    exception.getMessage(),
                    "",
                    "",
                    "",
                    LlmParsingResponse.empty()
            );
        } catch (Exception exception) {
            return new LlmParsingDebugResponse(
                    modelUrl,
                    modelId,
                    "ERROR",
                    exception.getMessage(),
                    "",
                    "",
                    "",
                    LlmParsingResponse.empty()
            );
        }
    }

    private String callHuggingFace(String ocrText) {
        return callHuggingFace(modelId, createPrompt(ocrText));
    }

    private String callHuggingFace(OcrPreprocessResult preprocessResult) {
        return callHuggingFace(modelId, promptFactory.createPrompt(preprocessResult));
    }

    private String callHuggingFace(String targetModelId, Object content) {
        Map<String, Object> request = Map.of(
                "model", targetModelId,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", content
                        )
                ),
                "max_tokens", maxNewTokens,
                "temperature", temperature,
                "stream", false
        );

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(modelUrl))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (!isLocalModelUrl() && apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new HuggingFaceApiException(response.statusCode(), response.body());
            }

            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("Hugging Face API 요청에 실패했습니다: "
                    + exception.getClass().getSimpleName()
                    + " - "
                    + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Hugging Face API 요청이 중단되었습니다.", exception);
        }
    }

    private LlmParsingResponse parseAndSanitize(String json, OcrPreprocessResult preprocessResult) throws Exception {
        if (json == null || json.isBlank()) {
            return LlmParsingResponse.empty();
        }

        LlmParsingResponse parsed = objectMapper.readValue(json, LlmParsingResponse.class);
        return validator.sanitize(parsed, preprocessResult);
    }

    private String createPrompt(String ocrText) {
        return promptFactory.createPrompt(ocrText);
    }

    private String extractGeneratedText(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.isArray() && !root.isEmpty() && root.get(0).has("generated_text")) {
                return root.get(0).get("generated_text").asText();
            }
            if (root.isObject() && root.has("generated_text")) {
                return root.get("generated_text").asText();
            }
            if (root.isObject() && root.has("choices") && root.get("choices").isArray()
                    && !root.get("choices").isEmpty()) {
                JsonNode firstChoice = root.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
                if (firstChoice.has("text")) {
                    return firstChoice.get("text").asText();
                }
            }
        } catch (Exception ignored) {
            return response;
        }

        return response;
    }

    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }

        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start == -1 || end == -1 || start > end) {
            return "";
        }

        return response.substring(start, end + 1);
    }

    private boolean requiresApiKey() {
        return !isLocalModelUrl();
    }

    private boolean isLocalModelUrl() {
        if (modelUrl == null || modelUrl.isBlank()) {
            return false;
        }

        String normalizedUrl = modelUrl.toLowerCase();
        return normalizedUrl.contains("localhost")
                || normalizedUrl.contains("127.0.0.1")
                || normalizedUrl.contains("[::1]");
    }

    private String normalizeModelUrl(String configuredModelUrl) {
        if (configuredModelUrl == null || configuredModelUrl.isBlank()) {
            return DEFAULT_MODEL_URL;
        }
        if (configuredModelUrl.contains("api-inference.huggingface.co/models")) {
            return DEFAULT_MODEL_URL;
        }
        return configuredModelUrl;
    }

    private String resolveModelId(String configuredModelId, String configuredModelUrl) {
        if (configuredModelId != null && !configuredModelId.isBlank()) {
            return configuredModelId.trim();
        }
        if (configuredModelUrl == null || configuredModelUrl.isBlank()) {
            return "";
        }

        String marker = "/models/";
        int markerIndex = configuredModelUrl.indexOf(marker);
        if (markerIndex < 0) {
            return "";
        }

        return configuredModelUrl.substring(markerIndex + marker.length()).trim();
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
    }

    private static class HuggingFaceApiException extends RuntimeException {

        private final int statusCode;
        private final String responseBody;

        private HuggingFaceApiException(int statusCode, String responseBody) {
            super("Hugging Face API responded with status " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody == null ? "" : responseBody;
        }

        private int getStatusCode() {
            return statusCode;
        }

        private String getResponseBody() {
            return responseBody;
        }
    }
}
