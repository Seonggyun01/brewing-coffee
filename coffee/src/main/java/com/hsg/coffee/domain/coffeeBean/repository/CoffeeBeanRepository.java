package com.hsg.coffee.domain.coffeeBean.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.user.entity.AppUser;

public interface CoffeeBeanRepository extends JpaRepository<CoffeeBean, Long> {

    List<CoffeeBean> findAllByOrderByIdDesc();

    List<CoffeeBean> findByOwnerIdOrderByIdDesc(Long ownerId);

    java.util.Optional<CoffeeBean> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    List<CoffeeBean> findByNameContainingIgnoreCaseOrderByIdDesc(String name);

    List<CoffeeBean> findByOwnerIdAndNameContainingIgnoreCaseOrderByIdDesc(Long ownerId, String name);

    List<CoffeeBean> findByRoasteryContainingIgnoreCaseOrderByIdDesc(String roastery);

    List<CoffeeBean> findByOwnerIdAndRoasteryContainingIgnoreCaseOrderByIdDesc(Long ownerId, String roastery);

    List<CoffeeBean> findByOriginCountryCodeOrderByIdDesc(String originCountryCode);

    List<CoffeeBean> findByOwnerIdAndOriginCountryCodeOrderByIdDesc(Long ownerId, String originCountryCode);

    List<CoffeeBean> findByOriginCountryCodeAndNameContainingIgnoreCaseOrderByIdDesc(String originCountryCode, String name);

    List<CoffeeBean> findByOwnerIdAndOriginCountryCodeAndNameContainingIgnoreCaseOrderByIdDesc(
            Long ownerId,
            String originCountryCode,
            String name
    );

    List<CoffeeBean> findTop8ByOriginCountryCodeOrderByIdDesc(String originCountryCode);

    List<CoffeeBean> findTop8ByOwnerIdAndOriginCountryCodeOrderByIdDesc(Long ownerId, String originCountryCode);

    @Query("select cb.originCountryCode, count(cb) from CoffeeBean cb "
            + "where cb.originCountryCode is not null group by cb.originCountryCode")
    List<Object[]> countBeansGroupByOriginCountryCode();

    @Query("select cb.originCountryCode, count(cb) from CoffeeBean cb "
            + "where cb.owner.id = :ownerId and cb.originCountryCode is not null group by cb.originCountryCode")
    List<Object[]> countBeansGroupByOwnerIdAndOriginCountryCode(@Param("ownerId") Long ownerId);

    @Modifying(clearAutomatically = true)
    @Query("update CoffeeBean cb set cb.owner = :owner where cb.owner is null")
    int assignOwnerToUnowned(@Param("owner") AppUser owner);
}
