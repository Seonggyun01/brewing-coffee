package com.hsg.coffee.domain.coffeeBean.service;

public enum CoffeeBeanCardExtractionPhase {
    OCR,
    LLM,
    PARSING,
    COMPLETED,
    FAILED
}
