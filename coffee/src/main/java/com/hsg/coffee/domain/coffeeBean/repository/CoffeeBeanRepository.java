package com.hsg.coffee.domain.coffeeBean.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;

public interface CoffeeBeanRepository extends JpaRepository<CoffeeBean, Long> {

    List<CoffeeBean> findAllByOrderByIdDesc();

    List<CoffeeBean> findByNameContainingIgnoreCaseOrderByIdDesc(String name);

    List<CoffeeBean> findByRoasteryContainingIgnoreCaseOrderByIdDesc(String roastery);
}
