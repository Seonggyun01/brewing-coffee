package com.hsg.coffee.domain.llmparsing.dto;

import java.util.List;

public record LlmParsingResponse(
        String name,
        String roastery,
        String originCountry,
        String region,
        String farmOrStation,
        String variety,
        String altitude,
        String process,
        String beanStatus,
        String roastedAt,
        String purchasedAt,
        String price,
        String remainingWeightGram,
        List<String> flavorNotes
) {
    public static LlmParsingResponse empty() {
        return new LlmParsingResponse(
                "", "", "", "", "", "", "", "", "", "", "", "", "", List.of()
        );
    }
}
