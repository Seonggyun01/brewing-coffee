package com.hsg.coffee.domain.coffeeBean.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;

@Transactional
@SpringBootTest
class CoffeeBeanRepositoryTest {

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @Test
    void saveAndFindById() {
        CoffeeBean coffeeBean = CoffeeBean.create(
                "에티오피아 구지",
                "브루잉 로스터스",
                "Ethiopia",
                "Guji",
                "Hambela",
                "Heirloom",
                "1900-2100m",
                ProcessType.WASHED,
                List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH),
                List.of("오렌지 껍질"),
                "첫 원두 테스트 기록",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                18000,
                200
        );

        CoffeeBean savedCoffeeBean = coffeeBeanRepository.save(coffeeBean);

        Optional<CoffeeBean> foundCoffeeBean = coffeeBeanRepository.findById(savedCoffeeBean.getId());

        assertTrue(foundCoffeeBean.isPresent());
        CoffeeBean result = foundCoffeeBean.get();

        System.out.println("=== CoffeeBeanRepository 저장/조회 테스트 결과 ===");
        System.out.println("저장 ID: " + result.getId());
        System.out.println("원두 이름: " + result.getName());
        System.out.println("로스터리: " + result.getRoastery());
        System.out.println("국가/지역: " + result.getCountry() + " / " + result.getRegion());
        System.out.println("농장: " + result.getFarm());
        System.out.println("품종: " + result.getVariety());
        System.out.println("고도: " + result.getAltitude());
        System.out.println("가공 방식: " + result.getProcessType());
        System.out.println("향미 노트: " + result.getFlavorNotes());
        System.out.println("메모: " + result.getMemo());
        System.out.println("로스팅일: " + result.getRoastedDate());
        System.out.println("구매일: " + result.getPurchasedDate());
        System.out.println("가격: " + result.getPrice());
        System.out.println("용량: " + result.getWeight());

        assertNotNull(savedCoffeeBean.getId());
        assertEquals("에티오피아 구지", result.getName());
        assertEquals(ProcessType.WASHED, result.getProcessType());
        assertEquals(18000, result.getPrice());
        assertEquals(200, result.getWeight());
    }
}
