package com.hsg.coffee.domain.llmparsing.service;

import org.springframework.stereotype.Component;

import com.hsg.coffee.domain.llmparsing.dto.OcrPreprocessResult;
import com.hsg.coffee.global.country.CountryInfo;

@Component
public class BeanMappingPromptFactory {

    private final OcrTextPreprocessor preprocessor;

    public BeanMappingPromptFactory(OcrTextPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public OcrPreprocessResult preprocess(String ocrText) {
        return preprocessor.preprocess(ocrText);
    }

    public String createPrompt(String ocrText) {
        return createPrompt(preprocess(ocrText));
    }

    public String createPrompt(OcrPreprocessResult preprocessResult) {
        return """
                너는 BrewLog 백엔드의 "원두 카드 OCR 텍스트 -> 원두 등록 JSON" 변환 함수다.

                기능 요구사항:
                1. OCR 텍스트에는 오탈자, 줄바꿈 오류, 손글씨, 불필요한 안내 문구가 섞여 있다.
                2. OCR 원문에 근거가 있는 값만 채우고, 근거가 약하면 빈 문자열 "" 또는 빈 배열 []로 둔다.
                3. 반드시 JSON 하나만 출력한다. 설명, 마크다운, 주석, 코드블록은 금지한다.
                4. roastery는 로스터리/브랜드/카페명이고, name은 원두 상품명이다. 둘은 같으면 안 된다.
                5. originCountry는 생산 국가만 쓰고, 가능하면 한국어 국가명으로 쓴다.
                6. originCountry는 다음 목록 중 하나로 매핑한다: %s.
                7. region은 국가명이 아니라 Guji, Yirgacheffe, Huila, Boquete 같은 산지/지역/마을명만 쓴다.
                8. farmOrStation은 농장/스테이션/생산자명만 쓴다.
                9. process는 NATURAL, WASHED, HONEY, ANAEROBIC, DECAF, OTHER, "" 중 하나만 쓴다.
                10. altitude는 ALTITUDE, ELEVATION, MASL, 해발고도, 고도, m 같은 고도 근거가 있을 때만 쓴다.
                11. 74110, 74112, 74158 같은 에티오피아 품종 번호는 variety에 쓰고 altitude에는 쓰지 않는다.
                12. remainingWeightGram은 손글씨 또는 라벨의 남은 중량을 숫자만 문자열로 쓴다. 예: "100".
                13. flavorNotes는 원문 표현을 유지하되 향미 단위로 쪼개서 배열로 쓴다.
                14. 보관방법, 설명문, 문장형 맛 설명은 필드에 억지로 넣지 않는다.

                후보:
                roastery=%s
                name=%s
                keyValue=%s
                flavorNotes=%s

                반환 JSON 형식:
                {"name":"","roastery":"","originCountry":"","region":"","farmOrStation":"","variety":"","altitude":"","process":"","beanStatus":"","roastedAt":"","purchasedAt":"","price":"","remainingWeightGram":"","flavorNotes":[]}

                OCR 텍스트:
                ---
                %s
                ---
                """.formatted(
                allowedCountryNames(),
                String.join(", ", preprocessResult.roasteryCandidates()),
                String.join(", ", preprocessResult.productNameCandidates()),
                formatKeyValueCandidates(preprocessResult),
                String.join(", ", preprocessResult.tastingNoteCandidates()),
                preprocessResult.rawText()
        );
    }

    private String allowedCountryNames() {
        return java.util.Arrays.stream(CountryInfo.values())
                .map(CountryInfo::getKoreanName)
                .toList()
                .toString();
    }

    private String formatKeyValueCandidates(OcrPreprocessResult preprocessResult) {
        return preprocessResult.keyValueCandidates().stream()
                .map(candidate -> candidate.key() + " -> " + candidate.value())
                .toList()
                .toString();
    }
}
