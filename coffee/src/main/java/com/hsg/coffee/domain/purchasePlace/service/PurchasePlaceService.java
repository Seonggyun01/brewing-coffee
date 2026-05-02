package com.hsg.coffee.domain.purchasePlace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchasePlaceService {

    private final PurchasePlaceRepository purchasePlaceRepository;

    @Transactional
    public PurchasePlace createIfPresent(
            String name,
            PurchasePlaceType type,
            String address,
            String memo
    ) {
        if (!StringUtils.hasText(name)) {
            return null;
        }

        return purchasePlaceRepository.save(PurchasePlace.create(
                name,
                typeOrDefault(type),
                address,
                null,
                null,
                memo
        ));
    }

    @Transactional
    public PurchasePlace updateIfPresent(
            PurchasePlace purchasePlace,
            String name,
            PurchasePlaceType type,
            String address,
            String memo
    ) {
        if (!StringUtils.hasText(name)) {
            return null;
        }

        if (purchasePlace == null) {
            return createIfPresent(name, type, address, memo);
        }

        purchasePlace.update(
                name,
                typeOrDefault(type),
                address,
                purchasePlace.getLatitude(),
                purchasePlace.getLongitude(),
                memo
        );
        return purchasePlace;
    }

    @Transactional
    public void updateCoordinates(Long id, Double latitude, Double longitude) {
        PurchasePlace purchasePlace = purchasePlaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("구매처를 찾을 수 없습니다. id=" + id));
        purchasePlace.update(
                purchasePlace.getName(),
                purchasePlace.getType(),
                purchasePlace.getAddress(),
                latitude,
                longitude,
                purchasePlace.getMemo()
        );
    }

    private PurchasePlaceType typeOrDefault(PurchasePlaceType type) {
        return type != null ? type : PurchasePlaceType.OTHER;
    }
}
