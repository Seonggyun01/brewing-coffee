package com.hsg.coffee.domain.originMap.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.repository.BrewRecordRepository;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.originMap.dto.OriginBeanPreviewResponse;
import com.hsg.coffee.domain.originMap.dto.OriginCountryResponse;
import com.hsg.coffee.domain.originMap.dto.OriginMapSummaryResponse;
import com.hsg.coffee.global.country.CountryInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OriginMapService {

    private final CoffeeBeanRepository coffeeBeanRepository;
    private final BrewRecordRepository brewRecordRepository;

    public OriginMapSummaryResponse getOriginMap() {
        Map<String, Long> beanCounts = toCountMap(coffeeBeanRepository.countBeansGroupByOriginCountryCode());
        Map<String, Long> brewRecordCounts = toCountMap(brewRecordRepository.countBrewRecordsGroupByOriginCountryCode());

        List<OriginCountryResponse> countries = beanCounts.entrySet().stream()
                .map(entry -> toCountryResponse(entry.getKey(), entry.getValue(), brewRecordCounts.getOrDefault(entry.getKey(), 0L)))
                .filter(country -> country != null)
                .sorted(Comparator.comparing(OriginCountryResponse::countryName))
                .toList();

        long totalBeanCount = countries.stream()
                .mapToLong(OriginCountryResponse::beanCount)
                .sum();
        long totalBrewRecordCount = countries.stream()
                .mapToLong(OriginCountryResponse::brewRecordCount)
                .sum();

        return new OriginMapSummaryResponse(countries.size(), totalBeanCount, totalBrewRecordCount, countries);
    }

    private OriginCountryResponse toCountryResponse(String countryCode, long beanCount, long brewRecordCount) {
        CountryInfo countryInfo = CountryInfo.findByCode(countryCode);
        if (countryInfo == null) {
            return null;
        }

        List<OriginBeanPreviewResponse> recentBeans = coffeeBeanRepository.findTop3ByOriginCountryCodeOrderByIdDesc(countryCode)
                .stream()
                .map(OriginBeanPreviewResponse::from)
                .toList();

        return OriginCountryResponse.of(countryInfo, beanCount, brewRecordCount, recentBeans);
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        return rows.stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                        row -> row[0].toString().toUpperCase(),
                        row -> (Long) row[1]
                ));
    }
}
