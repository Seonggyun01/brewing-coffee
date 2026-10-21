package com.hsg.coffee.domain.coffeeBean.service;

import java.time.LocalDate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Reject unlabeled background numbers, including guesses reintroduced by the text parser. */
final class OcrMetadataEvidence {
    private static final Pattern PRICE_LABEL = Pattern.compile(
            "(?iu)^(?:원두\\s*가격|판매가|구매\\s*가격|가격|price\\b)\\s*[:：-]?\\s*");
    private static final Pattern ROAST_LABEL = Pattern.compile(
            "(?iu)^(?:로스팅\\s*(?:일자|날짜|일)|볶은\\s*날|roast\\s*date\\b|roasted(?:\\s+on)?\\b)\\s*[:：-]?\\s*");

    private OcrMetadataEvidence() {}

    static Integer price(String rawText, Integer candidate) {
        if (candidate == null) return null;
        return hasEvidence(rawText, PRICE_LABEL, value -> {
            String amount = value.replaceAll("(?iu)(?:KRW|원|₩|\\s|,)", "");
            return amount.equals(candidate.toString());
        }) ? candidate : null;
    }

    static LocalDate roastedDate(String rawText, LocalDate candidate) {
        if (candidate == null) return null;
        return hasEvidence(rawText, ROAST_LABEL, value -> {
            var match = Pattern.compile("^(\\d{4}|\\d{2})\\s*[-./년]\\s*(\\d{1,2})\\s*[-./월]\\s*(\\d{1,2})\\s*[일.]?$")
                    .matcher(value.trim());
            if (!match.matches()) return false;
            int year = Integer.parseInt(match.group(1));
            if (match.group(1).length() == 2) year += 2000;
            return year == candidate.getYear()
                    && Integer.parseInt(match.group(2)) == candidate.getMonthValue()
                    && Integer.parseInt(match.group(3)) == candidate.getDayOfMonth();
        }) ? candidate : null;
    }

    private static boolean hasEvidence(String text, Pattern label, Predicate<String> matchesValue) {
        if (text == null) return false;
        String[] lines = text.lines().map(String::trim).filter(line -> !line.isBlank()).toArray(String[]::new);
        for (int i = 0; i < lines.length; i++) {
            var match = label.matcher(lines[i]);
            if (!match.find()) continue;
            String value = lines[i].substring(match.end()).trim();
            if (value.isEmpty() && i + 1 < lines.length) value = lines[i + 1];
            if (matchesValue.test(value)) return true;
        }
        return false;
    }
}
