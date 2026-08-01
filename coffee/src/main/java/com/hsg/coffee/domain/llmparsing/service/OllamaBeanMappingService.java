package com.hsg.coffee.domain.llmparsing.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
@ConditionalOnProperty(name = "brewlog.llm.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaBeanMappingService implements BeanMappingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaBeanMappingService.class);

    private final String modelUrl;
    private final String modelId;
    private final int timeoutSeconds;
    private final int numPredict;
    private final double temperature;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final BeanOcrMappingValidator validator;
    private final BeanMappingPromptFactory promptFactory;

    public OllamaBeanMappingService(
            @Value("${brewlog.llm.ollama.model-url:http://127.0.0.1:11434/api/generate}") String modelUrl,
            @Value("${brewlog.llm.ollama.model-id:gemma3:12b}") String modelId,
            @Value("${brewlog.llm.ollama.timeout-seconds:120}") int timeoutSeconds,
            @Value("${brewlog.llm.ollama.num-predict:700}") int numPredict,
            @Value("${brewlog.llm.ollama.temperature:0.0}") double temperature,
            ObjectMapper objectMapper,
            BeanOcrMappingValidator validator,
            BeanMappingPromptFactory promptFactory
    ) {
        this.modelUrl = modelUrl;
        this.modelId = modelId;
        this.timeoutSeconds = timeoutSeconds;
        this.numPredict = numPredict;
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
        if (modelUrl == null || modelUrl.isBlank() || modelId == null || modelId.isBlank()) {
            log.warn("Ollama 설정이 비어 있어 LLM 매핑을 건너뜁니다.");
            return LlmParsingResponse.empty();
        }

        try {
            OcrPreprocessResult preprocessResult = promptFactory.preprocess(ocrText);
            String response = callOllama(promptFactory.createPrompt(preprocessResult));
            String generatedText = extractGeneratedText(response);
            String json = extractJson(generatedText);
            if (json.isBlank()) {
                log.warn("Ollama 응답에서 JSON을 찾지 못했습니다. response={}", abbreviate(response));
                return LlmParsingResponse.empty();
            }

            LlmParsingResponse parsed = objectMapper.readValue(json, LlmParsingResponse.class);
            return validator.sanitize(parsed, preprocessResult);
        } catch (RuntimeException exception) {
            log.warn("Ollama OCR 매핑 처리 실패: {}", exception.getMessage());
            return LlmParsingResponse.empty();
        } catch (Exception exception) {
            log.warn("Ollama OCR 매핑 JSON 파싱 실패: {}", exception.getMessage());
            return LlmParsingResponse.empty();
        }
    }

    @Override
    public LlmParsingDebugResponse debugParseOcrText(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return new LlmParsingDebugResponse(modelUrl, modelId, "SKIPPED", "ocrText is blank", "", "", "", LlmParsingResponse.empty());
        }
        if (modelUrl == null || modelUrl.isBlank() || modelId == null || modelId.isBlank()) {
            return new LlmParsingDebugResponse(modelUrl, modelId, "SKIPPED", "Ollama configuration is empty", "", "", "", LlmParsingResponse.empty());
        }

        try {
            OcrPreprocessResult preprocessResult = promptFactory.preprocess(ocrText);
            String response = callOllama(promptFactory.createPrompt(preprocessResult));
            String generatedText = extractGeneratedText(response);
            String json = extractJson(generatedText);
            LlmParsingResponse parsedResponse = parseAndSanitize(json, preprocessResult);
            return new LlmParsingDebugResponse(modelUrl, modelId, "OK", "", response, generatedText, json, parsedResponse);
        } catch (RuntimeException exception) {
            return new LlmParsingDebugResponse(modelUrl, modelId, "ERROR", exception.getMessage(), "", "", "", LlmParsingResponse.empty());
        } catch (Exception exception) {
            return new LlmParsingDebugResponse(modelUrl, modelId, "ERROR", exception.getMessage(), "", "", "", LlmParsingResponse.empty());
        }
    }

    private String callOllama(String prompt) {
        Map<String, Object> request = Map.of(
                "model", modelId,
                "prompt", prompt,
                "stream", false,
                "format", jsonSchema(),
                "options", Map.of(
                        "temperature", temperature,
                        "num_predict", numPredict
                )
        );

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(modelUrl))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama API responded with status "
                        + response.statusCode()
                        + ": "
                        + abbreviate(response.body()));
            }
            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("Ollama API 요청에 실패했습니다: "
                    + exception.getClass().getSimpleName()
                    + " - "
                    + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama API 요청이 중단되었습니다.", exception);
        }
    }

    private LlmParsingResponse parseAndSanitize(String json, OcrPreprocessResult preprocessResult) throws Exception {
        if (json == null || json.isBlank()) {
            return LlmParsingResponse.empty();
        }
        LlmParsingResponse parsed = objectMapper.readValue(json, LlmParsingResponse.class);
        return validator.sanitize(parsed, preprocessResult);
    }

    private Map<String, Object> jsonSchema() {
        Map<String, Object> stringField = Map.of("type", "string");
        return Map.of(
                "type", "object",
                "properties", Map.ofEntries(
                        Map.entry("name", stringField),
                        Map.entry("roastery", stringField),
                        Map.entry("originCountry", stringField),
                        Map.entry("region", stringField),
                        Map.entry("farmOrStation", stringField),
                        Map.entry("variety", stringField),
                        Map.entry("altitude", stringField),
                        Map.entry("process", stringField),
                        Map.entry("beanStatus", stringField),
                        Map.entry("roastedAt", stringField),
                        Map.entry("purchasedAt", stringField),
                        Map.entry("price", stringField),
                        Map.entry("remainingWeightGram", stringField),
                        Map.entry("flavorNotes", Map.of(
                                "type", "array",
                                "items", stringField
                        ))
                ),
                "required", List.of(
                        "name",
                        "roastery",
                        "originCountry",
                        "region",
                        "farmOrStation",
                        "variety",
                        "altitude",
                        "process",
                        "beanStatus",
                        "roastedAt",
                        "purchasedAt",
                        "price",
                        "remainingWeightGram",
                        "flavorNotes"
                ),
                "additionalProperties", false
        );
    }

    private String extractGeneratedText(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.isObject() && root.has("response")) {
                return root.get("response").asText();
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

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
    }
}
