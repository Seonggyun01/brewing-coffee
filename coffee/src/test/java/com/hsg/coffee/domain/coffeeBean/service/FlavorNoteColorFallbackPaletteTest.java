package com.hsg.coffee.domain.coffeeBean.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FlavorNoteColorFallbackPaletteTest {

    @Test
    void distinguishesTropicalFruitGrapeAndMilkyNotes() {
        assertThat(FlavorNoteColorFallbackPalette.resolve("핑크구아바")).isEqualTo("#E56F87");
        assertThat(FlavorNoteColorFallbackPalette.resolve("망고스틴")).isEqualTo("#814B73");
        assertThat(FlavorNoteColorFallbackPalette.resolve("밀키스")).isEqualTo("#9FC9C7");
        assertThat(FlavorNoteColorFallbackPalette.resolve("화이트 그레이프 소다")).isEqualTo("#A9BD5A");
    }

    @Test
    void replacesOnlyGenericStoredColorWithSpecificFallback() {
        assertThat(FlavorNoteColorFallbackPalette.replaceGenericDefault("핑크구아바", "#7A5038"))
                .isEqualTo("#E56F87");
        assertThat(FlavorNoteColorFallbackPalette.replaceGenericDefault("핑크구아바", "#123456"))
                .isEqualTo("#123456");
        assertThat(FlavorNoteColorFallbackPalette.replaceGenericDefault("알 수 없는 향", "#7A5038"))
                .isEqualTo("#7A5038");
    }
}
