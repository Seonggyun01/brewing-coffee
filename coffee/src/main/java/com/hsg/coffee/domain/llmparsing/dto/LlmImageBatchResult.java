package com.hsg.coffee.domain.llmparsing.dto;

public record LlmImageBatchResult(
        String imageFile,
        String status,
        String error,
        String ocrTextFile,
        String llmDebugFile,
        LlmParsingResponse parsedResponse
) {
}
