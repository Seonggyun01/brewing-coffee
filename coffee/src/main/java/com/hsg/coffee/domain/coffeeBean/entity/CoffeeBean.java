package com.hsg.coffee.domain.coffeeBean.entity;

import java.time.LocalDate;

import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @Column(length = 500)
    private String flavorNotes;

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
            String flavorNotes,
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
                region,
                farm,
                variety,
                altitude,
                processType,
                flavorNotes,
                memo,
                roastedDate,
                purchasedDate,
                price,
                weight,
                null
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
            String flavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight,
            PurchasePlace purchasePlace
    ) {
        CoffeeBean coffeeBean = new CoffeeBean();
        coffeeBean.name = name;
        coffeeBean.roastery = roastery;
        coffeeBean.country = country;
        coffeeBean.region = region;
        coffeeBean.farm = farm;
        coffeeBean.variety = variety;
        coffeeBean.altitude = altitude;
        coffeeBean.processType = processType;
        coffeeBean.flavorNotes = flavorNotes;
        coffeeBean.memo = memo;
        coffeeBean.roastedDate = roastedDate;
        coffeeBean.purchasedDate = purchasedDate;
        coffeeBean.price = price;
        coffeeBean.weight = weight;
        coffeeBean.purchasePlace = purchasePlace;
        return coffeeBean;
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
            String flavorNotes,
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
                region,
                farm,
                variety,
                altitude,
                processType,
                flavorNotes,
                memo,
                roastedDate,
                purchasedDate,
                price,
                weight,
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
            String flavorNotes,
            String memo,
            LocalDate roastedDate,
            LocalDate purchasedDate,
            Integer price,
            Integer weight,
            PurchasePlace purchasePlace
    ) {
        this.name = name;
        this.roastery = roastery;
        this.country = country;
        this.region = region;
        this.farm = farm;
        this.variety = variety;
        this.altitude = altitude;
        this.processType = processType;
        this.flavorNotes = flavorNotes;
        this.memo = memo;
        this.roastedDate = roastedDate;
        this.purchasedDate = purchasedDate;
        this.price = price;
        this.weight = weight;
        this.purchasePlace = purchasePlace;
    }
}
