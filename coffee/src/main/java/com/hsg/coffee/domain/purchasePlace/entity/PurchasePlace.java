package com.hsg.coffee.domain.purchasePlace.entity;

import com.hsg.coffee.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "purchase_places")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchasePlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchasePlaceType type;

    @Column(length = 300)
    private String address;

    private Double latitude;

    private Double longitude;

    @Column(length = 1000)
    private String memo;

    public static PurchasePlace create(
            String name,
            PurchasePlaceType type,
            String address,
            Double latitude,
            Double longitude,
            String memo
    ) {
        PurchasePlace purchasePlace = new PurchasePlace();
        purchasePlace.name = name;
        purchasePlace.type = type;
        purchasePlace.address = address;
        purchasePlace.latitude = latitude;
        purchasePlace.longitude = longitude;
        purchasePlace.memo = memo;
        return purchasePlace;
    }

    public void update(
            String name,
            PurchasePlaceType type,
            String address,
            Double latitude,
            Double longitude,
            String memo
    ) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.memo = memo;
    }
}
