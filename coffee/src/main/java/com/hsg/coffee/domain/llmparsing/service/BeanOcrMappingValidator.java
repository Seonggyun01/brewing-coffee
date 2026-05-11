package com.hsg.coffee.domain.llmparsing.service;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.service.FlavorNoteTextMapper;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;
import com.hsg.coffee.domain.llmparsing.dto.OcrPreprocessResult;
import com.hsg.coffee.global.country.CountryInfo;

@Component
public class BeanOcrMappingValidator {

    private static final int MAX_FLAVOR_NOTE_COUNT = 8;
    private static final Map<String, String> COUNTRY_ALIASES = createCountryAliases();
    private static final Map<String, String> REGION_ALIASES = Map.ofEntries(
            Map.entry("YIRGACHEFFE", "Yirgacheffe"),
            Map.entry("예가체프", "Yirgacheffe"),
            Map.entry("이르가체페", "Yirgacheffe"),
            Map.entry("GUJI", "Guji"),
            Map.entry("구지", "Guji"),
            Map.entry("SIDAMO", "Sidamo"),
            Map.entry("시다모", "Sidamo"),
            Map.entry("GEDEB", "Gedeb"),
            Map.entry("게뎁", "Gedeb"),
            Map.entry("KIRINYAGA", "Kirinyaga"),
            Map.entry("키린야가", "Kirinyaga"),
            Map.entry("NYERI", "Nyeri"),
            Map.entry("니에리", "Nyeri"),
            Map.entry("HUILA", "Huila"),
            Map.entry("우일라", "Huila"),
            Map.entry("CAUCA", "Cauca"),
            Map.entry("카우카", "Cauca"),
            Map.entry("NARINO", "Narino"),
            Map.entry("NARIÑO", "Narino"),
            Map.entry("나리뇨", "Narino"),
            Map.entry("TOLIMA", "Tolima"),
            Map.entry("톨리마", "Tolima"),
            Map.entry("BOQUETE", "Boquete"),
            Map.entry("보케테", "Boquete"),
            Map.entry("CHIRIQUI", "Chiriqui"),
            Map.entry("CHIRIQUÍ", "Chiriqui"),
            Map.entry("치리키", "Chiriqui"),
            Map.entry("VOLCAN", "Volcan"),
            Map.entry("VOLCÁN", "Volcan"),
            Map.entry("볼칸", "Volcan"),
            Map.entry("TARRAZU", "Tarrazu"),
            Map.entry("TARRAZÚ", "Tarrazu"),
            Map.entry("따라주", "Tarrazu"),
            Map.entry("CERRADO", "Cerrado"),
            Map.entry("세하도", "Cerrado"),
            Map.entry("MINASGERAIS", "Minas Gerais"),
            Map.entry("MINAS GERAIS", "Minas Gerais"),
            Map.entry("미나스제라이스", "Minas Gerais"),
            Map.entry("JAEN", "Jaen"),
            Map.entry("하엔", "Jaen")
    );
    private static final Set<String> COUNTRY_KEYS = Set.of("COUNTRY", "ORIGIN", "PRODUCER COUNTRY", "국가", "원산지", "생산국");
    private static final Set<String> REGION_KEYS = Set.of("REGION", "AREA", "ZONE", "DISTRICT", "VILLAGE", "지역", "산지", "마을");
    private static final Set<String> FARM_KEYS = Set.of("FARM", "FARMER", "PRODUCER", "ESTATE", "STATION", "WASHING STATION", "농장", "생산자", "워싱스테이션", "가공소");
    private static final Set<String> KNOWN_ROASTERIES = Set.of(
            "CLOSE COFFEE",
            "PLTER",
            "FELT",
            "FRITZ",
            "CENTER COFFEE",
            "COFFEE LIBRE",
            "MOMOS COFFEE"
    );

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
        return sanitize(response, null);
    }

    public LlmParsingResponse sanitize(LlmParsingResponse response, OcrPreprocessResult preprocessResult) {
        if (response == null) {
            return LlmParsingResponse.empty();
        }

        String name = clean(response.name());
        String roastery = clean(response.roastery());
        String originCountry = cleanCountry(response.originCountry());
        if (originCountry.isBlank()) {
            originCountry = findCountryCandidate(preprocessResult);
        }
        String region = cleanRegion(response.region(), response.farmOrStation());
        if (region.isBlank()) {
            region = cleanRegion(findKeyValueCandidate(preprocessResult, REGION_KEYS), response.farmOrStation());
        }
        String farmOrStation = cleanFarmOrStation(response.farmOrStation());
        if (farmOrStation.isBlank()) {
            farmOrStation = cleanFarmOrStation(findKeyValueCandidate(preprocessResult, FARM_KEYS));
        }
        String process = cleanProcess(response.process());

        if (looksLikeBeanName(roastery) || isCountry(roastery) || isProcessText(roastery)) {
            roastery = "";
        }
        if (isKnownRoastery(name) && roastery.isBlank()) {
            roastery = name;
            name = bestProductNameCandidate(preprocessResult, roastery);
        }
        if (!name.isBlank() && name.equalsIgnoreCase(roastery)) {
            name = bestProductNameCandidate(preprocessResult, roastery);
        }
        if (region.equalsIgnoreCase(farmOrStation) || isCountry(region)) {
            region = "";
        }
        if (isCountry(farmOrStation) || isProcessText(farmOrStation)) {
            farmOrStation = "";
        }

        return new LlmParsingResponse(
                name,
                roastery,
                originCountry,
                region,
                farmOrStation,
                clean(response.variety()),
                clean(response.altitude()),
                process,
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
        process = switch (process) {
            case "내추럴", "NATURAL PROCESS" -> "NATURAL";
            case "워시드", "WASHED PROCESS", "SOAKING WASHED", "소킹 워시드" -> "WASHED";
            case "허니", "HONEY PROCESS" -> "HONEY";
            case "무산소", "ANAEROBIC NATURAL" -> "ANAEROBIC";
            case "디카페인" -> "DECAF";
            default -> process;
        };
        return ALLOWED_PROCESS_VALUES.contains(process) ? process : "";
    }

    private String cleanCountry(String value) {
        String country = clean(value);
        if (country.isBlank()) {
            return "";
        }
        String normalizedCountry = normalizeCountry(country);
        String aliasCountry = COUNTRY_ALIASES.get(normalizedCountry);
        if (aliasCountry != null) {
            return aliasCountry;
        }

        for (CountryInfo countryInfo : CountryInfo.values()) {
            if (containsCountry(normalizedCountry, countryInfo)) {
                return countryInfo.getKoreanName();
            }
        }

        return "";
    }

    private String cleanRegion(String value, String farmOrStation) {
        String region = cleanRegionName(value);
        if (region.length() > 80 || region.contains(". ")) {
            return "";
        }
        String farm = clean(farmOrStation);
        return !farm.isBlank() && region.equalsIgnoreCase(farm) ? "" : region;
    }

    private String cleanFarmOrStation(String value) {
        String farmOrStation = clean(value);
        return farmOrStation.length() > 100 || farmOrStation.contains(". ") ? "" : farmOrStation;
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

        LinkedHashSet<String> cleanedNotes = flavorNotes.stream()
                .map(this::clean)
                .filter(note -> !note.isBlank())
                .flatMap(note -> normalizeFlavorNote(note).stream())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        return cleanedNotes.stream()
                .limit(MAX_FLAVOR_NOTE_COUNT)
                .toList();
    }

    private List<String> normalizeFlavorNote(String value) {
        List<FlavorNote> mappedNotes = FlavorNoteTextMapper.findFlavorNotes(value);
        if (mappedNotes.isEmpty()) {
            return List.of(value);
        }

        return mappedNotes.stream()
                .map(FlavorNote::getDisplayName)
                .toList();
    }

    private String findCountryCandidate(OcrPreprocessResult preprocessResult) {
        String keyValueCountry = findKeyValueCandidate(preprocessResult, COUNTRY_KEYS);
        String country = cleanCountry(keyValueCountry);
        if (!country.isBlank()) {
            return country;
        }
        if (preprocessResult == null || preprocessResult.cleanedLines() == null) {
            return "";
        }

        return preprocessResult.cleanedLines().stream()
                .map(this::cleanCountry)
                .filter(candidate -> !candidate.isBlank())
                .findFirst()
                .orElse("");
    }

    private String findKeyValueCandidate(OcrPreprocessResult preprocessResult, Set<String> keys) {
        if (preprocessResult == null || preprocessResult.keyValueCandidates() == null) {
            return "";
        }

        return preprocessResult.keyValueCandidates().stream()
                .filter(candidate -> keys.contains(normalizeCountry(candidate.key())))
                .map(candidate -> clean(candidate.value()))
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private String cleanRegionName(String value) {
        String region = clean(value)
                .replaceFirst("(?i)^(region|area|zone|district|village|지역|산지|마을)\\s*[:：\\-]?\\s*", "");
        if (region.isBlank()) {
            return "";
        }

        String normalizedRegion = normalizeRegion(region);
        String aliasRegion = REGION_ALIASES.get(normalizedRegion);
        if (aliasRegion != null) {
            return aliasRegion;
        }

        return region;
    }

    private String bestProductNameCandidate(OcrPreprocessResult preprocessResult, String roastery) {
        if (preprocessResult == null || preprocessResult.productNameCandidates() == null) {
            return "";
        }

        return preprocessResult.productNameCandidates().stream()
                .map(this::clean)
                .filter(candidate -> !candidate.isBlank())
                .filter(candidate -> !candidate.equalsIgnoreCase(clean(roastery)))
                .findFirst()
                .orElse("");
    }

    private boolean looksLikeBeanName(String value) {
        String text = clean(value);
        return isCountry(text) || isProcessText(text)
                || text.matches(".*\\d{3,}\\s*(m|M|masl|MASL).*");
    }

    private boolean isKnownRoastery(String value) {
        return KNOWN_ROASTERIES.contains(clean(value).toUpperCase(Locale.ROOT));
    }

    private boolean isCountry(String value) {
        return !cleanCountry(value).isBlank();
    }

    private boolean isProcessText(String value) {
        return !clean(value).isBlank() && !cleanProcess(value).isBlank();
    }

    private String normalizeCountry(String value) {
        return clean(value)
                .replace("-", " ")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeRegion(String value) {
        return normalizeCountry(value).replace(" ", "");
    }

    private static Map<String, String> createCountryAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        for (CountryInfo countryInfo : CountryInfo.values()) {
            aliases.put(normalizeStatic(countryInfo.getEnglishName()), countryInfo.getKoreanName());
            aliases.put(normalizeStatic(countryInfo.getKoreanName()), countryInfo.getKoreanName());
            aliases.put(normalizeStatic(countryInfo.getCode()), countryInfo.getKoreanName());
        }

        aliases.put(normalizeStatic("Ethiiopia"), "에티오피아");
        aliases.put(normalizeStatic("Ethiopa"), "에티오피아");
        aliases.put(normalizeStatic("Etiopia"), "에티오피아");
        aliases.put(normalizeStatic("에디오피아"), "에티오피아");
        aliases.put(normalizeStatic("Columbia"), "콜롬비아");
        aliases.put(normalizeStatic("Costarica"), "코스타리카");
        aliases.put(normalizeStatic("Costa-Rica"), "코스타리카");
        aliases.put(normalizeStatic("Elsalvador"), "엘살바도르");
        aliases.put(normalizeStatic("El-Salvador"), "엘살바도르");
        aliases.put(normalizeStatic("PNG"), "파푸아뉴기니");
        aliases.put(normalizeStatic("Papua New-Guinea"), "파푸아뉴기니");
        aliases.put(normalizeStatic("DR Congo"), "콩고민주공화국");
        aliases.put(normalizeStatic("DRC"), "콩고민주공화국");
        aliases.put(normalizeStatic("Democratic Republic Congo"), "콩고민주공화국");
        aliases.put(normalizeStatic("Dominican Rep"), "도미니카공화국");
        aliases.put(normalizeStatic("Dominican"), "도미니카공화국");
        return Map.copyOf(aliases);
    }

    private static String normalizeStatic(String value) {
        return value == null ? "" : value.trim()
                .replace("-", " ")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private boolean containsCountry(String normalizedText, CountryInfo countryInfo) {
        String normalizedEnglishName = normalizeCountry(countryInfo.getEnglishName());
        String normalizedKoreanName = normalizeCountry(countryInfo.getKoreanName());
        String normalizedCode = normalizeCountry(countryInfo.getCode());
        return normalizedText.equals(normalizedEnglishName)
                || normalizedText.equals(normalizedKoreanName)
                || normalizedText.equals(normalizedCode)
                || normalizedText.contains(normalizedEnglishName)
                || normalizedText.contains(normalizedKoreanName);
    }
}
