package com.hsg.coffee.domain.coffeeBean.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

            List<FlavorNote> foundNotes = FlavorNoteTextMapper.findFlavorNotes(cleanNoteText);
            if (foundNotes.isEmpty()) {
                customNotes.add(cleanNoteText);
            } else {
                matchedNotes.addAll(foundNotes);
            }
        }

        form.setFlavorNotes(new ArrayList<>(matchedNotes));
        form.setCustomFlavorNotesText(customNotes.isEmpty() ? null : String.join(", ", customNotes));
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

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("원두 카드 이미지를 업로드해주세요.");
        }

        String contentType = image.getContentType();
        String extension = extractExtension(image.getOriginalFilename());
        boolean allowedContentType = StringUtils.hasText(contentType)
                && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
        boolean allowedExtension = StringUtils.hasText(extension)
                && ALLOWED_EXTENSIONS.contains(extension);

        if (!allowedContentType && !allowedExtension) {
            throw new IllegalArgumentException("JPG, PNG, WEBP, HEIC 이미지만 업로드할 수 있습니다.");
        }

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
