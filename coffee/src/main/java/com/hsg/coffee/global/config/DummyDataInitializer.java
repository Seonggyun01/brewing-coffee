package com.hsg.coffee.global.config;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "brewlog.dummy-data.enabled", havingValue = "true")
public class DummyDataInitializer implements ApplicationRunner {

    private final CoffeeBeanRepository coffeeBeanRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (coffeeBeanRepository.count() > 0) {
            return;
        }

        coffeeBeanRepository.saveAll(List.of(
                CoffeeBean.create(
                        "에티오피아 구지",
                        "브루잉 로스터스",
                        "Ethiopia",
                        "Guji",
                        "Hambela",
                        "Heirloom",
                        "1900-2100m",
                        ProcessType.WASHED,
                        "floral, citrus, tea-like",
                        "개발 확인용 더미 원두",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 2),
                        18000,
                        200
                ),
                CoffeeBean.create(
                        "케냐 키린야가",
                        "데일리 로스터스",
                        "Kenya",
                        "Kirinyaga",
                        "Kii Factory",
                        "SL28, SL34",
                        "1600-1800m",
                        ProcessType.WASHED,
                        "blackcurrant, grapefruit, brown sugar",
                        "목록/검색 확인용 더미 원두",
                        LocalDate.of(2026, 4, 28),
                        LocalDate.of(2026, 5, 2),
                        22000,
                        200
                ),
                CoffeeBean.create(
                        "콜롬비아 엘 파라이소",
                        "샘플 로스터리",
                        "Colombia",
                        "Cauca",
                        "El Paraiso",
                        "Castillo",
                        "1850m",
                        ProcessType.ANAEROBIC,
                        "mango, peach, tropical",
                        "수정/삭제 확인용 더미 원두",
                        LocalDate.of(2026, 4, 25),
                        LocalDate.of(2026, 5, 1),
                        25000,
                        100
                )
        ));
    }
}
