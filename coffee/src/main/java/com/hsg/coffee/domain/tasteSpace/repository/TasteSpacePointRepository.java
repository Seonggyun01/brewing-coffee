package com.hsg.coffee.domain.tasteSpace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hsg.coffee.domain.tasteSpace.entity.TasteSpacePoint;

public interface TasteSpacePointRepository extends JpaRepository<TasteSpacePoint, Long> {

    List<TasteSpacePoint> findByOwnerIdOrderByCoffeeBeanId(Long ownerId);

    void deleteByOwnerId(Long ownerId);
}
