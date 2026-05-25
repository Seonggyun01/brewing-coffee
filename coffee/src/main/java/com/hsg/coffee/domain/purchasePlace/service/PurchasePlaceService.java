package com.hsg.coffee.domain.purchasePlace.service;

import java.util.List;

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

    public List<PurchasePlace> getAll() {
        return purchasePlaceRepository.findAllByOrderByNameAsc();
    }

    public PurchasePlace getEntity(Long id) {
        return purchasePlaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("구매처를 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public PurchasePlace selectOrCreateIfPresent(
            Long selectedId,
            String name,
            PurchasePlaceType type,
            String address,
            String placeUrl,
            Double latitude,
            Double longitude,
            String memo
    ) {
        if (selectedId != null) {
            return getEntity(selectedId);
        }

        return createIfPresent(name, type, address, placeUrl, latitude, longitude, memo);
    }

    @Transactional
    public PurchasePlace createIfPresent(
            String name,
            PurchasePlaceType type,
            String address,
            String placeUrl,
            Double latitude,
            Double longitude,
            String memo
    ) {
        if (!StringUtils.hasText(name)) {
            return null;
        }

        return purchasePlaceRepository.save(PurchasePlace.create(
                clean(name),
                typeOrDefault(type),
                clean(address),
                clean(placeUrl),
                latitude,
                longitude,
                clean(memo)
        ));
    }

    @Transactional
    public void updateCoordinates(Long id, Double latitude, Double longitude) {
        PurchasePlace purchasePlace = getEntity(id);
        purchasePlace.update(
                purchasePlace.getName(),
                purchasePlace.getType(),
                purchasePlace.getAddress(),
                purchasePlace.getPlaceUrl(),
                latitude,
                longitude,
                purchasePlace.getMemo()
        );
    }

    private PurchasePlaceType typeOrDefault(PurchasePlaceType type) {
        return type != null ? type : PurchasePlaceType.OTHER;
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
