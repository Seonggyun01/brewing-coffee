package com.hsg.coffee.domain.llmparsing.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "brewlog.llm.provider", havingValue = "groq")
public class GroqBeanMappingService extends OpenRouterBeanMappingService {

    public GroqBeanMappingService(GroqChatClient client, ObjectMapper mapper,
            BeanOcrMappingValidator validator, BeanMappingPromptFactory promptFactory) {
        super(client, mapper, validator, promptFactory);
    }
}
