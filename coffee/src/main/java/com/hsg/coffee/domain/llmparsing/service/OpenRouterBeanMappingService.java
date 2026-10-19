package com.hsg.coffee.domain.llmparsing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.hsg.coffee.domain.llmparsing.dto.LlmParsingDebugResponse;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;
import com.hsg.coffee.domain.llmparsing.dto.OcrPreprocessResult;

import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "brewlog.llm.provider", havingValue = "openrouter")
public class OpenRouterBeanMappingService implements BeanMappingService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterBeanMappingService.class);

    private final OpenRouterChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final BeanOcrMappingValidator validator;
    private final BeanMappingPromptFactory promptFactory;

    public OpenRouterBeanMappingService(
            OpenRouterChatClient chatClient,
            ObjectMapper objectMapper,
            BeanOcrMappingValidator validator,
            BeanMappingPromptFactory promptFactory
    ) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.promptFactory = promptFactory;
    }

    @Override
    public LlmParsingResponse parseOcrText(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            throw new IllegalArgumentException("AI가 원두 정보를 정리하지 못했어요. 다시 시도하거나 직접 입력해주세요.");
        }
        if (!chatClient.isConfigured()) {
            com.hsg.coffee.global.logging.ExtractionTrace.record("llm.failure", "LLM provider is not configured");
            log.warn("LLM 설정이 비어 있어 LLM 매핑을 건너뜁니다.");
            throw new IllegalArgumentException("AI가 원두 정보를 정리하지 못했어요. 다시 시도하거나 직접 입력해주세요.");
        }

        try {
            OcrPreprocessResult preprocessResult = promptFactory.preprocess(ocrText);
            OpenRouterChatClient.Completion completion = chatClient.complete(
                    promptFactory.createPrompt(preprocessResult)
            );
            com.hsg.coffee.global.logging.ExtractionTrace.record("llm.completion", java.util.Map.of("model", completion.modelId(), "content", completion.content()));
            String json = extractJson(completion.content());
            com.hsg.coffee.global.logging.ExtractionTrace.record("llm.json", json);
            if (json.isBlank()) {
                com.hsg.coffee.global.logging.ExtractionTrace.record("llm.failure", "LLM response did not contain JSON");
                log.warn(
                        "LLM 응답에서 JSON을 찾지 못했습니다. model={}, response={}",
                        completion.modelId(),
                        abbreviate(completion.content())
                );
                throw new IllegalArgumentException("AI가 원두 정보를 정리하지 못했어요. 다시 시도하거나 직접 입력해주세요.");
            }

            LlmParsingResponse parsed = objectMapper.readValue(json, LlmParsingResponse.class);
            return validator.sanitize(parsed, preprocessResult);
        } catch (Exception exception) {
            com.hsg.coffee.global.logging.ExtractionTrace.record("llm.failure", java.util.Map.of("provider", getClass().getSimpleName(), "type", exception.getClass().getSimpleName(), "message", String.valueOf(exception.getMessage())));
            log.warn("LLM OCR 매핑 처리 실패: {}", exception.getMessage());
            throw new IllegalArgumentException("AI 응답이 비어 있거나 처리에 실패했어요. 다시 시도하거나 직접 입력해주세요.", exception);
        }
    }

    @Override
    public LlmParsingDebugResponse debugParseOcrText(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return debugResponse("SKIPPED", "ocrText is blank", "", "", "", LlmParsingResponse.empty());
        }
        if (!chatClient.isConfigured()) {
            com.hsg.coffee.global.logging.ExtractionTrace.record("llm.failure", "LLM provider is not configured");
            return debugResponse(
                    "SKIPPED",
                    "LLM provider configuration is empty",
                    "",
                    "",
                    "",
                    LlmParsingResponse.empty()
            );
        }

        try {
            OcrPreprocessResult preprocessResult = promptFactory.preprocess(ocrText);
            OpenRouterChatClient.Completion completion = chatClient.complete(
                    promptFactory.createPrompt(preprocessResult)
            );
            com.hsg.coffee.global.logging.ExtractionTrace.record("llm.completion", java.util.Map.of("model", completion.modelId(), "content", completion.content()));
            String json = extractJson(completion.content());
            com.hsg.coffee.global.logging.ExtractionTrace.record("llm.json", json);
            LlmParsingResponse parsed = json.isBlank()
                    ? LlmParsingResponse.empty()
                    : validator.sanitize(
                            objectMapper.readValue(json, LlmParsingResponse.class),
                            preprocessResult
                    );

            return new LlmParsingDebugResponse(
                    chatClient.modelUrl(),
                    completion.modelId(),
                    "OK",
                    "",
                    completion.rawResponse(),
                    completion.content(),
                    json,
                    parsed
            );
        } catch (Exception exception) {
            com.hsg.coffee.global.logging.ExtractionTrace.record("llm.failure", java.util.Map.of("provider", getClass().getSimpleName(), "type", exception.getClass().getSimpleName(), "message", String.valueOf(exception.getMessage())));
            return debugResponse(
                    "ERROR",
                    exception.getMessage(),
                    "",
                    "",
                    "",
                    LlmParsingResponse.empty()
            );
        }
    }

    private LlmParsingDebugResponse debugResponse(
            String status,
            String error,
            String rawResponse,
            String generatedText,
            String json,
            LlmParsingResponse parsedResponse
    ) {
        return new LlmParsingDebugResponse(
                chatClient.modelUrl(),
                chatClient.modelId(),
                status,
                error,
                rawResponse,
                generatedText,
                json,
                parsedResponse
        );
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
