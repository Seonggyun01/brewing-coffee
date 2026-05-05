package com.hsg.coffee.domain.originMap.dto;

import java.util.List;

import com.hsg.coffee.domain.originMap.entity.OriginColorLevel;
import com.hsg.coffee.global.country.CountryInfo;

public record OriginCountryResponse(
        String countryCode,
        String countryName,
        String koreanCountryName,
        long beanCount,
        long brewRecordCount,
        OriginColorLevel colorLevel,
        String fillColor,
        List<OriginBeanPreviewResponse> recentBeans
) {

    public static OriginCountryResponse of(
            CountryInfo countryInfo,
            long beanCount,
            long brewRecordCount,
            List<OriginBeanPreviewResponse> recentBeans
    ) {
        OriginColorLevel colorLevel = OriginColorLevel.fromBeanCount(beanCount);
        return new OriginCountryResponse(
                countryInfo.getCode(),
                countryInfo.getEnglishName(),
                countryInfo.getKoreanName(),
                beanCount,
                brewRecordCount,
                colorLevel,
                colorLevel.getFillColor(),
                recentBeans
        );
    }
}
