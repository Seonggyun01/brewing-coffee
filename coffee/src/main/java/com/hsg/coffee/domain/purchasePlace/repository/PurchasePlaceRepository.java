package com.hsg.coffee.domain.purchasePlace.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;

public interface PurchasePlaceRepository extends JpaRepository<PurchasePlace, Long> {

    java.util.List<PurchasePlace> findAllByOrderByNameAsc();
}
