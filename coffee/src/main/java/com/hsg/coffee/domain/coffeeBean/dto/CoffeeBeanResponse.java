package com.hsg.coffee.domain.coffeeBean.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.global.country.CountryInfo;

import lombok.Getter;

@Getter
public class CoffeeBeanResponse {

    private final Long id;
    private final String name;
    private final String roastery;
    private final String country;
    private final String originCountryCode;
    private final String originCountryDisplayName;
    private final String region;
    private final String farm;
    private final String variety;
    private final String altitude;
    private final ProcessType processType;
    private final CoffeeBeanStatus status;
    private final List<FlavorNote> flavorNotes;
    private final List<String> customFlavorNotes;
    private final List<CustomFlavorNoteResponse> customFlavorNoteDetails;
    private final String memo;
    private final LocalDate roastedDate;
    private final LocalDate purchasedDate;
    private final Integer price;
    private final Integer weight;
    private final Long purchasePlaceId;
    private final String purchasePlaceName;
    private final PurchasePlaceType purchasePlaceType;
    private final String purchasePlaceAddress;
    private final String purchasePlaceUrl;
    private final Double purchasePlaceLatitude;
    private final Double purchasePlaceLongitude;
    private final String purchasePlaceMemo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private CoffeeBeanResponse(CoffeeBean coffeeBean, List<CustomFlavorNoteResponse> customFlavorNoteDetails) {
        PurchasePlace purchasePlace = coffeeBean.getPurchasePlace();

        this.id = coffeeBean.getId();
        this.name = coffeeBean.getName();
        this.roastery = coffeeBean.getRoastery();
        this.country = coffeeBean.getCountry();
        this.originCountryCode = coffeeBean.getOriginCountryCode();
        CountryInfo countryInfo = CountryInfo.findByCode(coffeeBean.getOriginCountryCode());
        this.originCountryDisplayName = countryInfo != null ? countryInfo.getDisplayName() : coffeeBean.getCountry();
        this.region = coffeeBean.getRegion();
        this.farm = coffeeBean.getFarm();
        this.variety = coffeeBean.getVariety();
        this.altitude = coffeeBean.getAltitude();
        this.processType = coffeeBean.getProcessType();
        this.status = coffeeBean.getStatus();
        this.flavorNotes = List.copyOf(coffeeBean.getFlavorNotes());
        this.customFlavorNotes = List.copyOf(coffeeBean.getCustomFlavorNotes());
        this.customFlavorNoteDetails = customFlavorNoteDetails == null ? List.of() : List.copyOf(customFlavorNoteDetails);
        this.memo = coffeeBean.getMemo();
        this.roastedDate = coffeeBean.getRoastedDate();
        this.purchasedDate = coffeeBean.getPurchasedDate();
        this.price = coffeeBean.getPrice();
        this.weight = coffeeBean.getWeight();
        this.purchasePlaceId = purchasePlace != null ? purchasePlace.getId() : null;
        this.purchasePlaceName = purchasePlace != null ? purchasePlace.getName() : null;
        this.purchasePlaceType = purchasePlace != null ? purchasePlace.getType() : null;
        this.purchasePlaceAddress = purchasePlace != null ? purchasePlace.getAddress() : null;
        this.purchasePlaceUrl = purchasePlace != null ? purchasePlace.getPlaceUrl() : null;
        this.purchasePlaceLatitude = purchasePlace != null ? purchasePlace.getLatitude() : null;
        this.purchasePlaceLongitude = purchasePlace != null ? purchasePlace.getLongitude() : null;
        this.purchasePlaceMemo = purchasePlace != null ? purchasePlace.getMemo() : null;
        this.createdAt = coffeeBean.getCreatedAt();
        this.updatedAt = coffeeBean.getUpdatedAt();
    }

    public static CoffeeBeanResponse from(CoffeeBean coffeeBean) {
        return new CoffeeBeanResponse(coffeeBean, List.of());
    }

    public static CoffeeBeanResponse from(CoffeeBean coffeeBean, List<CustomFlavorNoteResponse> customFlavorNoteDetails) {
        return new CoffeeBeanResponse(coffeeBean, customFlavorNoteDetails);
    }

    public String getFlavorGradientStyle() {
        if (flavorNotes.isEmpty() && customFlavorNoteDetails.isEmpty()) {
            return "background: #ded4c8;";
        }

        List<String> colors = new ArrayList<>();
        colors.addAll(flavorNotes.stream()
                .map(FlavorNote::getColor)
                .toList());
        colors.addAll(customFlavorNoteDetails.stream()
                .map(CustomFlavorNoteResponse::color)
                .toList());
        return "background: linear-gradient(90deg, " + String.join(", ", colors) + ");";
    }

    public String getFlavorNoteSummary() {
        if (flavorNotes.isEmpty() && customFlavorNotes.isEmpty()) {
            return "-";
        }

        String selectedNotes = flavorNotes.stream()
                .map(FlavorNote::getDisplayName)
                .collect(Collectors.joining(", "));
        String customNotes = String.join(", ", customFlavorNotes);

        if (selectedNotes.isBlank()) {
            return customNotes;
        }
        if (customNotes.isBlank()) {
            return selectedNotes;
        }
        return selectedNotes + ", " + customNotes;
    }
}
