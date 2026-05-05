package com.hsg.coffee.domain.purchasePlace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;

public interface PurchasePlaceRepository extends JpaRepository<PurchasePlace, Long> {

    List<PurchasePlace> findAllByOrderByNameAsc();

    List<PurchasePlace> findByTypeAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByNameAsc(PurchasePlaceType type);
}
