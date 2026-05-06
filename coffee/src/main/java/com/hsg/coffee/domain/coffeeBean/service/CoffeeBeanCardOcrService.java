package com.hsg.coffee.domain.coffeeBean.service;

import org.springframework.web.multipart.MultipartFile;

public interface CoffeeBeanCardOcrService {

    String extractText(MultipartFile image);
}
