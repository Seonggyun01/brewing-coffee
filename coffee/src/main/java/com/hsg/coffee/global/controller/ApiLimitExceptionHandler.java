package com.hsg.coffee.global.controller;

import com.hsg.coffee.domain.apiusage.service.ApiLimitException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiLimitExceptionHandler {
    @ExceptionHandler(ApiLimitException.class)
    public ResponseEntity<?> limited(ApiLimitException exception) {
        return ResponseEntity.status(exception.status()).body(Map.of("message", exception.getMessage()));
    }
}
