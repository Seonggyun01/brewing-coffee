package com.hsg.coffee.domain.cafeMap.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hsg.coffee.domain.cafeMap.dto.CafeMapMarkerResponse;
import com.hsg.coffee.domain.cafeMap.service.CafeMapService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maps/cafes")
public class CafeMapApiController {

    private final CafeMapService cafeMapService;

    @GetMapping
    public List<CafeMapMarkerResponse> cafes() {
        return cafeMapService.getCafeMarkers();
    }
}
