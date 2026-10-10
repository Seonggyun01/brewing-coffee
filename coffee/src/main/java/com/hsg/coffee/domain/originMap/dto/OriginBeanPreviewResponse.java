package com.hsg.coffee.domain.originMap.dto;

import java.util.List;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.dto.CustomFlavorNoteResponse;
import com.hsg.coffee.domain.coffeeBean.dto.FlavorGradientStyle;
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
        String flavorGradientStyle = FlavorGradientStyle.from(
                coffeeBean.getFlavorNotes(),
                List.<CustomFlavorNoteResponse>of()
        );

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
