package com.hsg.coffee.domain.originMap.dto;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;

public record OriginBeanPreviewResponse(
        Long id,
        String name,
        String roastery,
        String region,
        String processType,
        String flavorNoteSummary,
        String flavorGradientStyle
) {

    public static OriginBeanPreviewResponse from(CoffeeBean coffeeBean) {
        String processType = coffeeBean.getProcessType() != null ? coffeeBean.getProcessType().name() : null;
        String flavorNoteSummary = coffeeBean.getFlavorNotes().stream()
                .limit(4)
                .map(flavorNote -> flavorNote.getDisplayName())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
        String flavorGradientStyle = coffeeBean.getFlavorNotes().isEmpty()
                ? "background: linear-gradient(90deg, #d8c8b5, #efe4d3);"
                : "background: linear-gradient(90deg, " + coffeeBean.getFlavorNotes().stream()
                        .map(FlavorNote::getColor)
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("#d8c8b5") + ");";

        return new OriginBeanPreviewResponse(
                coffeeBean.getId(),
                coffeeBean.getName(),
                coffeeBean.getRoastery(),
                coffeeBean.getRegion(),
                processType,
                flavorNoteSummary,
                flavorGradientStyle
        );
    }
}
