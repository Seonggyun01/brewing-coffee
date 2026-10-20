package com.hsg.coffee.domain.apiusage.controller;

import com.hsg.coffee.domain.apiusage.service.ApiUsageService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiUsageController {
    private final ApiUsageService service;
    public ApiUsageController(ApiUsageService service) { this.service = service; }

    @GetMapping("/api/front/api-usage")
    public ResponseEntity<ApiUsageService.Snapshot> usage() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.snapshot());
    }
}
