package com.hsg.coffee.domain.coffeeBean.service;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.hsg.coffee.domain.llmparsing.service.OpenRouterChatClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Primary
@ConditionalOnProperty(name = "brewlog.llm.provider", havingValue = "openrouter")
public class OpenRouterCustomFlavorNoteColorRecommender implements CustomFlavorNoteColorRecommender {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterCustomFlavorNoteColorRecommender.class);
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final int COLOR_RECOMMENDATION_MAX_TOKENS = 500;

    private final OpenRouterChatClient chatClient;
    private final ObjectMapper objectMapper;

    public OpenRouterCustomFlavorNoteColorRecommender(
            OpenRouterChatClient chatClient,
            ObjectMapper objectMapper
    ) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String recommendColor(String flavorNoteName) {
        String fallbackColor = FlavorNoteColorFallbackPalette.resolve(flavorNoteName);
        if (!chatClient.isConfigured()) {
            return fallbackColor;
        }

        try {
            OpenRouterChatClient.Completion completion = chatClient.complete(
                    createPrompt(flavorNoteName),
                    maxTokens()
            );
            String json = extractJson(completion.content());
            JsonNode root = objectMapper.readTree(json);
            String color = root.path("color").asText();
            if (HEX_COLOR.matcher(color).matches()) {
                return color.toUpperCase();
            }
        } catch (Exception exception) {
            log.warn(
                    "{} 향미 색상 추천 실패. note={}, fallback={}, reason={}",
                    providerName(),
                    flavorNoteName,
                    fallbackColor,
                    exception.getMessage()
            );
        }
        return fallbackColor;
    }

    protected int maxTokens() {
        return COLOR_RECOMMENDATION_MAX_TOKENS;
    }

    protected String providerName() {
        return "OpenRouter";
    }

    private String createPrompt(String flavorNoteName) {
        return """
                너는 BLANK의 커피 향미 노트 색상 추천 함수다.
                새 향미를 SCA/WCR Coffee Taster's Flavor Wheel 계열과 가장 가까운 앱 색상으로 매핑한다.

                규칙:
                1. 반드시 JSON 하나만 출력한다.
                2. color는 #RRGGBB 형식 하나만 쓴다.
                3. 같은 계열의 기존 색상과 조화롭게 고른다.
                4. 알 수 없으면 가장 가까운 향미 계열을 추정한다.

                기준 예시:
                - 자스민 #C8A2C8, 라벤더 #B497D6, 오렌지 블라썸 #F4C27A
                - 레몬 #F6C945, 라임 #B7D968, 오렌지 #F79A3E, 자몽 #F06F61
                - 딸기 #D94B6A, 블루베리 #4C3A8C, 블랙커런트 #5A174A
                - 복숭아 #F4A261, 살구 #F5B66A, 리치 #F2B8AA, 망고 #F9A03F
                - 꿀 #D6A04D, 카라멜 #C47A32, 바닐라 #E0C176
                - 밀크 초콜릿 #7B4B37, 다크 초콜릿 #5C3A2E, 헤이즐넛 #8E5E3C
                - 레드 와인 #7B2D43, 건포도 #5B3144, 말린 과일 #9E5D3D
                - 허브 #6B705C, 풀 #7FA35A, 시더우드 #8A5D3B, 민트 #72A980

                새 향미 노트: %s

                출력:
                {"color":"#RRGGBB"}
                """.formatted(flavorNoteName == null ? "" : flavorNoteName.trim());
    }

    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start == -1 || end == -1 || start > end) {
            return "";
        }
        return response.substring(start, end + 1);
    }
}
