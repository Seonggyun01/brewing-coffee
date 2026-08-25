package com.hsg.coffee;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanCardOcrService;
import com.hsg.coffee.domain.coffeeBean.service.CustomFlavorNoteColorRecommender;
import com.hsg.coffee.domain.coffeeBean.service.GoogleVisionCoffeeBeanCardOcrService;
import com.hsg.coffee.domain.coffeeBean.service.OpenRouterCustomFlavorNoteColorRecommender;
import com.hsg.coffee.domain.llmparsing.service.BeanMappingService;
import com.hsg.coffee.domain.llmparsing.service.OpenRouterBeanMappingService;

@SpringBootTest
@ActiveProfiles("server")
class ServerAiProviderConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CoffeeBeanCardOcrService ocrService;

    @Autowired
    private BeanMappingService beanMappingService;

    @Autowired
    private CustomFlavorNoteColorRecommender colorRecommender;

    @Test
    void serverProfileSelectsExternalAiProvidersWithoutOcrFallback() {
        Map<String, CoffeeBeanCardOcrService> ocrServices =
                applicationContext.getBeansOfType(CoffeeBeanCardOcrService.class);

        assertThat(ocrServices).hasSize(1);
        assertThat(ocrService).isInstanceOf(GoogleVisionCoffeeBeanCardOcrService.class);
        assertThat(beanMappingService).isInstanceOf(OpenRouterBeanMappingService.class);
        assertThat(colorRecommender).isInstanceOf(OpenRouterCustomFlavorNoteColorRecommender.class);
    }
}
