package com.hsg.coffee.domain.coffeeBean.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.hsg.coffee.domain.coffeeBean.entity.CustomFlavorNote;
import com.hsg.coffee.domain.coffeeBean.repository.CustomFlavorNoteRepository;

class CustomFlavorNoteServiceTest {

    @Test
    void retriesRecommendationForExistingGenericColor() {
        CustomFlavorNoteRepository repository = mock(CustomFlavorNoteRepository.class);
        CustomFlavorNoteColorRecommender recommender = mock(CustomFlavorNoteColorRecommender.class);
        CustomFlavorNote existing = CustomFlavorNote.of("핑크구아바", "핑크구아바", "#7A5038");
        when(repository.findByNormalizedName("핑크구아바")).thenReturn(Optional.of(existing));
        when(recommender.recommendColor("핑크구아바")).thenReturn("#E56F87");

        CustomFlavorNoteService service = new CustomFlavorNoteService(repository, recommender);
        service.ensureAll(List.of("핑크구아바"));

        assertThat(existing.getColor()).isEqualTo("#E56F87");
        verify(repository, never()).save(existing);
    }
}
