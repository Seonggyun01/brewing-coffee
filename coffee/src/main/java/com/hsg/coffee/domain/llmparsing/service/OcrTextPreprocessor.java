package com.hsg.coffee.domain.llmparsing.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.hsg.coffee.domain.llmparsing.dto.KeyValueCandidate;
import com.hsg.coffee.domain.llmparsing.dto.OcrPreprocessResult;
import com.hsg.coffee.domain.coffeeBean.service.FlavorNoteTextMapper;
import com.hsg.coffee.global.country.CountryInfo;

@Component
public class OcrTextPreprocessor {

    private static final int MAX_TASTING_NOTE_COUNT = 8;
    private static final Pattern VARIETY_DOT_SEPARATOR = Pattern.compile("(\\d{5,})\\.(\\d{5,})");
    private static final Set<String> KNOWN_ROASTERIES = Set.of(
            "CLOSE COFFEE",
            "PLTER",
            "FELT",
            "FRITZ",
            "CENTER COFFEE",
            "COFFEE LIBRE",
            "MOMOS COFFEE"
    );
    private static final Set<String> ROASTERY_KEYWORDS = Set.of(
            "COFFEE",
            "ROASTERS",
            "ROASTERY",
            "CAFE",
            "커피",
            "로스터스"
    );
    private static final Set<String> DETAIL_KEYWORDS = Set.of(
            "COUNTRY",
            "ORIGIN",
            "PRODUCER COUNTRY",
            "국가",
            "원산지",
            "생산국",
            "산지 국가",
            "REGION",
            "AREA",
            "ZONE",
            "DISTRICT",
            "지역",
            "산지",
            "FARM",
            "FARMER",
            "VILLAGE",
            "STATION",
            "WASHING STATION",
            "농장",
            "마을",
            "워싱스테이션",
            "가공소",
            "VARIETY",
            "VARIETAL",
            "CULTIVAR",
            "품종",
            "PROCESS",
            "PROCESSING",
            "PROCESS METHOD",
            "가공",
            "가공 방식",
            "프로세스",
            "ALTITUDE",
            "ELEVATION",
            "MASL",
            "고도",
            "해발고도",
            "해발",
            "TASTING NOTES",
            "CUP NOTES",
            "FLAVOR",
            "FLAVOUR",
            "NOTES",
            "향미",
            "테이스팅 노트",
            "컵노트",
            "ROASTED",
            "ROAST DATE",
            "ROASTED ON",
            "로스팅일",
            "볶은날"
    );
    private static final Set<String> TASTING_NOTE_KEYS = Set.of(
            "TASTING NOTES",
            "CUP NOTES",
            "FLAVOR",
            "FLAVOUR",
            "NOTES",
            "향미",
            "테이스팅 노트",
            "컵노트"
    );

    public OcrPreprocessResult preprocess(String rawText) {
        List<String> cleanedLines = cleanLines(rawText);
        List<KeyValueCandidate> keyValueCandidates = findKeyValueCandidates(cleanedLines);
        List<String> roasteryCandidates = findRoasteryCandidates(cleanedLines);
        List<String> productNameCandidates = findProductNameCandidates(cleanedLines, roasteryCandidates);
        List<String> tastingNoteCandidates = findTastingNoteCandidates(cleanedLines);
        String promptText = buildPromptText(
                cleanedLines,
                roasteryCandidates,
                productNameCandidates,
                keyValueCandidates,
                tastingNoteCandidates,
                rawText
        );

        return new OcrPreprocessResult(
                rawText == null ? "" : rawText,
                cleanedLines,
                roasteryCandidates,
                productNameCandidates,
                keyValueCandidates,
                tastingNoteCandidates,
                promptText
        );
    }

    private List<String> cleanLines(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        for (String line : rawText.split("\\R")) {
            String cleaned = cleanLine(line);
            if (!cleaned.isBlank()) {
                lines.add(cleaned);
            }
        }
        return lines;
    }

    private String cleanLine(String line) {
        if (line == null) {
            return "";
        }

        String cleaned = line.trim()
                .replaceAll("^[◎ㅇ○●•\\-*ㆍ·]+\\s*", "")
                .replaceAll("\\s+", " ");

        Matcher matcher = VARIETY_DOT_SEPARATOR.matcher(cleaned);
        return matcher.replaceAll("$1, $2").trim();
    }

