package com.hsg.coffee.domain.llmparsing.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {
        "brewlog.llm.provider=groq",
        "brewlog.llm.groq.api-key=test-key"
})
class GroqConfigurationTest {
    @Autowired ApplicationContext context;
    @Autowired BeanMappingService service;
    @Autowired GroqChatClient client;

    @Test
    void selectsGroqProviderAndDefaultsWithoutExternalCalls() {
        assertThat(service).isInstanceOf(GroqBeanMappingService.class);
        assertThat(context.getBeansOfType(BeanMappingService.class)).hasSize(1);
        assertThat(context.getBean(com.hsg.coffee.domain.coffeeBean.service.CustomFlavorNoteColorRecommender.class))
                .isInstanceOf(com.hsg.coffee.domain.coffeeBean.service.GroqCustomFlavorNoteColorRecommender.class);
        assertThat(client.isConfigured()).isTrue();
        assertThat(client.modelId()).isEqualTo("qwen/qwen3.8-27b");
        assertThat(client.modelUrl()).isEqualTo("https://api.groq.com/openai/v1/chat/completions");
    }
}
