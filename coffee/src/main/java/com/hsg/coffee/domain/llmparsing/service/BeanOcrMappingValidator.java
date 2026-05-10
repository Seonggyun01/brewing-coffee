package com.hsg.coffee.domain.llmparsing.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;

@Component
public class BeanOcrMappingValidator {

    private static final int MAX_FLAVOR_NOTE_COUNT = 8;

    private static final Set<String> ALLOWED_PROCESS_VALUES = Set.of(
            "NATURAL",
            "WASHED",
            "HONEY",
            "ANAEROBIC",
            "DECAF",
            "OTHER",
            ""
    );

    public LlmParsingResponse sanitize(LlmParsingResponse response) {
        if (response == null) {
            return LlmParsingResponse.empty();
        }

        return new LlmParsingResponse(
                clean(response.name()),
                clean(response.roastery()),
                clean(response.originCountry()),
                clean(response.region()),
                clean(response.farmOrStation()),
                clean(response.variety()),
                clean(response.altitude()),
                cleanProcess(response.process()),
                clean(response.beanStatus()),
                cleanDate(response.roastedAt()),
                cleanDate(response.purchasedAt()),
                digitsOnly(response.price()),
                digitsOnly(response.remainingWeightGram()),
                cleanFlavorNotes(response.flavorNotes())
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanProcess(String value) {
        String process = clean(value).toUpperCase();
        return ALLOWED_PROCESS_VALUES.contains(process) ? process : "";
    }

    private String digitsOnly(String value) {
        String digits = clean(value).replaceAll("[^0-9]", "");
        return digits.isBlank() ? "" : digits;
    }

    private String cleanDate(String value) {
        String date = clean(value);
        return date.matches("\\d{4}-\\d{2}-\\d{2}") ? date : "";
    }

    private List<String> cleanFlavorNotes(List<String> flavorNotes) {
        if (flavorNotes == null) {
            return List.of();
        }

        return flavorNotes.stream()
                .map(this::clean)
                .filter(note -> !note.isBlank())
                .limit(MAX_FLAVOR_NOTE_COUNT)
                .toList();
    }
}