    private List<KeyValueCandidate> findKeyValueCandidates(List<String> lines) {
        LinkedHashSet<KeyValueCandidate> candidates = new LinkedHashSet<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String matchedKey = findMatchedKeyAtStart(line);
            if (matchedKey != null) {
                if (isTastingNoteKey(line) || isPartOfConsecutiveKeyBlock(lines, index)) {
                    continue;
                }
                String inlineValue = line.substring(matchedKey.length()).replaceFirst("^[:：\\-\\s]+", "").trim();
                if (!inlineValue.isBlank() && !isDetailKey(inlineValue)) {
                    candidates.add(new KeyValueCandidate(matchedKey, inlineValue));
                    continue;
                }
                if (index + 1 < lines.size() && !isDetailKey(lines.get(index + 1))) {
                    candidates.add(new KeyValueCandidate(matchedKey, lines.get(index + 1)));
                }
            }
        }

        for (int index = 0; index < lines.size(); index++) {
            List<String> keys = new ArrayList<>();
            int cursor = index;
            while (cursor < lines.size() && isDetailKey(lines.get(cursor))) {
                keys.add(normalizeKey(lines.get(cursor)));
                cursor++;
            }
            if (keys.size() < 2 || cursor + keys.size() > lines.size()) {
                continue;
            }

            boolean allValues = true;
            for (int offset = 0; offset < keys.size(); offset++) {
                if (isDetailKey(lines.get(cursor + offset))) {
                    allValues = false;
                    break;
                }
            }
            if (!allValues) {
                continue;
            }

            for (int offset = 0; offset < keys.size(); offset++) {
                candidates.add(new KeyValueCandidate(keys.get(offset), lines.get(cursor + offset)));
            }
            index = cursor + keys.size() - 1;
        }

