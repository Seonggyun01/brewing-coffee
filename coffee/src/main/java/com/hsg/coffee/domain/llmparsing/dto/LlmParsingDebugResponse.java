package com.hsg.coffee.domain.llmparsing.dto;

public record LlmParsingDebugResponse(
        String modelUrl,
        String modelId,
        String status,
        String error,
        String rawResponse,
        String generatedText,
        String extractedJson,
        LlmParsingResponse parsedResponse
) {
}
