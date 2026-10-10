package com.hsg.coffee.domain.cafeMap.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.brewRecord.entity.BrewRecord;
import com.hsg.coffee.domain.brewRecord.repository.BrewRecordRepository;
import com.hsg.coffee.domain.cafeMap.dto.CafeMapMarkerResponse;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;
import com.hsg.coffee.global.security.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CafeMapService {

    private final PurchasePlaceRepository purchasePlaceRepository;
    private final BrewRecordRepository brewRecordRepository;
    private final CurrentUserService currentUserService;

    public List<CafeMapMarkerResponse> getCafeMarkers() {
        Map<String, PurchasePlace> uniquePlaces = new LinkedHashMap<>();
        List<PurchasePlace> places = currentUserService.getCurrentUserId()
                .map(ownerId -> purchasePlaceRepository.findByOwnerIdAndTypeAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByNameAsc(
                        ownerId, PurchasePlaceType.CAFE))
                .orElseGet(() -> purchasePlaceRepository.findByTypeAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByNameAsc(
                        PurchasePlaceType.CAFE));
        for (PurchasePlace purchasePlace : places) {
            uniquePlaces.putIfAbsent(placeKey(purchasePlace), purchasePlace);
        }

        Map<String, VisitSummary> visitsByPlace = summarizeVisits();

        return uniquePlaces.entrySet().stream()
                .map(entry -> {
                    VisitSummary visits = visitsByPlace.getOrDefault(entry.getKey(), VisitSummary.EMPTY);
                    return CafeMapMarkerResponse.from(
                            entry.getValue(),
                            visits.visitCount(),
                            visits.latestVisitDate()
                    );
                })
                .toList();
    }

    private Map<String, VisitSummary> summarizeVisits() {
        List<BrewRecord> records = currentUserService.getCurrentUserId()
                .map(ownerId -> brewRecordRepository
                        .findByCoffeeBeanOwnerIdAndCoffeeBeanStatusOrderByBrewedDateDescIdDesc(
                                ownerId,
                                CoffeeBeanStatus.CAFE
                        ))
                .orElseGet(() -> brewRecordRepository
                        .findByCoffeeBeanStatusOrderByBrewedDateDescIdDesc(CoffeeBeanStatus.CAFE));
        Map<String, VisitSummary> summaries = new LinkedHashMap<>();

        for (BrewRecord record : records) {
            PurchasePlace purchasePlace = record.getCoffeeBean().getPurchasePlace();
            if (purchasePlace == null) {
                continue;
            }

            String key = placeKey(purchasePlace);
            VisitSummary current = summaries.getOrDefault(key, VisitSummary.EMPTY);
            summaries.put(key, current.addVisit(record.getBrewedDate()));
        }

        return summaries;
    }

    private String placeKey(PurchasePlace purchasePlace) {
        if (StringUtils.hasText(purchasePlace.getPlaceUrl())) {
            return "url:" + normalize(purchasePlace.getPlaceUrl());
        }
        if (purchasePlace.getLatitude() != null && purchasePlace.getLongitude() != null) {
            return "geo:" + purchasePlace.getLatitude() + "," + purchasePlace.getLongitude();
        }
        return "name:" + normalize(purchasePlace.getName()) + "|address:" + normalize(purchasePlace.getAddress());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "").toLowerCase();
    }

    private record VisitSummary(int visitCount, LocalDate latestVisitDate) {

        private static final VisitSummary EMPTY = new VisitSummary(0, null);

        private VisitSummary addVisit(LocalDate visitedDate) {
            LocalDate latest = latestVisitDate;
            if (visitedDate != null && (latest == null || visitedDate.isAfter(latest))) {
                latest = visitedDate;
            }
            return new VisitSummary(visitCount + 1, latest);
        }
    }
}
