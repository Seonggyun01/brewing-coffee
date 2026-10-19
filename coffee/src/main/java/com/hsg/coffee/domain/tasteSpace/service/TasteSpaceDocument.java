package com.hsg.coffee.domain.tasteSpace.service;

import java.util.List;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;

record TasteSpaceDocument(
        CoffeeBean bean,
        String sourceHash,
        String text,
        String color,
        List<String> colors,
        List<String> flavorNotes,
        Double averageRating,
        int brewCount
) {
}
