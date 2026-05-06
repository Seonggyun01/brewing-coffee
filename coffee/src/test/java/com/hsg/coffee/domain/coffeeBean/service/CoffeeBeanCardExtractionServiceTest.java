package com.hsg.coffee.domain.coffeeBean.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCardExtractResult;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCardTextParseResult;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;

class CoffeeBeanCardExtractionServiceTest {

    private CoffeeBeanCardExtractionService extractionService;

    @BeforeEach
    void setUp() {
        extractionService = new CoffeeBeanCardExtractionService(
                new MockCoffeeBeanCardOcrService(),
                new CoffeeBeanCardTextParser()
        );
    }

    @Test
    void extractWithMockOcr() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "coffee-card.jpg",
                "image/jpeg",
                "mock image".getBytes()
        );

        CoffeeBeanCardExtractResult result = extractionService.extract(image);
        CoffeeBeanCreateForm form = result.getForm();

        System.out.println("=== 원두 카드 Mock OCR 추출 테스트 결과 ===");
        System.out.println("OCR 원문: " + result.getRawText().replace("\n", " / "));
        System.out.println("원두명: " + form.getName());
        System.out.println("로스터리: " + form.getRoastery());
        System.out.println("국가: " + form.getCountry());
        System.out.println("가공 방식: " + form.getProcessType());
        System.out.println("향미 노트: " + form.getFlavorNotes());
        System.out.println("용량: " + form.getWeight());
        System.out.println("가격: " + form.getPrice());
        System.out.println("로스팅일: " + form.getRoastedDate());

        assertTrue(result.getRawText().contains("Ethiopia Yirgacheffe Kochere"));
        assertEquals("Ethiopia Yirgacheffe Kochere", form.getName());
        assertEquals("Fritz Coffee", form.getRoastery());
        assertEquals("Ethiopia", form.getCountry());
        assertEquals("ET", form.getOriginCountryCode());
        assertEquals(ProcessType.WASHED, form.getProcessType());
        assertEquals(List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH), form.getFlavorNotes());
        assertEquals(200, form.getWeight());
        assertEquals(18000, form.getPrice());
        assertEquals(LocalDate.of(2026, 5, 1), form.getRoastedDate());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void rejectEmptyImage() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractionService.extract(image)
        );

        assertEquals("원두 카드 이미지를 업로드해주세요.", exception.getMessage());
    }

    @Test
    void rejectNonImageFile() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "coffee-card.txt",
                "text/plain",
                "not image".getBytes()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractionService.extract(image)
        );

        assertEquals("이미지 파일만 업로드할 수 있습니다.", exception.getMessage());
    }

    @Test
    void rejectTooLargeImage() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "large-card.jpg",
                "image/jpeg",
                new byte[5 * 1024 * 1024 + 1]
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractionService.extract(image)
        );

        assertEquals("이미지 파일은 5MB 이하만 업로드할 수 있습니다.", exception.getMessage());
    }

    @Test
    void parseEmptyTextAsWarning() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        var result = parser.parse("   ");

        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().getFirst().contains("텍스트를 추출하지 못했어요"));
    }

    @Test
    void parseKoreanCountryAndFlavorNotes() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        CoffeeBeanCardTextParseResult result = parser.parse("""
                에티오피아 예가체프 코체레
                워시드
                자스민, 레몬, 복숭아
                """);

        assertEquals("에티오피아 예가체프 코체레", result.getName());
        assertEquals("Ethiopia", result.getCountry());
        assertEquals("ET", result.getOriginCountryCode());
        assertEquals(ProcessType.WASHED, result.getProcessType());
        assertEquals(List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH), result.getFlavorNotes());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void parseAnaerobicBeforeNatural() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        CoffeeBeanCardTextParseResult result = parser.parse("""
                Colombia El Paraiso
                Anaerobic Natural
                Strawberry, Mango
                """);

        assertEquals("Colombia El Paraiso", result.getName());
        assertEquals("Colombia", result.getCountry());
        assertEquals("CO", result.getOriginCountryCode());
        assertEquals(ProcessType.ANAEROBIC, result.getProcessType());
        assertEquals(List.of(FlavorNote.STRAWBERRY, FlavorNote.MANGO), result.getFlavorNotes());
    }

    @Test
    void skipMetadataLinesWhenExtractingName() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        CoffeeBeanCardTextParseResult result = parser.parse("""
                200g
                Roasted 2026.05.01
                18,000 KRW
                Kenya Kirinyaga
                Washed
                Black Tea, Brown Sugar
                """);

        assertEquals("Kenya Kirinyaga", result.getName());
        assertEquals("Kenya", result.getCountry());
        assertEquals("KE", result.getOriginCountryCode());
        assertEquals(200, result.getWeight());
        assertEquals(18000, result.getPrice());
        assertEquals(LocalDate.of(2026, 5, 1), result.getRoastedDate());
        assertEquals(List.of(FlavorNote.BROWN_SUGAR, FlavorNote.BLACK_TEA), result.getFlavorNotes());
    }

    @Test
    void parseKgWeightWonPriceAndHyphenDate() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        CoffeeBeanCardTextParseResult result = parser.parse("""
                Brazil Fazenda Santa Ines
                Natural
                1kg
                ₩42,000
                Roasted 2026-05-03
                Milk Chocolate, Almond
                """);

        assertEquals("Brazil Fazenda Santa Ines", result.getName());
        assertEquals("Brazil", result.getCountry());
        assertEquals("BR", result.getOriginCountryCode());
        assertEquals(ProcessType.NATURAL, result.getProcessType());
        assertEquals(1000, result.getWeight());
        assertEquals(42000, result.getPrice());
        assertEquals(LocalDate.of(2026, 5, 3), result.getRoastedDate());
        assertEquals(List.of(FlavorNote.MILK_CHOCOLATE, FlavorNote.ALMOND), result.getFlavorNotes());
    }

    @Test
    void parseLabeledRoasteryOriginDetailsAndTwoDigitRoastedDate() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        CoffeeBeanCardTextParseResult result = parser.parse("""
                Roastery: Sample Roasters
                Colombia La Palma
                Region: Cundinamarca
                Farm: La Palma y El Tucan
                Variety: Gesha
                Carbonic Maceration
                26.05.04
                White Grape, Cane Sugar, Yuzu
                """);

        assertEquals("Colombia La Palma", result.getName());
        assertEquals("Sample Roasters", result.getRoastery());
        assertEquals("Colombia", result.getCountry());
        assertEquals("CO", result.getOriginCountryCode());
        assertEquals("Cundinamarca", result.getRegion());
        assertEquals("La Palma y El Tucan", result.getFarm());
        assertEquals("Gesha", result.getVariety());
        assertEquals(ProcessType.CARBONIC_MACERATION, result.getProcessType());
        assertEquals(LocalDate.of(2026, 5, 4), result.getRoastedDate());
        assertEquals(List.of(FlavorNote.YUZU, FlavorNote.GRAPE), result.getFlavorNotes());
        assertEquals("Cane Sugar", result.getCustomFlavorNotesText());
    }

    @Test
    void parseOriginLineAltitudeAndProducerLabels() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        CoffeeBeanCardTextParseResult result = parser.parse("""
                Ethiopia Hambela Buku
                Origin: Ethiopia / Guji / Hambela Washing Station
                Producer: METAD
                Altitude: 1,900 - 2,100 masl
                Heirloom
                Washed
                Earl Grey, Bergamot, White Peach
                """);

        assertEquals("Ethiopia Hambela Buku", result.getName());
        assertEquals("Ethiopia", result.getCountry());
        assertEquals("ET", result.getOriginCountryCode());
        assertEquals("Guji", result.getRegion());
        assertEquals("METAD", result.getFarm());
        assertEquals("1,900 - 2,100 masl", result.getAltitude());
        assertEquals(ProcessType.WASHED, result.getProcessType());
        assertEquals(List.of(FlavorNote.WHITE_PEACH, FlavorNote.EARL_GREY), result.getFlavorNotes());
        assertEquals("Bergamot", result.getCustomFlavorNotesText());
    }

    @Test
    void parseKoreanOriginAndAltitudeLabels() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        CoffeeBeanCardTextParseResult result = parser.parse("""
                케냐 키린야가 기추구
                원산지: 케냐 / Kirinyaga / Gichugu
                고도: 1800m
                품종: SL28, SL34
                워시드
                블랙커런트, 자몽, 흑설탕
                """);

        assertEquals("케냐 키린야가 기추구", result.getName());
        assertEquals("Kenya", result.getCountry());
        assertEquals("KE", result.getOriginCountryCode());
        assertEquals("Kirinyaga", result.getRegion());
        assertEquals("Gichugu", result.getFarm());
        assertEquals("1800m", result.getAltitude());
        assertEquals("SL28, SL34", result.getVariety());
        assertEquals(ProcessType.WASHED, result.getProcessType());
        assertEquals(List.of(FlavorNote.GRAPEFRUIT, FlavorNote.BLACKCURRANT, FlavorNote.BROWN_SUGAR), result.getFlavorNotes());
        assertNull(result.getCustomFlavorNotesText());
    }

    @Test
    void preferDateNearRoastedKeywordWhenSeveralDatesExist() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        CoffeeBeanCardTextParseResult result = parser.parse("""
                Guatemala El Injerto
                Purchased 2026.05.10
                Roasted 2026.05.02
                Washed
                Orange, Caramel
                """);

        assertEquals("Guatemala El Injerto", result.getName());
        assertEquals("Guatemala", result.getCountry());
        assertEquals("GT", result.getOriginCountryCode());
        assertEquals(LocalDate.of(2026, 5, 2), result.getRoastedDate());
        assertEquals(List.of(FlavorNote.ORANGE, FlavorNote.CARAMEL), result.getFlavorNotes());
    }

    @Test
    void addWarningWhenCountryIsMissing() {
        CoffeeBeanCardTextParser parser = new CoffeeBeanCardTextParser();

        CoffeeBeanCardTextParseResult result = parser.parse("""
                Mystery Lot 03
                Honey
                Orange, Caramel
                """);

        assertEquals("Mystery Lot 03", result.getName());
        assertNull(result.getCountry());
        assertNull(result.getOriginCountryCode());
        assertEquals(ProcessType.HONEY, result.getProcessType());
        assertTrue(result.getWarnings().stream()
                .anyMatch(warning -> warning.contains("원산지 국가를 자동으로 찾지 못했어요")));
    }

    @Test
    void rejectImageWithoutContentType() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "coffee-card",
                null,
                "mock image".getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractionService.extract(image)
        );

        assertEquals("이미지 파일만 업로드할 수 있습니다.", exception.getMessage());
    }
}
