package com.hsg.coffee.domain.originMap.dto;

import java.util.List;

public record OriginMapSummaryResponse(
        long experiencedCountryCount,
        long totalBeanCount,
        long totalBrewRecordCount,
        List<OriginCountryResponse> countries
) {
}
