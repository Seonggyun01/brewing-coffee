package com.hsg.coffee.domain.llmparsing.dto;

import java.util.Map;

public record HuggingFaceRequest(
        String inputs,
        Map<String, Object> parameters
) {
}
