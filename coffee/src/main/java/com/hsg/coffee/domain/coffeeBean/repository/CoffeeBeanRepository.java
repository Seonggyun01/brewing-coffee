package com.hsg.coffee.domain.coffeeBean.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;

public interface CoffeeBeanRepository extends JpaRepository<CoffeeBean, Long> {

    List<CoffeeBean> findAllByOrderByIdDesc();

    List<CoffeeBean> findByNameContainingIgnoreCaseOrderByIdDesc(String name);

    List<CoffeeBean> findByRoasteryContainingIgnoreCaseOrderByIdDesc(String roastery);

    List<CoffeeBean> findByOriginCountryCodeOrderByIdDesc(String originCountryCode);

    List<CoffeeBean> findByOriginCountryCodeAndNameContainingIgnoreCaseOrderByIdDesc(String originCountryCode, String name);

    List<CoffeeBean> findTop3ByOriginCountryCodeOrderByIdDesc(String originCountryCode);

    @Query("select cb.originCountryCode, count(cb) from CoffeeBean cb "
            + "where cb.originCountryCode is not null group by cb.originCountryCode")
    List<Object[]> countBeansGroupByOriginCountryCode();
}
