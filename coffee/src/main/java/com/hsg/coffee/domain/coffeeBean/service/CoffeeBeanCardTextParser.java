package com.hsg.coffee.domain.coffeeBean.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCardTextParseResult;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.global.country.CountryInfo;

@Component
public class CoffeeBeanCardTextParser {

    public CoffeeBeanCardTextParseResult parse(String rawText) {
        CoffeeBeanCardTextParseResult result = new CoffeeBeanCardTextParseResult();
        String normalizedText = normalize(rawText);

        if (!StringUtils.hasText(normalizedText)) {
            result.getWarnings().add("이미지에서 텍스트를 추출하지 못했어요. 직접 입력해주세요.");
            return result;
        }

        result.setName(extractName(normalizedText));
        result.setRoastery(extractRoastery(normalizedText));
        result.setProcessType(extractProcessType(normalizedText));
        extractCountry(normalizedText, result);
        result.setFlavorNotes(extractFlavorNotes(normalizedText).stream().toList());

        if (!StringUtils.hasText(result.getName())) {
            result.getWarnings().add("원두 이름을 자동으로 찾지 못했어요.");
        }
        if (!StringUtils.hasText(result.getCountry())) {
            result.getWarnings().add("원산지 국가를 자동으로 찾지 못했어요.");
        }

        return result;
    }

    private String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }

        return rawText
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ ]{2,}", " ")
                .trim();
    }

    private String extractName(String text) {
        return text.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> !isMetadataLine(line))
                .findFirst()
                .orElse(null);
    }

    private boolean isMetadataLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);

        return lower.contains("washed")
                || lower.contains("natural")
                || lower.contains("honey")
                || lower.contains("anaerobic")
                || lower.contains("roasted")
                || lower.matches(".*\\d+\\s*g.*")
                || lower.matches(".*\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}.*")
                || lower.matches(".*\\d{1,3}(,\\d{3})+.*");
    }

    private String extractRoastery(String text) {
        return text.lines()
                .map(String::trim)
                .filter(line -> line.toLowerCase(Locale.ROOT).startsWith("roasted by"))
                .map(line -> line.replaceFirst("(?i)^roasted by\\s*[:\\-]?\\s*", "").trim())
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private ProcessType extractProcessType(String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        if (lower.contains("anaerobic") || text.contains("무산소")) {
            return ProcessType.ANAEROBIC;
        }
        if (lower.contains("washed") || lower.contains("wash") || text.contains("워시드")) {
            return ProcessType.WASHED;
        }
        if (lower.contains("natural") || text.contains("내추럴")) {
            return ProcessType.NATURAL;
        }
        if (lower.contains("honey") || text.contains("허니")) {
            return ProcessType.HONEY;
        }

        return null;
    }

    private void extractCountry(String text, CoffeeBeanCardTextParseResult result) {
        String lower = text.toLowerCase(Locale.ROOT);

        Arrays.stream(CountryInfo.values())
                .filter(countryInfo -> lower.contains(countryInfo.getEnglishName().toLowerCase(Locale.ROOT))
                        || text.contains(countryInfo.getKoreanName()))
                .findFirst()
                .ifPresent(countryInfo -> {
                    result.setOriginCountryCode(countryInfo.getCode());
                    result.setCountry(countryInfo.getEnglishName());
                });
    }

    private Set<FlavorNote> extractFlavorNotes(String text) {
        Set<FlavorNote> notes = new LinkedHashSet<>();
        String lower = text.toLowerCase(Locale.ROOT);

        for (FlavorNote note : FlavorNote.values()) {
            String enumKeyword = note.name().toLowerCase(Locale.ROOT).replace("_", " ");
            if (lower.contains(enumKeyword) || text.contains(note.getDisplayName())) {
                notes.add(note);
            }
        }

        return notes;
    }
}
