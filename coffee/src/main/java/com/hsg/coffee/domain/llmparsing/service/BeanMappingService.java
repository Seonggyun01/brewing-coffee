package com.hsg.coffee.domain.llmparsing.service;

import com.hsg.coffee.domain.llmparsing.dto.LlmParsingDebugResponse;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;

public interface BeanMappingService {

    LlmParsingResponse parseOcrText(String ocrText);

    LlmParsingDebugResponse debugParseOcrText(String ocrText);
}
