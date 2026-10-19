package com.hsg.coffee.domain.tasteSpace.dto;

import java.util.List;

public record TasteSpaceResponse(
        String state,
        String model,
        int beanCount,
        TasteProfile profile,
        List<BeanPoint> points
) {
    public record TasteProfile(
            String title,
            String description,
            String favoriteBeanName,
            Double averageRating,
            List<String> dominantNotes,
            String preferredOrigin,
            String preferredProcess,
            int totalBrewCount,
            int ratedBeanCount,
            String confidenceLabel
    ) {
    }

    public record BeanPoint(
            Long id,
            String name,
            String roastery,
            String country,
            String process,
            String status,
            String color,
            List<String> colors,
            List<String> flavorNotes,
            Double averageRating,
            int brewCount,
            double x,
            double y,
            double z,
            Long closestBeanId,
            String closestBeanName,
            double similarity,
            String detailUrl
    ) {
    }
}
