package com.hsg.coffee.domain.coffeeBean.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public interface CustomFlavorNoteColorRecommender {
    String recommendColor(String flavorNoteName);

    default Map<String, String> recommendColors(Collection<String> names) {
        Map<String, String> colors = new LinkedHashMap<>();
        for (String name : names) colors.put(name, recommendColor(name));
        return colors;
    }
}
