package com.hsg.coffee.domain.llmparsing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class OpenRouterChatClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));

            String responseBody = objectMapper.writeValueAsString(Map.of(
                    "model", "minimax/minimax-m3:free",
                    "choices", List.of(Map.of(
                            "message", Map.of("content", "{\"name\":\"테스트 원두\"}")
                    ))
            ));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsFixedMiniMaxModelAndJsonResponseFormat() throws Exception {
        OpenRouterChatClient client = new OpenRouterChatClient(
                "test-api-key",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions",
                "minimax/minimax-m3:free",
                5,
                700,
                0.0,
                "https://brewlog.example.com",
                "BrewLog",
                objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        );

        OpenRouterChatClient.Completion completion = client.complete("JSON으로 변환해줘");

        JsonNode sent = objectMapper.readTree(requestBody.get());
        assertThat(sent.path("model").asText()).isEqualTo("minimax/minimax-m3:free");
        assertThat(sent.has("models")).isFalse();
        assertThat(sent.path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(sent.path("max_tokens").asInt()).isEqualTo(700);
        assertThat(authorization.get()).isEqualTo("Bearer test-api-key");
        assertThat(completion.modelId()).isEqualTo("minimax/minimax-m3:free");
        assertThat(completion.content()).contains("테스트 원두");
    }
}
