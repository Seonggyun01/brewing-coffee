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
        assertThat(prompt).contains("응답 전 내부 검증");
        assertThat(prompt).contains("최종 JSON 하나만 출력한다");
        assertThat(prompt).contains("검증 과정이나 수정 내용은 출력하지 않는다");
        assertThat(prompt).contains("해당 원두 상품을 식별하는 대표 제목");
        assertThat(prompt).contains("원두 상품을 식별할 근거가 충분한 제목이 있으면 name을 비워두지 않는다");
        assertThat(prompt).doesNotContain("후보:");
        assertThat(prompt).doesNotContain("roastery=");
        assertThat(prompt).doesNotContain("keyValue=");
    }
}
