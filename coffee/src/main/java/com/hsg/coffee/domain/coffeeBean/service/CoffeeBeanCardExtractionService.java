package com.hsg.coffee.domain.coffeeBean.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCardExtractResult;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCardTextParseResult;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CoffeeBeanCardExtractionService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private final CoffeeBeanCardOcrService ocrService;
    private final CoffeeBeanCardTextParser textParser;

    public CoffeeBeanCardExtractResult extract(MultipartFile image) {
        validateImage(image);

        String rawText = ocrService.extractText(image);
        CoffeeBeanCardTextParseResult parseResult = textParser.parse(rawText);

        return new CoffeeBeanCardExtractResult(
                rawText,
                toCreateForm(parseResult),
                parseResult.getWarnings()
        );
    }

    private CoffeeBeanCreateForm toCreateForm(CoffeeBeanCardTextParseResult parseResult) {
        CoffeeBeanCreateForm form = new CoffeeBeanCreateForm();
        form.setName(parseResult.getName());
        form.setRoastery(parseResult.getRoastery());
        form.setCountry(parseResult.getCountry());
        form.setOriginCountryCode(parseResult.getOriginCountryCode());
        form.setRegion(parseResult.getRegion());
        form.setFarm(parseResult.getFarm());
        form.setVariety(parseResult.getVariety());
        form.setAltitude(parseResult.getAltitude());
        form.setProcessType(parseResult.getProcessType());
        form.setFlavorNotes(parseResult.getFlavorNotes());
        form.setCustomFlavorNotesText(parseResult.getCustomFlavorNotesText());
        form.setWeight(parseResult.getWeight());
        form.setPrice(parseResult.getPrice());
        form.setRoastedDate(parseResult.getRoastedDate());
        return form;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("원두 카드 이미지를 업로드해주세요.");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지 파일은 5MB 이하만 업로드할 수 있습니다.");
        }
    }
}
