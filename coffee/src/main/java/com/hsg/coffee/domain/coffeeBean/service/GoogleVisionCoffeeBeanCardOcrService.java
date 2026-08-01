package com.hsg.coffee.domain.coffeeBean.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "brewlog.ocr.provider", havingValue = "google-vision")
public class GoogleVisionCoffeeBeanCardOcrService implements CoffeeBeanCardOcrService {

    private final GoogleVisionCoffeeBeanCardOcrClient client;

    public GoogleVisionCoffeeBeanCardOcrService(GoogleVisionCoffeeBeanCardOcrClient client) {
        this.client = client;
    }

    @Override
    public String extractText(MultipartFile image) {
        return client.extractText(image);
    }
}
