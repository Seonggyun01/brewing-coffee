package com.hsg.coffee.domain.coffeeBean.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.global.country.CountryInfo;
import com.hsg.coffee.global.entity.BaseTimeEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "coffee_beans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeBean extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String roastery;

    @Column(length = 100)
    private String country;

    @Column(length = 2)
    private String originCountryCode;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String farm;

    @Column(length = 100)
    private String variety;

    @Column(length = 50)
    private String altitude;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ProcessType processType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CoffeeBeanStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "coffee_bean_flavor_notes", joinColumns = @JoinColumn(name = "coffee_bean_id"))
    @OrderColumn(name = "sort_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "flavor_note", length = 60)
    private List<FlavorNote> flavorNotes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "coffee_bean_custom_flavor_notes", joinColumns = @JoinColumn(name = "coffee_bean_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "custom_flavor_note", length = 60)
    private List<String> customFlavorNotes = new ArrayList<>();

    @Column(length = 1000)
    private String memo;

    private LocalDate roastedDate;

    private LocalDate purchasedDate;

    private Integer price;

    private Integer weight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_place_id")
    private PurchasePlace purchasePlace;

    public static CoffeeBean create(
            String name,
            String roastery,
            String country,
            String region,
            String farm,
            String variety,
            String altitude,
            ProcessType processType,
            List<FlavorNote> flavorNotes,
            List<String> customFlavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight
    ) {
        return create(
                name,
                roastery,
                country,
                CountryInfo.findCodeByName(country),
                region,
                farm,
                variety,
                altitude,
                processType,
                flavorNotes,
                customFlavorNotes,
                memo,
                roastedDate,
                purchasedDate,
                price,
                weight
        );
    }

    public static CoffeeBean create(
            String name,
            String roastery,
            String country,
            String originCountryCode,
            String region,
            String farm,
            String variety,
            String altitude,
            ProcessType processType,
            List<FlavorNote> flavorNotes,
            List<String> customFlavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight
    ) {
        return create(
                name,
                roastery,
                country,
                originCountryCode,
                region,
                farm,
                variety,
                altitude,
                processType,
                flavorNotes,
                customFlavorNotes,
                memo,
                roastedDate,
                purchasedDate,
                price,
                weight,
                CoffeeBeanStatus.CURRENT,
                null
        );
    }

    public static CoffeeBean create(
            String name,
            String roastery,
            String country,
            String originCountryCode,
            String region,
            String farm,
            String variety,
            String altitude,
            ProcessType processType,
            List<FlavorNote> flavorNotes,
            List<String> customFlavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight,
            CoffeeBeanStatus status,
            PurchasePlace purchasePlace
    ) {
        CoffeeBean coffeeBean = new CoffeeBean();
        coffeeBean.name = name;
        coffeeBean.roastery = roastery;
        coffeeBean.country = country;
        coffeeBean.originCountryCode = normalizeCountryCode(originCountryCode);
        coffeeBean.region = region;
        coffeeBean.farm = farm;
        coffeeBean.variety = variety;
        coffeeBean.altitude = altitude;
        coffeeBean.processType = processType;
        if (flavorNotes != null) {
            coffeeBean.flavorNotes.addAll(flavorNotes);
        }
        if (customFlavorNotes != null) {
            coffeeBean.customFlavorNotes.addAll(customFlavorNotes);
        }
        coffeeBean.memo = memo;
        coffeeBean.roastedDate = roastedDate;
        coffeeBean.purchasedDate = purchasedDate;
        coffeeBean.price = price;
        coffeeBean.weight = weight;
        coffeeBean.status = status != null ? status : CoffeeBeanStatus.CURRENT;
        coffeeBean.purchasePlace = purchasePlace;
        return coffeeBean;
    }

    public static CoffeeBean create(
            String name,
            String roastery,
            String country,
            String region,
            String farm,
            String variety,
            String altitude,
            ProcessType processType,
            List<FlavorNote> flavorNotes,
            List<String> customFlavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight,
            CoffeeBeanStatus status,
            PurchasePlace purchasePlace
    ) {
        return create(
                name,
                roastery,
                country,
                CountryInfo.findCodeByName(country),
                region,
                farm,
                variety,
                altitude,
                processType,
                flavorNotes,
                customFlavorNotes,
                memo,
                roastedDate,
                purchasedDate,
                price,
                weight,
                status,
                purchasePlace
        );
    }

    public static CoffeeBean create(
            String name,
            String roastery,
            String country,
            String region,
            String farm,
            String variety,
            String altitude,
            ProcessType processType,
            List<FlavorNote> flavorNotes,
            List<String> customFlavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight,
            PurchasePlace purchasePlace
    ) {
        return create(
                name,
                roastery,
                country,
                CountryInfo.findCodeByName(country),
                region,
                farm,
                variety,
                altitude,
                processType,
                flavorNotes,
                customFlavorNotes,
                memo,
                roastedDate,
                purchasedDate,
                price,
                weight,
                CoffeeBeanStatus.CURRENT,
                purchasePlace
        );
    }

    public void update(
            String name,
            String roastery,
            String country,
            String region,
            String farm,
            String variety,
            String altitude,
            ProcessType processType,
            List<FlavorNote> flavorNotes,
            List<String> customFlavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight
    ) {
        update(
                name,
                roastery,
                country,
                CountryInfo.findCodeByName(country),
                region,
                farm,
                variety,
                altitude,
                processType,
                flavorNotes,
                customFlavorNotes,
                memo,
                roastedDate,
                purchasedDate,
                price,
                weight
        );
    }

    public void update(
            String name,
            String roastery,
            String country,
            String originCountryCode,
            String region,
            String farm,
            String variety,
            String altitude,
            ProcessType processType,
            List<FlavorNote> flavorNotes,
            List<String> customFlavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight
    ) {
        update(
                name,
                roastery,
                country,
                originCountryCode,
                region,
                farm,
                variety,
                altitude,
                processType,
                flavorNotes,
                customFlavorNotes,
                memo,
                roastedDate,
                purchasedDate,
                price,
                weight,
                status,
                purchasePlace
        );
    }

    public void update(
            String name,
            String roastery,
            String country,
            String originCountryCode,
            String region,
            String farm,
            String variety,
            String altitude,
            ProcessType processType,
            List<FlavorNote> flavorNotes,
            List<String> customFlavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight,
            CoffeeBeanStatus status,
            PurchasePlace purchasePlace
    ) {
        this.name = name;
        this.roastery = roastery;
        this.country = country;
        this.originCountryCode = normalizeCountryCode(originCountryCode);
        this.region = region;
        this.farm = farm;
        this.variety = variety;
        this.altitude = altitude;
        this.processType = processType;
        this.flavorNotes.clear();
        if (flavorNotes != null) {
            this.flavorNotes.addAll(flavorNotes);
        }
        this.customFlavorNotes.clear();
        if (customFlavorNotes != null) {
            this.customFlavorNotes.addAll(customFlavorNotes);
        }
        this.memo = memo;
        this.roastedDate = roastedDate;
        this.purchasedDate = purchasedDate;
        this.price = price;
        this.weight = weight;
        this.status = status != null ? status : CoffeeBeanStatus.CURRENT;
        this.purchasePlace = purchasePlace;
    }

    public void update(
            String name,
            String roastery,
            String country,
            String region,
            String farm,
            String variety,
            String altitude,
            ProcessType processType,
            List<FlavorNote> flavorNotes,
            List<String> customFlavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight,
            CoffeeBeanStatus status,
            PurchasePlace purchasePlace
    ) {
        update(
                name,
                roastery,
                country,
                CountryInfo.findCodeByName(country),
                region,
                farm,
                variety,
                altitude,
                processType,
                flavorNotes,
                customFlavorNotes,
                memo,
                roastedDate,
                purchasedDate,
                price,
                weight,
                status,
                purchasePlace
        );
    }

    public Integer useWeight(Integer usedWeight) {
        if (status != CoffeeBeanStatus.CURRENT || weight == null || usedWeight == null || usedWeight <= 0) {
            return 0;
        }
        int deductedWeight = Math.min(weight, usedWeight);
        this.weight = Math.max(0, weight - usedWeight);
        if (this.weight == 0) {
            this.status = CoffeeBeanStatus.FINISHED;
        }
        return deductedWeight;
    }

    public void restoreWeight(Integer restoredWeight) {
        if (status == CoffeeBeanStatus.CAFE || weight == null || restoredWeight == null || restoredWeight <= 0) {
            return;
        }
        this.weight += restoredWeight;
        if (status == CoffeeBeanStatus.FINISHED && this.weight > 0) {
            this.status = CoffeeBeanStatus.CURRENT;
        }
    }

    private static String normalizeCountryCode(String originCountryCode) {
        if (originCountryCode == null || originCountryCode.isBlank()) {
            return null;
        }
        return originCountryCode.trim().toUpperCase();
    }
}
