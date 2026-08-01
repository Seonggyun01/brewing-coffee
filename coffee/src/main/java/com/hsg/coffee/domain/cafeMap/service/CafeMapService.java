package com.hsg.coffee.domain.cafeMap.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.cafeMap.dto.CafeMapMarkerResponse;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CafeMapService {

    private final PurchasePlaceRepository purchasePlaceRepository;

    public List<CafeMapMarkerResponse> getCafeMarkers() {
        Map<String, PurchasePlace> uniquePlaces = new LinkedHashMap<>();
        for (PurchasePlace purchasePlace : purchasePlaceRepository.findByTypeAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByNameAsc(PurchasePlaceType.CAFE)) {
            uniquePlaces.putIfAbsent(placeKey(purchasePlace), purchasePlace);
        }

        return uniquePlaces.values().stream()
                .map(CafeMapMarkerResponse::from)
                .toList();
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
}