        return List.copyOf(candidates);
    }

    private List<String> findRoasteryCandidates(List<String> lines) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        int topLimit = Math.min(3, lines.size());
        for (int index = 0; index < topLimit; index++) {
            String line = lines.get(index);
            if (looksLikeRoastery(line)) {
                candidates.add(line);
            }
        }

        for (String line : lines) {
            if (KNOWN_ROASTERIES.contains(normalizeUpper(line)) || containsAny(line, ROASTERY_KEYWORDS)) {
                candidates.add(line);
            }
        }

        return List.copyOf(candidates);
    }

    private boolean looksLikeRoastery(String line) {
        if (line == null || line.isBlank() || isDetailKey(line) || looksLikeBeanDetail(line)) {
            return false;
        }
        return line.length() <= 28 || containsAny(line, ROASTERY_KEYWORDS);
    }

    private List<String> findProductNameCandidates(List<String> lines, List<String> roasteryCandidates) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        for (String roasteryCandidate : roasteryCandidates) {
            int index = lines.indexOf(roasteryCandidate);
            if (index < 0) {
                continue;
            }
            List<String> adjacent = new ArrayList<>();
            for (int offset = 1; offset <= 3 && index + offset < lines.size(); offset++) {
                String line = lines.get(index + offset);
                if (isDetailKey(line)) {
                    break;
                }
                adjacent.add(line);
                candidates.add(line);
            }
            if (adjacent.size() >= 2) {
                candidates.add(String.join(" ", adjacent));
            }
        }

        List<String> titleLines = new ArrayList<>();
        for (String line : lines) {
            if (isDetailKey(line)) {
                break;
            }
            if (!roasteryCandidates.contains(line)) {
                titleLines.add(line);
                candidates.add(line);
            }
        }
        if (titleLines.size() >= 2) {
            candidates.add(String.join(" ", titleLines));
        }

        return candidates.stream()
                .filter(candidate -> candidate != null && !candidate.isBlank())
                .limit(8)
                .toList();
    }

    private List<String> findTastingNoteCandidates(List<String> lines) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        boolean inTastingNoteBlock = false;

        for (String line : lines) {
            if (isTastingNoteKey(line)) {
                inTastingNoteBlock = true;
                String inlineValue = line.replaceFirst("(?i)^.*?(TASTING NOTES|CUP NOTES|FLAVOU?R|NOTES|향미|테이스팅 노트|컵노트)[:：\\-\\s]*", "").trim();
                addTastingNotes(candidates, inlineValue);
                continue;
            }

            if (inTastingNoteBlock && isDetailKey(line)) {
                break;
            }
            if (inTastingNoteBlock) {
                addTastingNotes(candidates, line);
            }
            if (candidates.size() >= MAX_TASTING_NOTE_COUNT) {
                break;
            }
        }

        for (String line : lines) {
            if (candidates.size() >= MAX_TASTING_NOTE_COUNT) {
                break;
            }
            if (isDetailKey(line) || looksLikeBeanDetail(line) || !hasFlavorSeparator(line)) {
                continue;
            }
            List<String> tokens = splitTastingNoteTokens(line);
            if (tokens.size() >= 2 && tokens.stream().anyMatch(token -> !FlavorNoteTextMapper.findFlavorNotes(token).isEmpty())) {
                tokens.forEach(token -> addTastingNotes(candidates, token));
            }
        }

        return candidates.stream()
                .limit(MAX_TASTING_NOTE_COUNT)
                .toList();
    }

    private void addTastingNotes(Set<String> candidates, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String note : splitTastingNoteTokens(text)) {
            String cleaned = cleanLine(note);
            if (!cleaned.isBlank()) {
                candidates.add(cleaned);
            }
        }
    }

    private boolean hasFlavorSeparator(String line) {
        return line.contains(",") || line.contains("/") || line.contains("·") || line.contains("ㆍ") || line.contains("|");
    }

    private List<String> splitTastingNoteTokens(String text) {
        return Pattern.compile("[,/·ㆍ|]")
                .splitAsStream(text)
                .map(this::cleanLine)
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() <= 40)
                .toList();
    }

    private boolean isPartOfConsecutiveKeyBlock(List<String> lines, int index) {
        return (index > 0 && isDetailKey(lines.get(index - 1)))
                || (index + 1 < lines.size() && isDetailKey(lines.get(index + 1)));
    }

    private String buildPromptText(
            List<String> cleanedLines,
            List<String> roasteryCandidates,
            List<String> productNameCandidates,
            List<KeyValueCandidate> keyValueCandidates,
            List<String> tastingNoteCandidates,
            String rawText
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("[RAW_LINES]\n");
        for (int index = 0; index < cleanedLines.size(); index++) {
            builder.append(index + 1).append(". ").append(cleanedLines.get(index)).append("\n");
        }
        appendSection(builder, "ROASTERY_CANDIDATES", roasteryCandidates);
        appendSection(builder, "PRODUCT_NAME_CANDIDATES", productNameCandidates);
        builder.append("\n[KEY_VALUE_CANDIDATES]\n");
        for (KeyValueCandidate candidate : keyValueCandidates) {
            builder.append(candidate.key()).append(" -> ").append(candidate.value()).append("\n");
        }
        appendSection(builder, "TASTING_NOTE_CANDIDATES", tastingNoteCandidates);
        builder.append("\n[RAW_OCR_TEXT]\n").append(rawText == null ? "" : rawText);
        return builder.toString();
    }

    private void appendSection(StringBuilder builder, String title, List<String> values) {
        builder.append("\n[").append(title).append("]\n");
        for (String value : values) {
            builder.append(value).append("\n");
        }
    }

    private boolean isTastingNoteKey(String line) {
        String normalized = normalizeKey(line);
        return TASTING_NOTE_KEYS.stream()
                .map(this::normalizeKey)
                .anyMatch(normalized::contains);
    }

    private boolean isDetailKey(String line) {
        return findMatchedKeyAtStart(line) != null || DETAIL_KEYWORDS.stream()
                .map(this::normalizeKey)
                .anyMatch(key -> normalizeKey(line).equals(key));
    }

    private String findMatchedKeyAtStart(String line) {
        String normalizedLine = normalizeKey(line);
        return DETAIL_KEYWORDS.stream()
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .filter(key -> normalizedLine.equals(normalizeKey(key))
                        || normalizedLine.startsWith(normalizeKey(key) + " "))
                .findFirst()
                .orElse(null);
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("[:：\\-]+$", "")
                .replaceAll("[:：\\-]+", " ")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private boolean containsAny(String line, Set<String> keywords) {
        String normalized = normalizeUpper(line);
        return keywords.stream().anyMatch(keyword -> normalized.contains(normalizeUpper(keyword)));
    }

    private boolean looksLikeBeanDetail(String line) {
        String upper = normalizeUpper(line);
        if (upper.matches(".*\\d{3,}\\s*(M|MASL).*")) {
            return true;
        }
        if (upper.contains("NATURAL") || upper.contains("WASHED") || upper.contains("HONEY")
                || upper.contains("ANAEROBIC") || upper.contains("내추럴") || upper.contains("워시드")
                || upper.contains("허니") || upper.contains("무산소")) {
            return true;
        }
        for (CountryInfo countryInfo : CountryInfo.values()) {
            if (upper.contains(normalizeUpper(countryInfo.getEnglishName()))
                    || line.contains(countryInfo.getKoreanName())) {
                return true;
            }
        }
        return false;
    }
}
