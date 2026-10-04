package com.hsg.coffee.domain.purchasePlace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.user.entity.AppUser;

public interface PurchasePlaceRepository extends JpaRepository<PurchasePlace, Long> {

    List<PurchasePlace> findAllByOrderByNameAsc();

    List<PurchasePlace> findByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<PurchasePlace> findByIdAndOwnerId(Long id, Long ownerId);

    List<PurchasePlace> findByTypeOrderByNameAsc(PurchasePlaceType type);

    List<PurchasePlace> findByOwnerIdAndTypeOrderByNameAsc(Long ownerId, PurchasePlaceType type);

    List<PurchasePlace> findByTypeAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByNameAsc(PurchasePlaceType type);

    List<PurchasePlace> findByOwnerIdAndTypeAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByNameAsc(
            Long ownerId,
            PurchasePlaceType type
    );

    Optional<PurchasePlace> findFirstByPlaceUrl(String placeUrl);

    Optional<PurchasePlace> findFirstByOwnerIdAndPlaceUrl(Long ownerId, String placeUrl);

    Optional<PurchasePlace> findFirstByTypeAndLatitudeAndLongitude(PurchasePlaceType type, Double latitude, Double longitude);

    Optional<PurchasePlace> findFirstByOwnerIdAndTypeAndLatitudeAndLongitude(
            Long ownerId,
            PurchasePlaceType type,
            Double latitude,
            Double longitude
    );

    Optional<PurchasePlace> findFirstByTypeAndNameIgnoreCaseAndAddressIgnoreCase(
            PurchasePlaceType type,
            String name,
            String address
    );

    Optional<PurchasePlace> findFirstByOwnerIdAndTypeAndNameIgnoreCaseAndAddressIgnoreCase(
            Long ownerId,
            PurchasePlaceType type,
            String name,
            String address
    );

    Optional<PurchasePlace> findFirstByTypeAndNameIgnoreCase(PurchasePlaceType type, String name);

    Optional<PurchasePlace> findFirstByOwnerIdAndTypeAndNameIgnoreCase(Long ownerId, PurchasePlaceType type, String name);

    @Query("select distinct p from BrewRecord br "
            + "join br.coffeeBean cb "
            + "join cb.purchasePlace p "
            + "where cb.status = :status and p.type = :type "
            + "order by p.name asc")
    List<PurchasePlace> findDistinctByBrewRecordCoffeeBeanStatusAndTypeOrderByNameAsc(
            CoffeeBeanStatus status,
            PurchasePlaceType type
    );

    @Query("select distinct p from BrewRecord br "
            + "join br.coffeeBean cb "
            + "join cb.purchasePlace p "
            + "where cb.owner.id = :ownerId and cb.status = :status and p.type = :type "
            + "order by p.name asc")
    List<PurchasePlace> findDistinctByOwnerAndBrewRecordCoffeeBeanStatusAndTypeOrderByNameAsc(
            @Param("ownerId") Long ownerId,
            @Param("status") CoffeeBeanStatus status,
            @Param("type") PurchasePlaceType type
    );

    @Modifying(clearAutomatically = true)
    @Query("update PurchasePlace p set p.owner = :owner where p.owner is null")
    int assignOwnerToUnowned(@Param("owner") AppUser owner);
}
