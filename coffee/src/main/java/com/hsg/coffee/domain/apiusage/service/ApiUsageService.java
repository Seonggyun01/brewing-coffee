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
    private int activeRequests;
    private int activeExtractions;
    @Value("${brewlog.api-usage.max-concurrent-requests:2}")
    private int maxConcurrentRequests = 2;

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
        loadEvents();
    }

    private void loadEvents() {
        if (!Files.exists(file) && !events.isEmpty()) {
            delayed = true;
            return;
        }
        events.clear();
        if (Files.exists(file)) {
            try (var lines = Files.lines(file)) {
                lines.forEach(line -> {
                    try {
                        Event event = json.readValue(line, Event.class);
                        Instant.parse(event.time());
                        if (event.provider() == null || event.model() == null) throw new IllegalArgumentException();
                        events.add(event);
                    }
                    catch (Exception e) { delayed = true; }
                });
            } catch (Exception e) { delayed = true; }
        }
    }

    private <T> T withLedgerLock(java.util.function.Supplier<T> action) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path lockFile = file.resolveSibling(file.getFileName() + ".lock");
            try (var channel = java.nio.channels.FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    var lock = channel.lock()) {
                loadEvents();
                if (delayed) throw ApiLimitException.unavailable();
                return action.get();
            }
        } catch (java.nio.channels.OverlappingFileLockException exception) {
            throw ApiLimitException.busy();
        } catch (ApiLimitException exception) {
            throw exception;
        } catch (Exception exception) {
            delayed = true;
            throw ApiLimitException.unavailable();
        }
    }

    /** Persist the worst-case budget before sending anything to a paid provider. */
    public synchronized Permit reserve(String provider, String model, long amount) {
        return withLedgerLock(() -> {
            if (delayed) throw ApiLimitException.unavailable();
            if (amount <= 0) throw new IllegalArgumentException("API 예약량은 양수여야 합니다.");
            if (activeRequests >= maxConcurrentRequests) throw ApiLimitException.busy();
            Instant now = clock.instant();
            var date = now.atZone(ZoneOffset.UTC).toLocalDate();
            Instant start = ("Google Vision".equals(provider) ? date.withDayOfMonth(1) : date)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            long limit = switch (provider) {
                case "Groq" -> groqLimit;
                case "OpenRouter" -> routerLimit;
                case "Google Vision" -> visionLimit;
                default -> throw new IllegalArgumentException("지원하지 않는 API 공급자입니다.");
            };
            long used = total(provider, start, now);
            if (limit <= 0 || used > limit - amount) throw ApiLimitException.exhausted(provider);
            Event reservation = new Event(now.toString(), provider, model, amount);
            appendStrict(reservation);
            activeRequests++;
            return new Permit(reservation);
        });
    }

    public Permit reserveChat(String provider, String model, String prompt, int maxTokens) {
        if (maxTokens <= 0) throw new IllegalArgumentException("최대 출력 토큰은 양수여야 합니다.");
        // UTF-8 bytes overestimate text tokens; include room for the chat template.
        long amount = "Groq".equals(provider)
                ? (long) prompt.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + maxTokens + 1024L : 1L;
        return reserve(provider, model, amount);
    }

    /** Bound expensive local OCR too, before it spawns a Python process. */
    public synchronized AutoCloseableSlot enterExtraction() {
        if (activeExtractions >= maxConcurrentRequests) throw ApiLimitException.busy();
        activeExtractions++;
        return new AutoCloseableSlot();
    }

    public final class AutoCloseableSlot implements AutoCloseable {
        private boolean closed;
        @Override public void close() {
            synchronized (ApiUsageService.this) {
                if (!closed) { activeExtractions--; closed = true; }
            }
        }
    }

    public final class Permit implements AutoCloseable {
        private final Event reservation;
        private boolean settled;
        private boolean closed;
        private Permit(Event reservation) { this.reservation = reservation; }

        public void completeChat(String body) {
            synchronized (ApiUsageService.this) {
                if (closed || settled || !"Groq".equals(reservation.provider())) return;
                long actual;
                try {
                    var usage = json.readTree(body).path("usage");
                    actual = usage.path("total_tokens").asLong(-1);
                    if (actual < 0 && usage.has("prompt_tokens") && usage.has("completion_tokens")) {
                        long input = usage.path("prompt_tokens").asLong(-1);
                        long output = usage.path("completion_tokens").asLong(-1);
                        if (input < 0 || output < 0) return;
                        actual = Math.addExact(input, output);
                    }
                } catch (Exception exception) { return; }
                // Missing/malformed usage keeps the full reservation, including across restarts.
                if (actual < 0) return;
                if (actual != reservation.amount()) {
                    long adjustment = actual - reservation.amount();
                    withLedgerLock(() -> {
                        appendStrict(new Event(reservation.time(), reservation.provider(), reservation.model(), adjustment));
                        return null;
                    });
                }
                settled = true;
                // Unexpected tokenizer accounting must never allow further unbounded calls.
                if (actual > reservation.amount()) delayed = true;
            }
        }

        @Override public void close() {
            synchronized (ApiUsageService.this) {
                if (!closed) { activeRequests--; closed = true; }
            }
        }
    }

    private void appendStrict(Event event) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            byte[] bytes = (json.writeValueAsString(event) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try (var channel = java.nio.channels.FileChannel.open(file,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            events.add(event);
        } catch (Exception exception) {
            delayed = true;
            throw ApiLimitException.unavailable();
        }
    }

    public synchronized void record(String provider, String model, long amount) {
        if (amount < 0) return;
        try {
            withLedgerLock(() -> {
                appendStrict(new Event(clock.instant().toString(), provider, model, amount));
                return null;
            });
        } catch (ApiLimitException exception) {
            delayed = true;
            LoggerFactory.getLogger(getClass()).warn("API usage persistence failed");
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
        return new Snapshot(now.toString(), delayed, "서비스 전체 · 예약 포함 앱 내 집계 · UTC 기준", List.of(
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
