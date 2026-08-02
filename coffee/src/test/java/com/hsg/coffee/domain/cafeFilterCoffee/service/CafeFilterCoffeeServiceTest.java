package com.hsg.coffee.domain.cafeFilterCoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.dto.BrewRecordResponse;
import com.hsg.coffee.domain.brewRecord.entity.BrewTemperatureType;
import com.hsg.coffee.domain.brewRecord.repository.BrewRecordRepository;
import com.hsg.coffee.domain.cafeFilterCoffee.dto.CafeFilterCoffeeForm;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;

@Transactional
@SpringBootTest
class CafeFilterCoffeeServiceTest {

    @Autowired
    private CafeFilterCoffeeService cafeFilterCoffeeService;

    @Autowired
    private BrewRecordRepository brewRecordRepository;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @Autowired
    private PurchasePlaceRepository purchasePlaceRepository;

    @BeforeEach
    void setUp() {
        brewRecordRepository.deleteAll();
        coffeeBeanRepository.deleteAll();
        purchasePlaceRepository.deleteAll();
    }

    @Test
    void createWithTasteProfile() {
        CafeFilterCoffeeForm form = new CafeFilterCoffeeForm();
        form.setName("카페 필터 테스트 원두");
        form.setCafeName("테스트 카페");
        form.setVisitedDate(LocalDate.of(2026, 8, 2));
        form.setTemperatureType(BrewTemperatureType.ICE);
        form.setRating(4);
        form.setAcidity(4);
        form.setSweetness(5);
        form.setBitterness(2);
        form.setBody(3);
        form.setAroma(4);
        form.setBalance(5);

        Long id = cafeFilterCoffeeService.create(form);

        BrewRecordResponse response = cafeFilterCoffeeService.get(id);
        assertEquals("카페 필터 테스트 원두", response.getCoffeeBeanName());
        assertEquals("테스트 카페", response.getPurchasePlaceName());
        assertEquals(BrewTemperatureType.ICE, response.getTemperatureType());
        assertEquals(4, response.getAcidity());
        assertEquals(5, response.getSweetness());
        assertEquals(2, response.getBitterness());
        assertEquals(3, response.getBody());
        assertEquals(4, response.getAroma());
        assertEquals(5, response.getBalance());
        assertEquals("4,5,2,3,4,5", response.getTasteChartValues());
    }
}
