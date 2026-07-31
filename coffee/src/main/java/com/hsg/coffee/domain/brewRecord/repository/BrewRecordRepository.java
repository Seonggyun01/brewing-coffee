package com.hsg.coffee.domain.brewRecord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hsg.coffee.domain.brewRecord.entity.BrewRecord;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;

public interface BrewRecordRepository extends JpaRepository<BrewRecord, Long> {

    List<BrewRecord> findAllByOrderByBrewedDateDescIdDesc();

    List<BrewRecord> findByCoffeeBeanStatusNotOrderByBrewedDateDescIdDesc(CoffeeBeanStatus status);

    List<BrewRecord> findByCoffeeBeanIdOrderByBrewedDateDescIdDesc(Long coffeeBeanId);

    List<BrewRecord> findByCoffeeBeanStatusOrderByBrewedDateDescIdDesc(CoffeeBeanStatus status);

    @Query("select br.coffeeBean.originCountryCode, count(br) from BrewRecord br "
            + "where br.coffeeBean.originCountryCode is not null group by br.coffeeBean.originCountryCode")
    List<Object[]> countBrewRecordsGroupByOriginCountryCode();
}
