package com.hsg.coffee.domain.coffeeBean.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MockCoffeeBeanCardOcrService implements CoffeeBeanCardOcrService {

    @Override
    public String extractText(MultipartFile image) {
        return """
                Ethiopia Yirgacheffe Kochere
                Washed
                Jasmine, Lemon, Peach
                Roasted by Fritz Coffee
                200g
                18,000 KRW
                Roasted 2026.05.01
                """;
    }
}
