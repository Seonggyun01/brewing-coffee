package com.hsg.coffee.domain.llmparsing.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.hsg.coffee.domain.llmparsing.dto.KeyValueCandidate;
import com.hsg.coffee.domain.llmparsing.dto.OcrPreprocessResult;

class OcrTextPreprocessorTest {

    private final OcrTextPreprocessor preprocessor = new OcrTextPreprocessor();

    @Test
    void preprocessCloseCoffeeOcrText() {
        String rawText = """
                CLOSE COFFEE
                반코 첼첼레, 에티오피아
                예가체프 소킹 워시드
                COUNTRY
                VILLAGE
                VARIETY
                ETHIOPIA
                BANKO CHELCHELE
                74112.74110
                PROCESS
                WASHED
                TASTING NOTES
                ◎ 재스민
                ◎ 청포도, 복숭아
                캐모마일
                ◎말린망고
                ㅇ꿀, 배
                """;

        OcrPreprocessResult result = preprocessor.preprocess(rawText);

        assertThat(result.cleanedLines()).contains(
                "CLOSE COFFEE",
                "74112, 74110",
                "재스민",
                "꿀, 배"
        );
        assertThat(result.roasteryCandidates()).contains("CLOSE COFFEE");
        assertThat(result.productNameCandidates()).contains(
                "반코 첼첼레, 에티오피아",
                "예가체프 소킹 워시드",
                "반코 첼첼레, 에티오피아 예가체프 소킹 워시드"
        );
        assertThat(result.productNameCandidates()).doesNotContain("ETHIOPIA", "WASHED");
        assertThat(result.keyValueCandidates()).contains(
                new KeyValueCandidate("COUNTRY", "ETHIOPIA"),
                new KeyValueCandidate("VILLAGE", "BANKO CHELCHELE"),
                new KeyValueCandidate("VARIETY", "74112, 74110"),
                new KeyValueCandidate("PROCESS", "WASHED")
        );
        assertThat(result.keyValueCandidates()).doesNotContain(
                new KeyValueCandidate("VARIETY", "ETHIOPIA"),
                new KeyValueCandidate("TASTING NOTES", "재스민")
        );
        assertThat(result.tastingNoteCandidates()).containsExactly(
                "재스민",
                "청포도",
                "복숭아",
                "캐모마일",
                "말린망고",
                "꿀",
                "배"
        );
        assertThat(result.promptText()).contains(
                "[RAW_LINES]",
                "[ROASTERY_CANDIDATES]",
                "[PRODUCT_NAME_CANDIDATES]",
                "[KEY_VALUE_CANDIDATES]",
                "[TASTING_NOTE_CANDIDATES]"
        );
    }
}
