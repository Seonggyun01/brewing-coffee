package com.hsg.coffee.domain.llmparsing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;

import tools.jackson.databind.ObjectMapper;

class OpenRouterBeanMappingServiceTest {

    @Test
    void parsesAndSanitizesOpenRouterJsonResponse() {
        OpenRouterChatClient chatClient = mock(OpenRouterChatClient.class);
        when(chatClient.isConfigured()).thenReturn(true);
        when(chatClient.complete(anyString())).thenReturn(new OpenRouterChatClient.Completion(
                "{}",
                """
                        {"name":"Guji Hambela","roastery":"Fritz Coffee","originCountry":"Ethiopia","region":"Guji","farmOrStation":"Hambela","variety":"74110","altitude":"2100m","process":"WASHED","beanStatus":"CURRENT","roastedAt":"2026-08-20","purchasedAt":"","price":"18000원","remainingWeightGram":"200g","flavorNotes":["Jasmine","Peach"]}
                        """,
                "minimax/minimax-m3:free"
        ));

        OpenRouterBeanMappingService service = new OpenRouterBeanMappingService(
                chatClient,
                new ObjectMapper(),
                new BeanOcrMappingValidator(),
                new BeanMappingPromptFactory(new OcrTextPreprocessor())
        );

        LlmParsingResponse result = service.parseOcrText("ETHIOPIA GUJI HAMBELA WASHED JASMINE PEACH");

        assertThat(result.name()).isEqualTo("Guji Hambela");
        assertThat(result.roastery()).isEqualTo("Fritz Coffee");
        assertThat(result.originCountry()).isEqualTo("에티오피아");
        assertThat(result.process()).isEqualTo("WASHED");
        assertThat(result.price()).isEqualTo("18000");
        assertThat(result.remainingWeightGram()).isEqualTo("200");
        assertThat(result.flavorNotes()).containsExactly("자스민", "복숭아");
    }

    @Test
    void extractsCodeFencedMiniMaxJsonAndNormalizesProcess() {
        OpenRouterChatClient chatClient = mock(OpenRouterChatClient.class);
        when(chatClient.isConfigured()).thenReturn(true);
        when(chatClient.complete(anyString())).thenReturn(new OpenRouterChatClient.Completion(
                "{}",
                """
                        ```json
                        {"name":"INDONESIA IJEN","roastery":"","originCountry":"Indonesia","region":"East Java","farmOrStation":"Mt. Ijen","variety":"Typica, Lini S-288, S-795","altitude":"1,100 - 1,500masl","process":"Anaerobic Natural","flavorNotes":["Dried Berry","Cranberry","Cinnamon","Grape"]}
                        ```
                        """,
                "minimax/minimax-m3:free"
        ));

        OpenRouterBeanMappingService service = new OpenRouterBeanMappingService(
                chatClient,
                new ObjectMapper(),
                new BeanOcrMappingValidator(),
                new BeanMappingPromptFactory(new OcrTextPreprocessor())
        );

        LlmParsingResponse result = service.parseOcrText("""
                INDONESIA IJEN
                Origin: Mt. Ijen, East Java
                Process: Anaerobic Natural
                """);

        assertThat(result.originCountry()).isEqualTo("인도네시아");
        assertThat(result.process()).isEqualTo("ANAEROBIC");
        assertThat(result.flavorNotes()).containsExactly("Dried Berry", "크랜베리", "시나몬", "포도");
    }
}
