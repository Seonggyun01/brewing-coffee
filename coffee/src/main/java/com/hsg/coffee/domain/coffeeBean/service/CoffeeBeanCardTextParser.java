package com.hsg.coffee.domain.coffeeBean.service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCardTextParseResult;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.global.country.CountryInfo;

@Component
public class CoffeeBeanCardTextParser {

    private static final Pattern GRAM_PATTERN = Pattern.compile("(\\d{2,4})\\s*g", Pattern.CASE_INSENSITIVE);
    private static final Pattern KG_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*kg", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?:₩|KRW\\s*)\\s*(\\d{1,3}(?:,\\d{3})+|\\d{4,7})"
                    + "|(\\d{1,3}(?:,\\d{3})+|\\d{4,7})\\s*(?:원|KRW)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ROASTED_DATE_PATTERN = Pattern.compile(
            "(20\\d{2}|\\d{2})[./-](\\d{1,2})[./-](\\d{1,2})"
    );
    private static final Pattern ROASTERY_PATTERN = Pattern.compile(
            "^(?:Roasted by|Roastery|Roaster|로스터리|볶은 곳)\\s*[:\\-]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ORIGIN_PATTERN = Pattern.compile(
            "^(?:Origin|Country|원산지|산지|국가)\\s*[:\\-]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REGION_PATTERN = Pattern.compile(
            "^(?:Region|지역)\\s*[:\\-]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FARM_PATTERN = Pattern.compile(
            "^(?:Farm|Producer|Estate|Washing Station|Station|Mill|농장|생산자|워싱스테이션)\\s*[:\\-]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VARIETY_PATTERN = Pattern.compile(
            "^(?:Variety|Varietal|품종)\\s*[:\\-]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ALTITUDE_LABEL_PATTERN = Pattern.compile(
            "^(?:Altitude|Elevation|고도)\\s*[:\\-]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ALTITUDE_VALUE_PATTERN = Pattern.compile(
            "((?:\\d{3,4}|\\d{1,2},\\d{3})\\s*(?:(?:m\\.a\\.s\\.l\\.?|masl|m)\\s*)?"
                    + "(?:[-~]\\s*(?:\\d{3,4}|\\d{1,2},\\d{3})\\s*)?(?:m\\.a\\.s\\.l\\.?|masl|m))",
            Pattern.CASE_INSENSITIVE
    );

    public CoffeeBeanCardTextParseResult parse(String rawText) {
        CoffeeBeanCardTextParseResult result = new CoffeeBeanCardTextParseResult();
        String normalizedText = normalize(rawText);

        if (!StringUtils.hasText(normalizedText)) {
            result.getWarnings().add("이미지에서 텍스트를 추출하지 못했어요. 직접 입력해주세요.");
            return result;
        }

        result.setName(extractName(normalizedText));
        result.setRoastery(extractRoastery(normalizedText));
        result.setRegion(extractLabeledValue(normalizedText, REGION_PATTERN));
        result.setFarm(extractLabeledValue(normalizedText, FARM_PATTERN));
        result.setVariety(extractLabeledValue(normalizedText, VARIETY_PATTERN));
        result.setAltitude(extractAltitude(normalizedText));
        result.setProcessType(extractProcessType(normalizedText));
        extractCountry(normalizedText, result);
        extractOriginDetails(normalizedText, result);
        result.setFlavorNotes(extractFlavorNotes(normalizedText).stream().toList());
        result.setCustomFlavorNotesText(extractCustomFlavorNotesText(normalizedText, result.getFlavorNotes()));
        result.setWeight(extractWeight(normalizedText));
        result.setPrice(extractPrice(normalizedText));
        result.setRoastedDate(extractRoastedDate(normalizedText));

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
                || lower.contains("carbonic")
                || lower.contains("decaf")
                || lower.contains("roasted")
                || lower.contains("roaster")
                || lower.contains("roastery")
                || lower.contains("로스터리")
                || lower.contains("볶은 곳")
                || lower.contains("region")
                || lower.contains("origin")
                || lower.contains("country")
                || lower.contains("farm")
                || lower.contains("producer")
                || lower.contains("estate")
                || lower.contains("station")
                || lower.contains("variety")
                || lower.contains("altitude")
                || lower.contains("elevation")
                || line.contains("원산지")
                || line.contains("산지")
                || line.contains("국가")
                || line.contains("지역")
                || line.contains("농장")
                || line.contains("생산자")
                || line.contains("워싱스테이션")
                || line.contains("품종")
                || line.contains("고도")
                || lower.matches(".*\\d+\\s*g.*")
                || lower.matches(".*\\d+(?:\\.\\d+)?\\s*kg.*")
                || lower.matches(".*\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}.*")
                || lower.matches(".*\\d{2}[./-]\\d{1,2}[./-]\\d{1,2}.*")
                || containsAltitude(line)
                || containsPrice(line);
    }

    private String extractRoastery(String text) {
        return extractLabeledValue(text, ROASTERY_PATTERN);
    }

    private String extractLabeledValue(String text, Pattern pattern) {
        return text.lines()
                .map(String::trim)
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1).trim())
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private ProcessType extractProcessType(String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        if (lower.contains("carbonic")) {
            return ProcessType.CARBONIC_MACERATION;
        }
        if (lower.contains("decaf") || text.contains("디카페인")) {
            return ProcessType.DECAF;
        }
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

    private void extractOriginDetails(String text, CoffeeBeanCardTextParseResult result) {
        String originText = extractLabeledValue(text, ORIGIN_PATTERN);
        if (!StringUtils.hasText(originText)) {
            return;
        }

        List<String> parts = splitOriginParts(originText);
        if (parts.isEmpty()) {
            return;
        }

        CountryInfo countryInfo = findCountry(parts.getFirst());
        int detailStartIndex = 0;
        if (countryInfo != null) {
            result.setOriginCountryCode(countryInfo.getCode());
            result.setCountry(countryInfo.getEnglishName());
            detailStartIndex = 1;
        }

        if (!StringUtils.hasText(result.getRegion()) && parts.size() > detailStartIndex) {
            result.setRegion(parts.get(detailStartIndex));
        }
        if (!StringUtils.hasText(result.getFarm()) && parts.size() > detailStartIndex + 1) {
            result.setFarm(parts.get(detailStartIndex + 1));
        }
    }

    private List<String> splitOriginParts(String originText) {
        return Arrays.stream(originText.split("[/，,·>|]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private CountryInfo findCountry(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return Arrays.stream(CountryInfo.values())
                .filter(countryInfo -> lower.contains(countryInfo.getEnglishName().toLowerCase(Locale.ROOT))
                        || text.contains(countryInfo.getKoreanName()))
                .findFirst()
                .orElse(null);
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

        removeSubsumedFlavorNotes(notes);
        return notes;
    }

    private void removeSubsumedFlavorNotes(Set<FlavorNote> notes) {
        Set<FlavorNote> notesToRemove = new LinkedHashSet<>();

        for (FlavorNote note : notes) {
            String keyword = note.name().toLowerCase(Locale.ROOT).replace("_", " ");
            for (FlavorNote otherNote : notes) {
                if (note == otherNote) {
                    continue;
                }

                String otherKeyword = otherNote.name().toLowerCase(Locale.ROOT).replace("_", " ");
                if (otherKeyword.contains(keyword)) {
                    notesToRemove.add(note);
                    break;
                }
            }
        }

        notes.removeAll(notesToRemove);
    }

    private String extractCustomFlavorNotesText(String text, List<FlavorNote> matchedNotes) {
        Set<String> customNotes = new LinkedHashSet<>();

        text.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(this::isFlavorCandidateLine)
                .flatMap(line -> Arrays.stream(line.split("[,，/·|]")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(token -> !isKnownFlavorNote(token, matchedNotes))
                .filter(token -> !isMetadataLine(token))
                .filter(token -> token.length() <= 40)
                .forEach(customNotes::add);

        if (customNotes.isEmpty()) {
            return null;
        }

        return String.join(", ", customNotes);
    }

    private boolean isFlavorCandidateLine(String line) {
        if (isMetadataLine(line)) {
            return false;
        }

        boolean hasSeparator = line.contains(",")
                || line.contains("，")
                || line.contains("/")
                || line.contains("·")
                || line.contains("|");
        if (!hasSeparator) {
            return false;
        }

        String lower = line.toLowerCase(Locale.ROOT);
        return !lower.contains("region")
                && !lower.contains("origin")
                && !lower.contains("country")
                && !lower.contains("farm")
                && !lower.contains("producer")
                && !lower.contains("estate")
                && !lower.contains("station")
                && !lower.contains("variety")
                && !lower.contains("altitude")
                && !lower.contains("elevation")
                && !line.contains("원산지")
                && !line.contains("산지")
                && !line.contains("국가")
                && !line.contains("지역")
                && !line.contains("농장")
                && !line.contains("생산자")
                && !line.contains("워싱스테이션")
                && !line.contains("품종")
                && !line.contains("고도");
    }

    private boolean isKnownFlavorNote(String token, List<FlavorNote> matchedNotes) {
        String lower = token.toLowerCase(Locale.ROOT);

        for (FlavorNote note : matchedNotes) {
            String enumKeyword = note.name().toLowerCase(Locale.ROOT).replace("_", " ");
            if (lower.contains(enumKeyword) || token.contains(note.getDisplayName())) {
                return true;
            }
        }

        return false;
    }

    private Integer extractWeight(String text) {
        Matcher gramMatcher = GRAM_PATTERN.matcher(text);
        if (gramMatcher.find()) {
            return Integer.parseInt(gramMatcher.group(1));
        }

        Matcher kgMatcher = KG_PATTERN.matcher(text);
        if (kgMatcher.find()) {
            BigDecimal kg = new BigDecimal(kgMatcher.group(1));
            return kg.multiply(BigDecimal.valueOf(1000)).intValue();
        }

        return null;
    }

    private String extractAltitude(String text) {
        String labeledAltitude = extractLabeledValue(text, ALTITUDE_LABEL_PATTERN);
        if (StringUtils.hasText(labeledAltitude)) {
            Matcher matcher = ALTITUDE_VALUE_PATTERN.matcher(labeledAltitude);
            if (matcher.find()) {
                return normalizeAltitude(matcher.group(1));
            }
            return labeledAltitude;
        }

        Matcher matcher = ALTITUDE_VALUE_PATTERN.matcher(text);
        if (matcher.find()) {
            return normalizeAltitude(matcher.group(1));
        }

        return null;
    }

    private String normalizeAltitude(String altitude) {
        return altitude.replaceAll("\\s+", " ").trim();
    }

    private Integer extractPrice(String text) {
        Matcher matcher = PRICE_PATTERN.matcher(text);

        while (matcher.find()) {
            String priceText = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            int price = Integer.parseInt(priceText.replace(",", ""));
            if (price >= 1000) {
                return price;
            }
        }

        return null;
    }

    private LocalDate extractRoastedDate(String text) {
        List<LocalDate> fallbackDates = new ArrayList<>();

        for (String line : text.lines().map(String::trim).toList()) {
            Matcher matcher = ROASTED_DATE_PATTERN.matcher(line);
            while (matcher.find()) {
                try {
                    LocalDate date = toLocalDate(matcher);
                    if (isRoastedDateLine(line)) {
                        return date;
                    }
                    fallbackDates.add(date);
                } catch (DateTimeException ignored) {
                    // OCR can produce invalid dates, so keep scanning for another candidate.
                }
            }
        }

        return fallbackDates.isEmpty() ? null : fallbackDates.getFirst();
    }

    private boolean isRoastedDateLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("roast")
                || line.contains("로스팅")
                || line.contains("볶은")
                || line.contains("제조");
    }

    private LocalDate toLocalDate(Matcher matcher) {
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        if (year < 100) {
            year += 2000;
        }
        return LocalDate.of(year, month, day);
    }

    private boolean containsPrice(String text) {
        return PRICE_PATTERN.matcher(text).find();
    }

    private boolean containsAltitude(String text) {
        return ALTITUDE_VALUE_PATTERN.matcher(text).find();
    }
}
