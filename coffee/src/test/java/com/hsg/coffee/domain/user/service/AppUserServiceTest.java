package com.hsg.coffee.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;
import com.hsg.coffee.domain.user.entity.AppUser;

import jakarta.persistence.EntityManager;

@Transactional
@SpringBootTest
class AppUserServiceTest {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @Autowired
    private PurchasePlaceRepository purchasePlaceRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void firstLoginAssignsExistingDataToTheAccount() {
        PurchasePlace place = purchasePlaceRepository.save(PurchasePlace.create(
                "기존 구매처",
                PurchasePlaceType.ROASTERY,
                "서울",
                null,
                null,
                null,
                null
        ));
        CoffeeBean bean = coffeeBeanRepository.save(CoffeeBean.create(
                "기존 원두",
                "로스터리",
                "Ethiopia",
                "Guji",
                null,
                null,
                null,
                ProcessType.WASHED,
                List.of(FlavorNote.JASMINE),
                List.of(),
                null,
                LocalDate.of(2026, 10, 1),
                null,
                null,
                200,
                place
        ));

        AppUser user = appUserService.loginWithKakao("123456789", "첫 사용자", null);
        entityManager.flush();
        entityManager.clear();

        assertEquals(user.getId(), purchasePlaceRepository.findById(place.getId()).orElseThrow().getOwner().getId());
        assertEquals(user.getId(), coffeeBeanRepository.findById(bean.getId()).orElseThrow().getOwner().getId());
    }
}
