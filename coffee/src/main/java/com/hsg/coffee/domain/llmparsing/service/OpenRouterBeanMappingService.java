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
            return LlmParsingResponse.empty();
        }
        if (!chatClient.isConfigured()) {
            log.warn("OpenRouter 설정이 비어 있어 LLM 매핑을 건너뜁니다.");
            return LlmParsingResponse.empty();
        }

        try {
            OcrPreprocessResult preprocessResult = promptFactory.preprocess(ocrText);
            OpenRouterChatClient.Completion completion = chatClient.complete(
                    promptFactory.createPrompt(preprocessResult)
            );
            String json = extractJson(completion.content());
            if (json.isBlank()) {
                log.warn(
                        "OpenRouter 응답에서 JSON을 찾지 못했습니다. model={}, response={}",
                        completion.modelId(),
                        abbreviate(completion.content())
                );
                return LlmParsingResponse.empty();
            }

            LlmParsingResponse parsed = objectMapper.readValue(json, LlmParsingResponse.class);
            return validator.sanitize(parsed, preprocessResult);
        } catch (Exception exception) {
            log.warn("OpenRouter OCR 매핑 처리 실패: {}", exception.getMessage());
            return LlmParsingResponse.empty();
        }
    }

    @Override
    public LlmParsingDebugResponse debugParseOcrText(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return debugResponse("SKIPPED", "ocrText is blank", "", "", "", LlmParsingResponse.empty());
        }
        if (!chatClient.isConfigured()) {
            return debugResponse(
                    "SKIPPED",
                    "OpenRouter configuration is empty",
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
            String json = extractJson(completion.content());
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
