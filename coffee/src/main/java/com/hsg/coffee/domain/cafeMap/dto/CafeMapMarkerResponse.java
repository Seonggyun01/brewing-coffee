package com.hsg.coffee.domain.cafeMap.dto;

import java.time.LocalDate;

import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;

public record CafeMapMarkerResponse(
        Long id,
        String cafeName,
        String address,
        Double latitude,
        Double longitude,
        int visitCount,
        LocalDate latestVisitDate,
        Double averageRating,
        String memo
) {

    public static CafeMapMarkerResponse from(
            PurchasePlace purchasePlace,
            int visitCount,
            LocalDate latestVisitDate
    ) {
        return new CafeMapMarkerResponse(
                purchasePlace.getId(),
                purchasePlace.getName(),
                purchasePlace.getAddress(),
                purchasePlace.getLatitude(),
                purchasePlace.getLongitude(),
                visitCount,
                latestVisitDate,
                null,
                purchasePlace.getMemo()
        );
    }
}
