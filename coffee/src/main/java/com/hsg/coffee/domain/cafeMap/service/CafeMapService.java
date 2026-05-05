package com.hsg.coffee.domain.cafeMap.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.cafeMap.dto.CafeMapMarkerResponse;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CafeMapService {

    private final PurchasePlaceRepository purchasePlaceRepository;

    public List<CafeMapMarkerResponse> getCafeMarkers() {
        return purchasePlaceRepository.findByTypeAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByNameAsc(PurchasePlaceType.CAFE)
                .stream()
                .map(CafeMapMarkerResponse::from)
                .toList();
    }
}
