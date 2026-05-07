package com.hsg.coffee.domain.coffeeBean.service;

public record PreparedCoffeeBeanCardImage(
        byte[] bytes,
        String filename,
        String contentType
) {
}
