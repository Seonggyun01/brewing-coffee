package com.hsg.coffee.domain.coffeeBean.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCardExtractResult;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCardTextParseResult;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;
import com.hsg.coffee.domain.llmparsing.service.HuggingFaceBeanMappingService;
import com.hsg.coffee.global.country.CountryInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CoffeeBeanCardExtractionService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg",
            "jpeg",
            "png",
            "webp",
            "heic",
            "heif"
    );
    private static final Map<String, FlavorNote> FLAVOR_NOTE_ALIASES = createFlavorNoteAliases();

    private final CoffeeBeanCardOcrService ocrService;
    private final CoffeeBeanCardTextParser textParser;
    private final HuggingFaceBeanMappingService huggingFaceBeanMappingService;

    public CoffeeBeanCardExtractResult extract(MultipartFile image) {
        validateImage(image);

        String rawText = ocrService.extractText(image);
        CoffeeBeanCardTextParseResult parseResult = textParser.parse(rawText);
        LlmParsingResponse llmResult = huggingFaceBeanMappingService.parseOcrText(rawText);

        return new CoffeeBeanCardExtractResult(
                rawText,
                toCreateForm(parseResult, llmResult),
                parseResult.getWarnings()
        );
    }

    private CoffeeBeanCreateForm toCreateForm(CoffeeBeanCardTextParseResult parseResult, LlmParsingResponse llmResult) {
        CoffeeBeanCreateForm form = new CoffeeBeanCreateForm();
        form.setName(firstText(llmResult.name(), parseResult.getName()));
        form.setRoastery(firstText(llmResult.roastery(), parseResult.getRoastery()));
        applyCountry(form, parseResult, llmResult);
        form.setRegion(firstText(llmResult.region(), parseResult.getRegion()));
        form.setFarm(firstText(llmResult.farmOrStation(), parseResult.getFarm()));
        form.setVariety(firstText(llmResult.variety(), parseResult.getVariety()));
        form.setAltitude(firstText(llmResult.altitude(), parseResult.getAltitude()));
        form.setProcessType(parseProcessType(llmResult.process(), parseResult.getProcessType()));
        applyFlavorNotes(form, parseResult, llmResult);
        form.setWeight(parseInteger(llmResult.remainingWeightGram(), parseResult.getWeight()));
        form.setPrice(parseInteger(llmResult.price(), parseResult.getPrice()));
        form.setRoastedDate(parseDate(llmResult.roastedAt(), parseResult.getRoastedDate()));
        return form;
    }

    private void applyCountry(
            CoffeeBeanCreateForm form,
            CoffeeBeanCardTextParseResult parseResult,
            LlmParsingResponse llmResult
    ) {
        String llmCountry = cleanText(llmResult.originCountry());
        if (StringUtils.hasText(llmCountry)) {
            String countryCode = CountryInfo.findCodeByName(llmCountry);
            CountryInfo countryInfo = CountryInfo.findByCode(countryCode);
            form.setOriginCountryCode(countryCode);
            form.setCountry(countryInfo != null ? countryInfo.getEnglishName() : llmCountry);
            return;
        }

        form.setCountry(parseResult.getCountry());
        form.setOriginCountryCode(parseResult.getOriginCountryCode());
    }

    private void applyFlavorNotes(
            CoffeeBeanCreateForm form,
            CoffeeBeanCardTextParseResult parseResult,
            LlmParsingResponse llmResult
    ) {
        if (llmResult.flavorNotes() == null || llmResult.flavorNotes().isEmpty()) {
            form.setFlavorNotes(parseResult.getFlavorNotes());
            form.setCustomFlavorNotesText(parseResult.getCustomFlavorNotesText());
            return;
        }

        Set<FlavorNote> matchedNotes = new LinkedHashSet<>();
        Set<String> customNotes = new LinkedHashSet<>();

        for (String flavorNoteText : llmResult.flavorNotes()) {
            String cleanNoteText = cleanText(flavorNoteText);
            if (!StringUtils.hasText(cleanNoteText)) {
                continue;
            }

            List<FlavorNote> foundNotes = findFlavorNotes(cleanNoteText);
            if (foundNotes.isEmpty()) {
                customNotes.add(cleanNoteText);
            } else {
                matchedNotes.addAll(foundNotes);
            }
        }

        form.setFlavorNotes(new ArrayList<>(matchedNotes));
        form.setCustomFlavorNotesText(customNotes.isEmpty() ? null : String.join(", ", customNotes));
    }

    private List<FlavorNote> findFlavorNotes(String text) {
        String normalizedText = normalizeFlavorKeyword(text);
        if (!StringUtils.hasText(normalizedText)) {
            return List.of();
        }

        Set<FlavorNote> notes = new LinkedHashSet<>();
        FlavorNote aliasNote = FLAVOR_NOTE_ALIASES.get(normalizedText);
        if (aliasNote != null) {
            notes.add(aliasNote);
        }

        for (FlavorNote note : FlavorNote.values()) {
            String normalizedDisplayName = normalizeFlavorKeyword(note.getDisplayName());
            String normalizedEnumName = normalizeFlavorKeyword(note.name());
            if (normalizedText.contains(normalizedDisplayName) || normalizedText.contains(normalizedEnumName)) {
                notes.add(note);
            }
        }

        removeSubsumedFlavorNotes(notes);
        return new ArrayList<>(notes);
    }

    private void removeSubsumedFlavorNotes(Set<FlavorNote> notes) {
        Set<FlavorNote> notesToRemove = new LinkedHashSet<>();

        for (FlavorNote note : notes) {
            String keyword = normalizeFlavorKeyword(note.name());
            for (FlavorNote otherNote : notes) {
                if (note == otherNote) {
                    continue;
                }

                String otherKeyword = normalizeFlavorKeyword(otherNote.name());
                if (otherKeyword.contains(keyword)) {
                    notesToRemove.add(note);
                    break;
                }
            }
        }

        notes.removeAll(notesToRemove);
    }

    private ProcessType parseProcessType(String value, ProcessType fallback) {
        String cleanValue = cleanText(value);
        if (!StringUtils.hasText(cleanValue)) {
            return fallback;
        }

        try {
            return ProcessType.valueOf(cleanValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private Integer parseInteger(String value, Integer fallback) {
        String cleanValue = cleanText(value);
        if (!StringUtils.hasText(cleanValue)) {
            return fallback;
        }

        String digits = cleanValue.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return fallback;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        String cleanValue = cleanText(value);
        if (!StringUtils.hasText(cleanValue)) {
            return fallback;
        }

        try {
            return LocalDate.parse(cleanValue);
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }

    private String firstText(String primary, String fallback) {
        String cleanPrimary = cleanText(primary);
        return StringUtils.hasText(cleanPrimary) ? cleanPrimary : fallback;
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String normalizeFlavorKeyword(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_\\-·,/|()\\[\\]{}]+", "");
    }

    private static Map<String, FlavorNote> createFlavorNoteAliases() {
        Map<String, FlavorNote> aliases = new LinkedHashMap<>();
        aliases.put("허니", FlavorNote.HONEY);
        aliases.put("밀크초콜릿", FlavorNote.MILK_CHOCOLATE);
        aliases.put("다크초콜릿", FlavorNote.DARK_CHOCOLATE);
        aliases.put("레드와인", FlavorNote.RED_WINE);
        aliases.put("화이트와인", FlavorNote.WHITE_WINE);
        aliases.put("청포도", FlavorNote.GRAPE);
        aliases.put("오렌지주스", FlavorNote.ORANGE);
        aliases.put("오렌지쥬스", FlavorNote.ORANGE);
        aliases.put("자스민티", FlavorNote.JASMINE_TEA);
        aliases.put("얼그레이티", FlavorNote.EARL_GREY);
        aliases.put("블랙티", FlavorNote.BLACK_TEA);
        aliases.put("그린티", FlavorNote.GREEN_TEA);
        return aliases;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("원두 카드 이미지를 업로드해주세요.");
        }

        String contentType = image.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("JPG, PNG, WEBP, HEIC 이미지만 업로드할 수 있습니다.");
        }

        String extension = extractExtension(image.getOriginalFilename());
        if (StringUtils.hasText(extension) && !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("JPG, PNG, WEBP, HEIC 이미지만 업로드할 수 있습니다.");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지 파일은 5MB 이하만 업로드할 수 있습니다.");
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
