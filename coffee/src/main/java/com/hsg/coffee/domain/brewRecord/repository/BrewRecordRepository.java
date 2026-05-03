package com.hsg.coffee.domain.brewRecord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hsg.coffee.domain.brewRecord.entity.BrewRecord;

public interface BrewRecordRepository extends JpaRepository<BrewRecord, Long> {

    List<BrewRecord> findAllByOrderByBrewedDateDescIdDesc();

    List<BrewRecord> findByCoffeeBeanIdOrderByBrewedDateDescIdDesc(Long coffeeBeanId);
}
