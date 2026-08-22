package com.hsg.coffee.domain.coffeeBean.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanCardExtractionProgressTracker;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coffee-bean-card-extractions")
public class CoffeeBeanCardExtractionProgressController {

    private final CoffeeBeanCardExtractionProgressTracker progressTracker;

    @GetMapping(value = "/{progressId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String progressId) {
        return progressTracker.subscribe(progressId);
    }
}
