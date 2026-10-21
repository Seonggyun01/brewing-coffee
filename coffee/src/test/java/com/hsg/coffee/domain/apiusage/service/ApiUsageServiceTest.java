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

    @Test void rejectsAtLimitAndKeepsReservationAfterRestartOrFailure() {
        var usage = service("2026-10-20T12:00:00Z");
        usage.record("OpenRouter", "nvidia/test", 49);
        try (var permit = usage.reserve("OpenRouter", "nvidia/test", 1)) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> usage.reserve("OpenRouter", "nvidia/test", 1))
                    .isInstanceOf(ApiLimitException.class);
        }
        var restarted = service("2026-10-20T13:00:00Z");
        assertThat(restarted.snapshot().items().get(1).current()).isEqualTo(50);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> restarted.reserve("OpenRouter", "nvidia/test", 1))
                .isInstanceOf(ApiLimitException.class);
    }

    @Test void settlesGroqTokensAndDoesNotDoubleRefund() {
        var usage = service("2026-10-20T12:00:00Z");
        try (var permit = usage.reserveChat("Groq", "qwen/test", "테스트", 768)) {
            assertThat(usage.snapshot().items().getFirst().current()).isGreaterThan(768);
            permit.completeChat("{\"usage\":{\"total_tokens\":1500}}");
            permit.completeChat("{\"usage\":{\"total_tokens\":100}}");
        }
        assertThat(service("2026-10-20T13:00:00Z").snapshot().items().getFirst().current()).isEqualTo(1500);
    }

    @Test void rejectsGroqBeforeSendingWhenRemainingBudgetIsTooSmall() {
        var usage = service("2026-10-20T12:00:00Z");
        usage.record("Groq", "qwen/test", 199999);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> usage.reserveChat("Groq", "qwen/test", "test", 768))
                .isInstanceOf(ApiLimitException.class);
    }

    @Test void unknownTokenUsageKeepsFullReservation() {
        var usage = service("2026-10-20T12:00:00Z");
        long reserved;
        try (var permit = usage.reserveChat("Groq", "qwen/test", "test", 768)) {
            reserved = usage.snapshot().items().getFirst().current();
            permit.completeChat("{}");
        }
        assertThat(service("2026-10-20T13:00:00Z").snapshot().items().getFirst().current()).isEqualTo(reserved);
    }

    @Test void failsClosedForCorruptLedgerAndUnwritableStorage() throws Exception {
        Files.writeString(directory.resolve("usage.jsonl"), "{broken\n");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service("2026-10-20T12:00:00Z").reserve("OpenRouter", "test", 1))
                .isInstanceOf(ApiLimitException.class);
        var blocked = new ApiUsageService(directory, Clock.systemUTC(), "test", "test", 10, 10, 10);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> blocked.reserve("OpenRouter", "test", 1))
                .isInstanceOf(ApiLimitException.class);
    }

    @Test void concurrencySlotsAreReleasedAfterFailure() {
        var usage = service("2026-10-20T12:00:00Z");
        var first = usage.reserve("OpenRouter", "test", 1);
        var second = usage.reserve("OpenRouter", "test", 1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> usage.reserve("OpenRouter", "test", 1))
                .isInstanceOf(ApiLimitException.class);
        first.close(); first.close(); second.close();
        try (var permit = usage.reserve("OpenRouter", "test", 1)) {
            assertThat(usage.snapshot().items().get(1).current()).isEqualTo(3);
        }
        var ocr1 = usage.enterExtraction(); var ocr2 = usage.enterExtraction();
        org.assertj.core.api.Assertions.assertThatThrownBy(usage::enterExtraction).isInstanceOf(ApiLimitException.class);
        ocr1.close(); ocr2.close();
        try (var slot = usage.enterExtraction()) { /* available again */ }
    }

    @Test void concurrentReservationsCannotOverspendLastUnit() throws Exception {
        var usage = service("2026-10-20T12:00:00Z");
        usage.record("OpenRouter", "test", 49);
        var admitted = new java.util.concurrent.atomic.AtomicInteger();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 20; i++) tasks.add(pool.submit(() -> {
                try (var permit = usage.reserve("OpenRouter", "test", 1)) { admitted.incrementAndGet(); }
                catch (ApiLimitException expected) { }
            }));
            for (var task : tasks) task.get();
        }
        assertThat(admitted.get()).isEqualTo(1);
        assertThat(usage.snapshot().items().get(1).current()).isEqualTo(50);
    }

    @Test void visionBudgetResetsMonthlyAndReservationsCountTowardLimit() {
        var usage = service("2026-10-20T12:00:00Z");
        usage.record("Google Vision", "test", 999);
        try (var permit = usage.reserve("Google Vision", "test", 1)) { }
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> usage.reserve("Google Vision", "test", 1))
                .isInstanceOf(ApiLimitException.class);
        try (var permit = service("2026-11-01T00:00:00Z").reserve("Google Vision", "test", 1)) { }
        try (var permit = service("2026-10-21T00:00:00Z").reserve("OpenRouter", "test", 1)) { }
    }
    @Test void anotherServiceInstanceCannotUseStaleCounters() {
        var first = service("2026-10-20T12:00:00Z");
        var second = service("2026-10-20T12:00:00Z");
        first.record("OpenRouter", "test", 50);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> second.reserve("OpenRouter", "test", 1))
                .isInstanceOf(ApiLimitException.class);
    }
}
