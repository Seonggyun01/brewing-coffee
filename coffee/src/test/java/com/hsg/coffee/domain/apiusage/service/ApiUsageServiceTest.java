package com.hsg.coffee.domain.apiusage.service;
import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class ApiUsageServiceTest {
    @TempDir Path directory;
    private ApiUsageService service(String time) {
        return new ApiUsageService(directory.resolve("usage.jsonl"), Clock.fixed(Instant.parse(time), ZoneOffset.UTC), "qwen/test", "nvidia/test", 200000, 50, 1000);
    }
    @Test void persistsSharedUsageWithoutStoringContent() throws Exception {
        var usage = service("2026-10-20T12:00:00Z");
        usage.recordChat("Groq", "qwen/test", "{\"usage\":{\"prompt_tokens\":1200,\"completion_tokens\":220}}");
        usage.recordChat("Groq", "qwen/test", "{\"usage\":{\"total_tokens\":100}}");
        usage.record("OpenRouter", "nvidia/test", 1);
        usage.record("Google Vision", "Text Detection", 1);
        assertThat(service("2026-10-20T13:00:00Z").snapshot().items()).extracting(ApiUsageService.Usage::current).containsExactly(1520L, 1L, 1L);
        assertThat(Files.readString(directory.resolve("usage.jsonl"))).doesNotContain("prompt_tokens");
    }
    @Test void resetsDailyAndMonthlyPeriods() {
        var usage = service("2026-10-20T23:59:59Z");
        usage.record("Groq", "qwen/test", 120);
        usage.record("OpenRouter", "nvidia/test", 1);
        usage.record("Google Vision", "Text Detection", 1);
        assertThat(service("2026-10-21T00:00:00Z").snapshot().items()).extracting(ApiUsageService.Usage::current).containsExactly(0L, 0L, 1L);
        assertThat(service("2026-11-01T00:00:00Z").snapshot().items()).extracting(ApiUsageService.Usage::current).containsExactly(0L, 0L, 0L);
    }
    @Test void missingTokensAreMarkedDelayed() {
        var usage = service("2026-10-20T12:00:00Z"); usage.recordChat("Groq", "qwen/test", "{}");
        assertThat(usage.snapshot().delayed()).isTrue();
    }
    @Test void retainsValidEventsAfterInterruptedWrite() throws Exception {
        service("2026-10-20T12:00:00Z").record("Groq", "qwen/test", 120);
        Files.writeString(directory.resolve("usage.jsonl"), "{broken\n", StandardOpenOption.APPEND);
        var loaded = service("2026-10-20T13:00:00Z").snapshot();
        assertThat(loaded.delayed()).isTrue(); assertThat(loaded.items().getFirst().current()).isEqualTo(120);
    }
}
