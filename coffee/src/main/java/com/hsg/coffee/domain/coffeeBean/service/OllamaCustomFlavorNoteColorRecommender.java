package com.hsg.coffee.domain.coffeeBean.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class OllamaCustomFlavorNoteColorRecommender implements CustomFlavorNoteColorRecommender {

    private static final Logger log = LoggerFactory.getLogger(OllamaCustomFlavorNoteColorRecommender.class);
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final String modelUrl;
    private final String modelId;
    private final int timeoutSeconds;
    private final double temperature;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaCustomFlavorNoteColorRecommender(
            @Value("${brewlog.llm.ollama.model-url:http://127.0.0.1:11434/api/generate}") String modelUrl,
            @Value("${brewlog.llm.ollama.model-id:gemma3:12b}") String modelId,
            @Value("${brewlog.llm.ollama.timeout-seconds:120}") int timeoutSeconds,
            @Value("${brewlog.llm.ollama.temperature:0.0}") double temperature,
            ObjectMapper objectMapper
    ) {
        this.modelUrl = modelUrl;
        this.modelId = modelId;
        this.timeoutSeconds = timeoutSeconds;
        this.temperature = temperature;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String recommendColor(String flavorNoteName) {
        String fallbackColor = fallbackColor(flavorNoteName);
        if (modelUrl == null || modelUrl.isBlank() || modelId == null || modelId.isBlank()) {
            return fallbackColor;
        }

        try {
            String response = callOllama(createPrompt(flavorNoteName));
            String generatedText = extractGeneratedText(response);
            String json = extractJson(generatedText);
            JsonNode root = objectMapper.readTree(json);
            String color = root.path("color").asText();
            if (HEX_COLOR.matcher(color).matches()) {
                return color.toUpperCase();
            }
        } catch (Exception exception) {
            log.warn("커스텀 향미 색상 추천 실패. note={}, fallback={}, reason={}",
                    flavorNoteName,
                    fallbackColor,
                    exception.getMessage());
        }
        return fallbackColor;
    }

    private String callOllama(String prompt) {
        Map<String, Object> request = Map.of(
                "model", modelId,
                "prompt", prompt,
                "stream", false,
                "format", jsonSchema(),
                "options", Map.of(
                        "temperature", temperature,
                        "num_predict", 80
                )
        );

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(modelUrl))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama API responded with status " + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("Ollama API 요청에 실패했습니다: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama API 요청이 중단되었습니다.", exception);
        }
    }

    private String createPrompt(String flavorNoteName) {
        return """
                너는 BrewLog의 커피 향미 노트 색상 추천 함수다.
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

    private Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "color", Map.of("type", "string")
                ),
                "required", List.of("color"),
                "additionalProperties", false
        );
    }

    private String extractGeneratedText(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.isObject() && root.has("response")) {
                return root.get("response").asText();
            }
        } catch (Exception ignored) {
            return response;
        }
        return response;
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

    private String fallbackColor(String flavorNoteName) {
        String normalized = flavorNoteName == null ? "" : flavorNoteName.toLowerCase();
        if (containsAny(normalized, "귤", "오렌지", "레몬", "라임", "시트러스", "citrus", "orange", "lemon", "lime")) {
            return "#F6A23A";
        }
        if (containsAny(normalized, "복숭", "살구", "자두", "peach", "apricot", "plum")) {
            return "#F4A261";
        }
        if (containsAny(normalized, "베리", "딸기", "라즈", "블루베리", "berry", "strawberry", "blueberry")) {
            return "#C94B70";
        }
        if (containsAny(normalized, "초콜", "카카오", "chocolate", "cacao", "cocoa")) {
            return "#6A4636";
        }
        if (containsAny(normalized, "견과", "아몬드", "헤이즐", "nut", "almond", "hazelnut")) {
            return "#A47148";
        }
        if (containsAny(normalized, "허브", "풀", "민트", "herb", "grass", "mint")) {
            return "#6F9A5B";
        }
        if (containsAny(normalized, "와인", "발효", "건포도", "wine", "fermented", "raisin")) {
            return "#7B2D43";
        }
        return "#7A5038";
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
