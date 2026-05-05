package com.hsg.coffee.domain.originMap.dto;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;

public record OriginBeanPreviewResponse(
        Long id,
        String name,
        String roastery,
        String region,
        String processType,
        String flavorNoteSummary
) {

    public static OriginBeanPreviewResponse from(CoffeeBean coffeeBean) {
        String processType = coffeeBean.getProcessType() != null ? coffeeBean.getProcessType().name() : null;
        String flavorNoteSummary = coffeeBean.getFlavorNotes().stream()
                .limit(4)
                .map(flavorNote -> flavorNote.getDisplayName())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");

        return new OriginBeanPreviewResponse(
                coffeeBean.getId(),
                coffeeBean.getName(),
                coffeeBean.getRoastery(),
                coffeeBean.getRegion(),
                processType,
                flavorNoteSummary
        );
    }
}
