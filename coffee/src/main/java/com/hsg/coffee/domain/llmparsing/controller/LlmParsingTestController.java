package com.hsg.coffee.domain.llmparsing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hsg.coffee.domain.llmparsing.dto.LlmImageBatchResponse;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingDebugResponse;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingRequest;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;
import com.hsg.coffee.domain.llmparsing.service.CoffeeBeanCardImageBatchLlmService;
import com.hsg.coffee.domain.llmparsing.service.HuggingFaceBeanMappingService;

@RestController
@RequestMapping("/dev/llm-parsing")
public class LlmParsingTestController {

    private final HuggingFaceBeanMappingService huggingFaceBeanMappingService;
    private final CoffeeBeanCardImageBatchLlmService coffeeBeanCardImageBatchLlmService;

    public LlmParsingTestController(
            HuggingFaceBeanMappingService huggingFaceBeanMappingService,
            CoffeeBeanCardImageBatchLlmService coffeeBeanCardImageBatchLlmService
    ) {
        this.huggingFaceBeanMappingService = huggingFaceBeanMappingService;
        this.coffeeBeanCardImageBatchLlmService = coffeeBeanCardImageBatchLlmService;
    }

    @PostMapping("/huggingface")
    public ResponseEntity<LlmParsingResponse> parseWithHuggingFace(@RequestBody LlmParsingRequest request) {
        return ResponseEntity.ok(huggingFaceBeanMappingService.parseOcrText(request.ocrText()));
    }

    @PostMapping("/huggingface/debug")
    public ResponseEntity<LlmParsingDebugResponse> debugWithHuggingFace(@RequestBody LlmParsingRequest request) {
        return ResponseEntity.ok(huggingFaceBeanMappingService.debugParseOcrText(request.ocrText()));
    }

    @PostMapping("/huggingface/image-batch")
    public ResponseEntity<LlmImageBatchResponse> parseImageBatchWithHuggingFace() {
        return ResponseEntity.ok(coffeeBeanCardImageBatchLlmService.processAllImages());
    }
}
