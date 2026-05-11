package com.hsg.coffee.domain.llmparsing.dto;

import java.util.List;

public record OcrPreprocessResult(
        String rawText,
        List<String> cleanedLines,
        List<String> roasteryCandidates,
        List<String> productNameCandidates,
        List<KeyValueCandidate> keyValueCandidates,
        List<String> tastingNoteCandidates,
        String promptText
) {
}
