package com.hsg.coffee.domain.coffeeBean.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;

public interface CoffeeBeanRepository extends JpaRepository<CoffeeBean, Long> {
}
