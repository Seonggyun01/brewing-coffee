package com.hsg.coffee.domain.brewRecord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hsg.coffee.domain.brewRecord.entity.BrewRecord;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;

public interface BrewRecordRepository extends JpaRepository<BrewRecord, Long> {

    List<BrewRecord> findAllByOrderByBrewedDateDescIdDesc();

    List<BrewRecord> findByCoffeeBeanStatusNotOrderByBrewedDateDescIdDesc(CoffeeBeanStatus status);

    List<BrewRecord> findByCoffeeBeanOwnerIdAndCoffeeBeanStatusNotOrderByBrewedDateDescIdDesc(
            Long ownerId,
            CoffeeBeanStatus status
    );

    java.util.Optional<BrewRecord> findByIdAndCoffeeBeanOwnerId(Long id, Long ownerId);

    List<BrewRecord> findByCoffeeBeanIdOrderByBrewedDateDescIdDesc(Long coffeeBeanId);

    List<BrewRecord> findByCoffeeBeanOwnerIdAndCoffeeBeanIdOrderByBrewedDateDescIdDesc(Long ownerId, Long coffeeBeanId);

    List<BrewRecord> findByCoffeeBeanOwnerIdOrderByBrewedDateDescIdDesc(Long ownerId);

    List<BrewRecord> findByCoffeeBeanStatusOrderByBrewedDateDescIdDesc(CoffeeBeanStatus status);

    List<BrewRecord> findByCoffeeBeanOwnerIdAndCoffeeBeanStatusOrderByBrewedDateDescIdDesc(
            Long ownerId,
            CoffeeBeanStatus status
    );

    List<BrewRecord> findByCoffeeBeanStatusAndCoffeeBeanPurchasePlaceIdInOrderByBrewedDateDescIdDesc(
            CoffeeBeanStatus status,
            List<Long> purchasePlaceIds
    );

    List<BrewRecord> findByCoffeeBeanOwnerIdAndCoffeeBeanStatusAndCoffeeBeanPurchasePlaceIdInOrderByBrewedDateDescIdDesc(
            Long ownerId,
            CoffeeBeanStatus status,
            List<Long> purchasePlaceIds
    );

    @Query("select br.coffeeBean.originCountryCode, count(br) from BrewRecord br "
            + "where br.coffeeBean.originCountryCode is not null group by br.coffeeBean.originCountryCode")
    List<Object[]> countBrewRecordsGroupByOriginCountryCode();

    @Query("select br.coffeeBean.originCountryCode, count(br) from BrewRecord br "
            + "where br.coffeeBean.owner.id = :ownerId and br.coffeeBean.originCountryCode is not null "
            + "group by br.coffeeBean.originCountryCode")
    List<Object[]> countBrewRecordsGroupByOwnerIdAndOriginCountryCode(Long ownerId);
}
