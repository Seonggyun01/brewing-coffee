package com.hsg.coffee.domain.llmparsing.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;
import com.hsg.coffee.domain.coffeeBean.service.FlavorNoteTextMapper;

class BeanOcrMappingValidatorTest {
    @Test void removesMetadataButKeepsUnlistedSensoryPhrases() {
        var response = new LlmParsingResponse("상품", "S", "에티오피아", "오로미아, 구지", "부쿠 사이사", "74110", "", "NATURAL", "", "", "", "", "",
                List.of("자두", "오로미아, 구지", "부쿠 사이사", "74110", "내추럴", "아사이베리", "스파클링", "화이트 그레이프 소다"));
        var result = new BeanOcrMappingValidator().sanitize(response);
        assertThat(result.roastery()).isEmpty();
        assertThat(result.flavorNotes()).contains("자두", "아사이베리", "스파클링", "화이트 그레이프 소다");
        assertThat(result.flavorNotes()).doesNotContain("오로미아", "구지", "부쿠 사이사", "74110", "내추럴", "배");
    }
    @Test void doesNotFuzzilyConvertCompoundNotes() {
        assertThat(FlavorNoteTextMapper.findExactFlavorNotes("화이트 그레이프 소다")).isEmpty();
        assertThat(FlavorNoteTextMapper.findExactFlavorNotes("아사이베리")).isEmpty();
    }
}
