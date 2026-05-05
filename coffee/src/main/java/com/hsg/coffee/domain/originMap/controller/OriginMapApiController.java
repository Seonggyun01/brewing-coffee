package com.hsg.coffee.domain.originMap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hsg.coffee.domain.originMap.dto.OriginMapSummaryResponse;
import com.hsg.coffee.domain.originMap.service.OriginMapService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/origin-map")
public class OriginMapApiController {

    private final OriginMapService originMapService;

    @GetMapping("/countries")
    public OriginMapSummaryResponse countries() {
        return originMapService.getOriginMap();
    }
}
