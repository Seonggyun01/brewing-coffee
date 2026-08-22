package com.hsg.coffee.domain.coffeeBean.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class CoffeeBeanCardExtractionProgressTracker {

    private static final Duration RETENTION = Duration.ofMinutes(10);
    private static final long EMITTER_TIMEOUT_MILLIS = Duration.ofMinutes(3).toMillis();

    private final Map<String, Progress> progressById = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersById = new ConcurrentHashMap<>();

    public void update(String progressId, CoffeeBeanCardExtractionPhase phase) {
        if (!StringUtils.hasText(progressId) || phase == null) {
            return;
        }
        removeExpired();
        progressById.put(progressId, new Progress(phase, Instant.now()));
        notifyEmitters(progressId, phase);
    }

    public Optional<CoffeeBeanCardExtractionPhase> find(String progressId) {
        if (!StringUtils.hasText(progressId)) {
            return Optional.empty();
        }
        removeExpired();
        Progress progress = progressById.get(progressId);
        return progress == null ? Optional.empty() : Optional.of(progress.phase());
    }

    public SseEmitter subscribe(String progressId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        CopyOnWriteArrayList<SseEmitter> emitters = emittersById.computeIfAbsent(
                progressId,
                ignored -> new CopyOnWriteArrayList<>()
        );
        emitters.add(emitter);

        Runnable removeEmitter = () -> removeEmitter(progressId, emitter);
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(() -> {
            removeEmitter.run();
            emitter.complete();
        });
        emitter.onError(ignored -> removeEmitter.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("progressId", progressId)));
        } catch (Exception exception) {
            removeEmitter.run();
            emitter.completeWithError(exception);
            return emitter;
        }

        find(progressId).ifPresent(phase -> sendPhase(progressId, emitter, phase));
        return emitter;
    }

    private void notifyEmitters(String progressId, CoffeeBeanCardExtractionPhase phase) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersById.get(progressId);
        if (emitters == null) {
            return;
        }
        emitters.forEach(emitter -> sendPhase(progressId, emitter, phase));
    }

    private void sendPhase(
            String progressId,
            SseEmitter emitter,
            CoffeeBeanCardExtractionPhase phase
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(Map.of("phase", phase.name())));
            if (phase == CoffeeBeanCardExtractionPhase.COMPLETED
                    || phase == CoffeeBeanCardExtractionPhase.FAILED) {
                removeEmitter(progressId, emitter);
                emitter.complete();
            }
        } catch (Exception exception) {
            removeEmitter(progressId, emitter);
            emitter.completeWithError(exception);
        }
    }

    private void removeEmitter(String progressId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersById.get(progressId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersById.remove(progressId, emitters);
        }
    }

    private void removeExpired() {
        Instant expirationThreshold = Instant.now().minus(RETENTION);
        progressById.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().updatedAt().isBefore(expirationThreshold);
            if (expired) {
                CopyOnWriteArrayList<SseEmitter> emitters = emittersById.remove(entry.getKey());
                if (emitters != null) {
                    emitters.forEach(SseEmitter::complete);
                }
            }
            return expired;
        });
    }

    private record Progress(CoffeeBeanCardExtractionPhase phase, Instant updatedAt) {
    }
}
