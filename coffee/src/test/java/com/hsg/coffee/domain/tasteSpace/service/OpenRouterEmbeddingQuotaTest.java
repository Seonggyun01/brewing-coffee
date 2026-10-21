package com.hsg.coffee.domain.tasteSpace.service;

import com.hsg.coffee.domain.apiusage.service.ApiLimitException;
import com.hsg.coffee.domain.apiusage.service.ApiUsageService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

class OpenRouterEmbeddingQuotaTest {
    @Test void countsEmbeddingAndRejectsBeforeSecondHttpCall(@TempDir Path dir) throws Exception {
        var count = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/embeddings", exchange -> {
            count.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"data\":[{\"embedding\":[0.1,0.2]}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var usage = new ApiUsageService(dir.resolve("usage.jsonl").toString(), "groq", "embedding", 200000, 1, 1000);
            var client = new OpenRouterEmbeddingClient(new ObjectMapper(), "test",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/embeddings", "embedding", 5);
            ReflectionTestUtils.setField(client, "apiUsage", usage);
            assertThat(client.embed(List.of("coffee"))).hasSize(1);
            assertThatThrownBy(() -> client.embed(List.of("coffee"))).isInstanceOf(ApiLimitException.class);
            assertThat(count).hasValue(1);
        } finally { server.stop(0); }
    }
}
