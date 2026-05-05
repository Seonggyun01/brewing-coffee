package com.hsg.coffee.domain.cafeMap.dto;

import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;

public record CafeMapMarkerResponse(
        Long id,
        String cafeName,
        String address,
        Double latitude,
        Double longitude,
        int visitCount,
        String latestVisitDate,
        Double averageRating,
        String memo
) {

    public static CafeMapMarkerResponse from(PurchasePlace purchasePlace) {
        return new CafeMapMarkerResponse(
                purchasePlace.getId(),
                purchasePlace.getName(),
                purchasePlace.getAddress(),
                purchasePlace.getLatitude(),
                purchasePlace.getLongitude(),
                1,
                null,
                null,
                purchasePlace.getMemo()
        );
    }
}
