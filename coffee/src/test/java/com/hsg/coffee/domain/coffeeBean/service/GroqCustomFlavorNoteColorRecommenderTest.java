package com.hsg.coffee.domain.coffeeBean.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import com.hsg.coffee.domain.llmparsing.service.GroqChatClient;
import com.hsg.coffee.domain.llmparsing.service.OpenRouterChatClient;
import tools.jackson.databind.ObjectMapper;

class GroqCustomFlavorNoteColorRecommenderTest {
    @Test
    void usesGroqColorWithSmallOutputBudget() {
        var client = mock(GroqChatClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.complete(anyString(), eq(96))).thenReturn(
                new OpenRouterChatClient.Completion("{}", "{\"color\":\"#a1b2c3\"}", "qwen/qwen3.8-27b"));
        var recommender = new GroqCustomFlavorNoteColorRecommender(client, new ObjectMapper());
        assertThat(recommender.recommendColor("청포도")).isEqualTo("#A1B2C3");
        verify(client).complete(anyString(), eq(96));
    }

    @Test
    void usesPaletteOnProviderFailure() {
        var client = mock(GroqChatClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.complete(anyString(), eq(96))).thenThrow(new IllegalStateException("rate limited"));
        var recommender = new GroqCustomFlavorNoteColorRecommender(client, new ObjectMapper());
        assertThat(recommender.recommendColor("오렌지 캔디")).isEqualTo("#F6A23A");
    }

    @Test
    void rejectsInvalidColor() {
        var client = mock(GroqChatClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.complete(anyString(), eq(96))).thenReturn(
                new OpenRouterChatClient.Completion("{}", "{\"color\":\"not-a-color\"}", "qwen/qwen3.8-27b"));
        var recommender = new GroqCustomFlavorNoteColorRecommender(client, new ObjectMapper());
        assertThat(recommender.recommendColor("오렌지 캔디")).isEqualTo("#F6A23A");
    }

    @Test
    void returnsMultipleFlavorColorsInOneRequestWithDynamicBudget() {
        var client = mock(GroqChatClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.complete(anyString(), eq(256))).thenReturn(new OpenRouterChatClient.Completion(
                "{}", "{\"colors\":{\"청포도\":\"#a1b2c3\",\"체리 주스\":\"#d94b6a\",\"민트\":\"invalid\",\"추가\":\"#ffffff\"}}", "qwen/qwen3.8-27b"));
        var result = new GroqCustomFlavorNoteColorRecommender(client, new ObjectMapper())
                .recommendColors(java.util.List.of("청포도", "체리 주스", "민트"));
        assertThat(result).hasSize(3).containsEntry("청포도", "#A1B2C3").containsEntry("체리 주스", "#D94B6A");
        assertThat(result.get("민트")).isEqualTo(FlavorNoteColorFallbackPalette.resolve("민트"));
        verify(client, times(1)).complete(anyString(), eq(256));
    }

    @Test
    void fallsBackPerFlavorWithoutRetryingOnBatchFailure() {
        var client = mock(GroqChatClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.complete(anyString(), eq(192))).thenThrow(new IllegalStateException("rate limited"));
        var result = new GroqCustomFlavorNoteColorRecommender(client, new ObjectMapper())
                .recommendColors(java.util.List.of("청포도", "민트"));
        assertThat(result).containsEntry("청포도", FlavorNoteColorFallbackPalette.resolve("청포도"))
                .containsEntry("민트", FlavorNoteColorFallbackPalette.resolve("민트"));
        verify(client, times(1)).complete(anyString(), eq(192));
    }
}
