package com.hsg.coffee.domain.coffeeBean.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.hsg.coffee.domain.llmparsing.service.OpenRouterChatClient;

import tools.jackson.databind.ObjectMapper;

class OpenRouterCustomFlavorNoteColorRecommenderTest {

    @Test
    void usesColorReturnedByOpenRouter() {
        OpenRouterChatClient chatClient = mock(OpenRouterChatClient.class);
        when(chatClient.isConfigured()).thenReturn(true);
        when(chatClient.complete(anyString(), eq(80))).thenReturn(new OpenRouterChatClient.Completion(
                "{}",
                "{\"color\":\"#a1b2c3\"}",
                "minimax/minimax-m3:free"
        ));

        OpenRouterCustomFlavorNoteColorRecommender recommender =
                new OpenRouterCustomFlavorNoteColorRecommender(chatClient, new ObjectMapper());

        assertThat(recommender.recommendColor("청포도")).isEqualTo("#A1B2C3");
    }

    @Test
    void usesLocalPaletteWhenOpenRouterIsUnavailable() {
        OpenRouterChatClient chatClient = mock(OpenRouterChatClient.class);
        when(chatClient.isConfigured()).thenReturn(true);
        when(chatClient.complete(anyString(), eq(80)))
                .thenThrow(new IllegalStateException("rate limited"));

        OpenRouterCustomFlavorNoteColorRecommender recommender =
                new OpenRouterCustomFlavorNoteColorRecommender(chatClient, new ObjectMapper());

        assertThat(recommender.recommendColor("오렌지 캔디")).isEqualTo("#F6A23A");
    }
}
