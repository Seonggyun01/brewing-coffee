package com.hsg.coffee.domain.coffeeBean.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;

class FlavorGradientStyleTest {

    @Test
    void invalidCustomColorsAreIgnoredWithoutBreakingRendering() {
        String style = FlavorGradientStyle.from(
                List.of(FlavorNote.LEMON),
                java.util.Arrays.asList(
                        new CustomFlavorNoteResponse("색상 없음", null),
                        new CustomFlavorNoteResponse("잘못된 색상", "brown")
                )
        );

        assertEquals("background: #F6C945;", style);
    }

    @Test
    void emptyColorsUseTheDefaultColor() {
        assertEquals("background: #DED4C8;", FlavorGradientStyle.from(List.of(), List.of()));
    }

    @Test
    void multipleColorsCreateAGradient() {
        String style = FlavorGradientStyle.from(
                List.of(FlavorNote.JASMINE, FlavorNote.LEMON),
                List.of(new CustomFlavorNoteResponse("커스텀", "#7a5038"))
        );

        assertEquals(
                "background: linear-gradient(90deg, #C8A2C8, #F6C945, #7A5038);",
                style
        );
    }
}
