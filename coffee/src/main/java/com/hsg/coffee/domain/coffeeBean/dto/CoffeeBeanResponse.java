package com.hsg.coffee.domain.coffeeBean.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;

import lombok.Getter;

@Getter
public class CoffeeBeanResponse {

    private final Long id;
    private final String name;
    private final String roastery;
    private final String country;
    private final String region;
    private final String farm;
    private final String variety;
    private final String altitude;
    private final ProcessType processType;
    private final String flavorNotes;
    private final String memo;
    private final LocalDate roastedDate;
    private final LocalDate purchasedDate;
    private final Integer price;
    private final Integer weight;
    private final Long purchasePlaceId;
    private final String purchasePlaceName;
    private final PurchasePlaceType purchasePlaceType;
    private final String purchasePlaceAddress;
    private final Double purchasePlaceLatitude;
    private final Double purchasePlaceLongitude;
    private final String purchasePlaceMemo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private CoffeeBeanResponse(CoffeeBean coffeeBean) {
        PurchasePlace purchasePlace = coffeeBean.getPurchasePlace();

        this.id = coffeeBean.getId();
        this.name = coffeeBean.getName();
        this.roastery = coffeeBean.getRoastery();
        this.country = coffeeBean.getCountry();
        this.region = coffeeBean.getRegion();
        this.farm = coffeeBean.getFarm();
        this.variety = coffeeBean.getVariety();
        this.altitude = coffeeBean.getAltitude();
        this.processType = coffeeBean.getProcessType();
        this.flavorNotes = coffeeBean.getFlavorNotes();
        this.memo = coffeeBean.getMemo();
        this.roastedDate = coffeeBean.getRoastedDate();
        this.purchasedDate = coffeeBean.getPurchasedDate();
        this.price = coffeeBean.getPrice();
        this.weight = coffeeBean.getWeight();
        this.purchasePlaceId = purchasePlace != null ? purchasePlace.getId() : null;
        this.purchasePlaceName = purchasePlace != null ? purchasePlace.getName() : null;
        this.purchasePlaceType = purchasePlace != null ? purchasePlace.getType() : null;
        this.purchasePlaceAddress = purchasePlace != null ? purchasePlace.getAddress() : null;
        this.purchasePlaceLatitude = purchasePlace != null ? purchasePlace.getLatitude() : null;
        this.purchasePlaceLongitude = purchasePlace != null ? purchasePlace.getLongitude() : null;
        this.purchasePlaceMemo = purchasePlace != null ? purchasePlace.getMemo() : null;
        this.createdAt = coffeeBean.getCreatedAt();
        this.updatedAt = coffeeBean.getUpdatedAt();
    }

    public static CoffeeBeanResponse from(CoffeeBean coffeeBean) {
        return new CoffeeBeanResponse(coffeeBean);
    }
}
