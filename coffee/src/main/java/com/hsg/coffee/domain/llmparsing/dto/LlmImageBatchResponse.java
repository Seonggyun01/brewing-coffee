package com.hsg.coffee.domain.llmparsing.dto;

import java.util.List;

public record LlmImageBatchResponse(
        String imageDirectory,
        String outputDirectory,
        List<LlmImageBatchResult> results
) {
}
