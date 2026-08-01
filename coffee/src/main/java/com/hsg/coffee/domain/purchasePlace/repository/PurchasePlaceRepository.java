package com.hsg.coffee.domain.purchasePlace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;

public interface PurchasePlaceRepository extends JpaRepository<PurchasePlace, Long> {

    List<PurchasePlace> findAllByOrderByNameAsc();

    List<PurchasePlace> findByTypeOrderByNameAsc(PurchasePlaceType type);

    List<PurchasePlace> findByTypeAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByNameAsc(PurchasePlaceType type);

    Optional<PurchasePlace> findFirstByPlaceUrl(String placeUrl);

    Optional<PurchasePlace> findFirstByTypeAndLatitudeAndLongitude(PurchasePlaceType type, Double latitude, Double longitude);

    Optional<PurchasePlace> findFirstByTypeAndNameIgnoreCaseAndAddressIgnoreCase(
            PurchasePlaceType type,
            String name,
            String address
    );

    Optional<PurchasePlace> findFirstByTypeAndNameIgnoreCase(PurchasePlaceType type, String name);

    @Query("select distinct p from BrewRecord br "
            + "join br.coffeeBean cb "
            + "join cb.purchasePlace p "
            + "where cb.status = :status and p.type = :type "
            + "order by p.name asc")
    List<PurchasePlace> findDistinctByBrewRecordCoffeeBeanStatusAndTypeOrderByNameAsc(
            CoffeeBeanStatus status,
            PurchasePlaceType type
    );
}
