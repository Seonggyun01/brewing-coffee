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
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "brewlog.dummy-data.enabled", havingValue = "true")
public class DummyDataInitializer implements ApplicationRunner {

    private final CoffeeBeanRepository coffeeBeanRepository;
    private final PurchasePlaceRepository purchasePlaceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (coffeeBeanRepository.count() > 0) {
            return;
        }

        PurchasePlace fritz = purchasePlaceRepository.save(PurchasePlace.create(
                "프릳츠 원서점",
                PurchasePlaceType.ROASTERY,
                "서울 종로구 율곡로 83",
                37.575864,
                126.989499,
                "지도 표시 확인용 로스터리 구매처"
        ));
        PurchasePlace smartStore = purchasePlaceRepository.save(PurchasePlace.create(
                "네이버 스마트스토어",
                PurchasePlaceType.ONLINE,
                null,
                null,
                null,
                "온라인 구매처 예시"
        ));
        PurchasePlace cafe = purchasePlaceRepository.save(PurchasePlace.create(
                "샘플 카페",
                PurchasePlaceType.CAFE,
                "서울 성동구 성수동",
                37.544579,
                127.055961,
                "카페 판매 원두 구매처 예시"
        ));

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
                        200,
                        fritz
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
                        200,
                        smartStore
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
                        100,
                        cafe
                )
        ));
    }
}
