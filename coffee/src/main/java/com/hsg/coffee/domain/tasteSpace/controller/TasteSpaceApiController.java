package com.hsg.coffee.domain.tasteSpace.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hsg.coffee.domain.tasteSpace.dto.TasteSpaceResponse;
import com.hsg.coffee.domain.tasteSpace.service.TasteSpaceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/taste-space")
public class TasteSpaceApiController {

    private final TasteSpaceService tasteSpaceService;

    @GetMapping
    public TasteSpaceResponse get() {
        return tasteSpaceService.get();
    }

    @PostMapping("/rebuild")
    public ResponseEntity<?> rebuild() {
        try {
            return ResponseEntity.ok(tasteSpaceService.rebuild());
        } catch (IllegalStateException exception) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("message", exception.getMessage()));
        }
    }
}
