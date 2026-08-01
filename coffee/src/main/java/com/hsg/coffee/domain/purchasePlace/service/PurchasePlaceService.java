package com.hsg.coffee.domain.purchasePlace.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public List<PurchasePlace> getCafePlacesWithFilterRecords() {
        Map<String, PurchasePlace> uniquePlaces = new LinkedHashMap<>();
        for (PurchasePlace purchasePlace : purchasePlaceRepository.findDistinctByBrewRecordCoffeeBeanStatusAndTypeOrderByNameAsc(
                com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus.CAFE,
                PurchasePlaceType.CAFE
        )) {
            uniquePlaces.putIfAbsent(placeKey(purchasePlace), purchasePlace);
        }
        return List.copyOf(uniquePlaces.values());
    }

    public List<Long> getEquivalentCafePlaceIds(Long cafeId) {
        if (cafeId == null) {
            return List.of();
        }

        PurchasePlace selectedPlace = getEntity(cafeId);
        String selectedKey = placeKey(selectedPlace);
        return purchasePlaceRepository.findByTypeOrderByNameAsc(PurchasePlaceType.CAFE)
                .stream()
                .filter(purchasePlace -> selectedKey.equals(placeKey(purchasePlace)))
                .map(PurchasePlace::getId)
                .toList();
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
        String cleanName = clean(name);
        if (!StringUtils.hasText(cleanName)) {
            return null;
        }

        PurchasePlaceType normalizedType = typeOrDefault(type);
        String cleanAddress = clean(address);
        String cleanPlaceUrl = clean(placeUrl);
        String cleanMemo = clean(memo);

        Optional<PurchasePlace> existingPlace = findExistingPlace(
                cleanName,
                normalizedType,
                cleanAddress,
                cleanPlaceUrl,
                latitude,
                longitude
        );
        if (existingPlace.isPresent()) {
            PurchasePlace purchasePlace = existingPlace.get();
            purchasePlace.update(
                    chooseText(cleanName, purchasePlace.getName()),
                    purchasePlace.getType(),
                    chooseText(cleanAddress, purchasePlace.getAddress()),
                    chooseText(cleanPlaceUrl, purchasePlace.getPlaceUrl()),
                    latitude != null ? latitude : purchasePlace.getLatitude(),
                    longitude != null ? longitude : purchasePlace.getLongitude(),
                    chooseText(cleanMemo, purchasePlace.getMemo())
            );
            return purchasePlace;
        }

        return purchasePlaceRepository.save(PurchasePlace.create(
                cleanName,
                normalizedType,
                cleanAddress,
                cleanPlaceUrl,
                latitude,
                longitude,
                cleanMemo
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

    private Optional<PurchasePlace> findExistingPlace(
            String name,
            PurchasePlaceType type,
            String address,
            String placeUrl,
            Double latitude,
            Double longitude
    ) {
        if (StringUtils.hasText(placeUrl)) {
            Optional<PurchasePlace> byUrl = purchasePlaceRepository.findFirstByPlaceUrl(placeUrl);
            if (byUrl.isPresent()) {
                return byUrl;
            }
        }

        if (latitude != null && longitude != null) {
            Optional<PurchasePlace> byCoordinates = purchasePlaceRepository.findFirstByTypeAndLatitudeAndLongitude(
                    type,
                    latitude,
                    longitude
            );
            if (byCoordinates.isPresent()) {
                return byCoordinates;
            }
        }

        if (StringUtils.hasText(address)) {
            Optional<PurchasePlace> byNameAndAddress = purchasePlaceRepository.findFirstByTypeAndNameIgnoreCaseAndAddressIgnoreCase(
                    type,
                    name,
                    address
            );
            if (byNameAndAddress.isPresent()) {
                return byNameAndAddress;
            }
        }

        if (!StringUtils.hasText(address) && latitude == null && longitude == null && !StringUtils.hasText(placeUrl)) {
            return purchasePlaceRepository.findFirstByTypeAndNameIgnoreCase(type, name);
        }

        return Optional.empty();
    }

    private String chooseText(String incoming, String current) {
        return StringUtils.hasText(incoming) ? incoming : current;
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

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
