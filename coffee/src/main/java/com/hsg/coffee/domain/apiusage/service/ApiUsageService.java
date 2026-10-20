package com.hsg.coffee.domain.apiusage.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Shared application usage only: never stores prompts, keys or user information. */
@Service
public class ApiUsageService {
    private final ObjectMapper json = new ObjectMapper();
    private final List<Event> events = new ArrayList<>();
    private final Path file;
    private final Clock clock;
    private final String groqModel;
    private final String embeddingModel;
    private final long groqLimit, routerLimit, visionLimit;
    private boolean delayed;

    @org.springframework.beans.factory.annotation.Autowired
    public ApiUsageService(
            @Value("${brewlog.api-usage.file:../logs/api-usage.jsonl}") String file,
            @Value("${brewlog.llm.groq.model-id:qwen/qwen3.8-27b}") String groqModel,
            @Value("${brewlog.llm.embedding.model-id:nvidia/nemotron-3-embed-1b:free}") String embeddingModel,
            @Value("${brewlog.api-usage.groq-daily-tokens:200000}") long groqLimit,
            @Value("${brewlog.api-usage.openrouter-daily-requests:50}") long routerLimit,
            @Value("${brewlog.api-usage.vision-monthly-units:1000}") long visionLimit) {
        this(Path.of(file), Clock.systemUTC(), groqModel, embeddingModel, groqLimit, routerLimit, visionLimit);
    }

    ApiUsageService(Path file, Clock clock, String groqModel, String embeddingModel,
                    long groqLimit, long routerLimit, long visionLimit) {
        this.file = file;
        this.clock = clock;
        this.groqModel = groqModel;
        this.embeddingModel = embeddingModel;
        this.groqLimit = groqLimit;
        this.routerLimit = routerLimit;
        this.visionLimit = visionLimit;
        if (Files.exists(file)) {
            try (var lines = Files.lines(file)) {
                lines.forEach(line -> {
                    try {
                        Event event = json.readValue(line, Event.class);
                        Instant.parse(event.time());
                        if (event.provider() == null || event.model() == null || event.amount() < 0) throw new IllegalArgumentException();
                        events.add(event);
                    }
                    catch (Exception e) { delayed = true; }
                });
            } catch (Exception e) { delayed = true; }
        }
    }

    public synchronized void record(String provider, String model, long amount) {
        if (amount < 0) return;
        var event = new Event(clock.instant().toString(), provider, model, amount);
        events.add(event);
        try {
            var parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Files.writeString(file, json.writeValueAsString(event) + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            delayed = true;
            LoggerFactory.getLogger(getClass()).warn("API usage persistence failed: {}", e.getClass().getSimpleName());
        }
    }

    public void recordChat(String provider, String model, String body) {
        if (!"Groq".equals(provider)) {
            record("OpenRouter", model, 1);
            return;
        }
        try {
            var usage = json.readTree(body).path("usage");
            long total = usage.path("total_tokens").asLong(-1);
            if (total < 0 && usage.has("prompt_tokens") && usage.has("completion_tokens")) {
                total = usage.path("prompt_tokens").asLong() + usage.path("completion_tokens").asLong();
            }
            if (total >= 0) record("Groq", model, total);
            else markDelayed();
        } catch (Exception e) { markDelayed(); }
    }

    private synchronized void markDelayed() { delayed = true; }

    public synchronized Snapshot snapshot() {
        var now = clock.instant();
        var date = now.atZone(ZoneOffset.UTC).toLocalDate();
        var day = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        var month = date.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new Snapshot(now.toString(), delayed, "서비스 전체 · 앱 내 집계 · UTC 기준", List.of(
                new Usage("Groq", groqModel, total("Groq", day, now), groqLimit, "토큰", "일간", "#F55036"),
                new Usage("OpenRouter", embeddingModel, total("OpenRouter", day, now), routerLimit, "회", "일간", "#7C3AED"),
                new Usage("Google Vision", "Text Detection", total("Google Vision", month, now), visionLimit, "건", "월간", "#4285F4")));
    }

    private long total(String provider, Instant start, Instant end) {
        return events.stream().filter(e -> e.provider().equals(provider))
                .filter(e -> !Instant.parse(e.time()).isBefore(start) && !Instant.parse(e.time()).isAfter(end))
                .mapToLong(Event::amount).sum();
    }

    public record Event(String time, String provider, String model, long amount) {}
    public record Usage(String site, String model, long current, long max, String unit, String period, String color) {}
    public record Snapshot(String updatedAt, boolean delayed, String scope, List<Usage> items) {}
}
