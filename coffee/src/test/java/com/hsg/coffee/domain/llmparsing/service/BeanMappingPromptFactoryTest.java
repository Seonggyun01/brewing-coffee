package com.hsg.coffee.domain.llmparsing.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BeanMappingPromptFactoryTest {

    private final BeanMappingPromptFactory promptFactory =
            new BeanMappingPromptFactory(new OcrTextPreprocessor());

    @Test
    void sendsInstructionsAndRawOcrTextWithoutDerivedCandidates() {
        String rawOcrText = "INDONESIA IJEN\nProcess: Anaerobic Natural";

        String prompt = promptFactory.createPrompt(rawOcrText);

        assertThat(prompt).contains(rawOcrText);
        assertThat(prompt).contains("반환 JSON 형식");
        assertThat(prompt).contains("최종 JSON 하나만 출력한다");
        assertThat(prompt).contains("해당 원두 상품을 식별하는 대표 제목");
        assertThat(prompt).contains("원두 상품을 식별할 근거가 충분한 제목이 있으면 name을 비워두지 않는다");
        assertThat(prompt).doesNotContain("후보:");
        assertThat(prompt).doesNotContain("roastery=");
        assertThat(prompt).doesNotContain("keyValue=");
    }
    @Test
    void explainsConservativeNamingAndFlavorBoundaries() {
        String prompt = promptFactory.createPrompt("판독 불가 로고\n지역: 오로미아, 구지");
        assertThat(prompt).contains("[생산국/생산지] + [지역] + [농장/워싱스테이션/로트·등급] + [품종] + [가공 방식]");
        assertThat(prompt).contains("원문에서 확인된 요소만 사용");
        assertThat(prompt).contains("flavorNotes 엄격 추출 규칙");
        assertThat(prompt).contains("확실한 향미만 최대 8개");
        assertThat(prompt).contains("한 글자 브랜드");
    }

}
