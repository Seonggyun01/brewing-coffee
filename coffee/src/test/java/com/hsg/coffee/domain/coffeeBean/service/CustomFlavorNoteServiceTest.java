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
        when(recommender.recommendColors(List.of("핑크구아바"))).thenReturn(java.util.Map.of("핑크구아바", "#E56F87"));

        CustomFlavorNoteService service = new CustomFlavorNoteService(repository, recommender);
        assertThat(service.ensureAll(List.of("핑크구아바", "핑크 구아바")))
                .containsExactly("핑크구아바");

        assertThat(existing.getColor()).isEqualTo("#E56F87");
        verify(repository, never()).save(existing);
    }

    @Test
    void batchesOnlyMissingColorsAndReusesSavedColors() {
        var repository = mock(CustomFlavorNoteRepository.class);
        var recommender = mock(CustomFlavorNoteColorRecommender.class);
        when(repository.findByNormalizedName("청포도")).thenReturn(Optional.of(
                CustomFlavorNote.of("청포도", "청포도", "#A1B2C3")));
        when(repository.findByNormalizedName("핑크구아바")).thenReturn(Optional.empty());
        when(repository.findByNormalizedName("체리주스")).thenReturn(Optional.empty());
        when(recommender.recommendColors(List.of("핑크구아바", "체리 주스")))
                .thenReturn(java.util.Map.of("핑크구아바", "#E56F87", "체리 주스", "#D94B6A"));
        var service = new CustomFlavorNoteService(repository, recommender);
        service.ensureAll(List.of("청포도", "핑크구아바", "체리 주스"));
        verify(recommender).recommendColors(List.of("핑크구아바", "체리 주스"));
        verify(recommender, never()).recommendColor(org.mockito.ArgumentMatchers.anyString());
        var capture = org.mockito.ArgumentCaptor.forClass(CustomFlavorNote.class);
        verify(repository, org.mockito.Mockito.times(2)).save(capture.capture());
        assertThat(capture.getAllValues()).extracting(CustomFlavorNote::getColor)
                .containsExactly("#E56F87", "#D94B6A");
    }

    @Test
    void doesNotRequestColorsWhenAllAreCached() {
        var repository = mock(CustomFlavorNoteRepository.class);
        var recommender = mock(CustomFlavorNoteColorRecommender.class);
        when(repository.findByNormalizedName("청포도")).thenReturn(Optional.of(
                CustomFlavorNote.of("청포도", "청포도", "#A1B2C3")));
        new CustomFlavorNoteService(repository, recommender).ensureAll(List.of("청포도"));
        org.mockito.Mockito.verifyNoInteractions(recommender);
    }
}
