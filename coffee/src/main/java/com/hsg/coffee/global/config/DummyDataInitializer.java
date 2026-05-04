package com.hsg.coffee.global.config;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.entity.BrewFeelingTag;
import com.hsg.coffee.domain.brewRecord.entity.BrewMethod;
import com.hsg.coffee.domain.brewRecord.entity.BrewPourStep;
import com.hsg.coffee.domain.brewRecord.entity.BrewRecord;
import com.hsg.coffee.domain.brewRecord.entity.BrewTemperatureType;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.brewRecord.repository.BrewRecordRepository;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
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
    private final BrewRecordRepository brewRecordRepository;

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

        List<CoffeeBean> coffeeBeans = coffeeBeanRepository.saveAll(List.of(
                CoffeeBean.create(
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
                        "개발 확인용 더미 원두",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 2),
                        18000,
                        200,
                        CoffeeBeanStatus.CURRENT,
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
                        List.of(FlavorNote.BLACKCURRANT, FlavorNote.GRAPEFRUIT, FlavorNote.BROWN_SUGAR),
                        List.of("포도잼"),
                        "목록/검색 확인용 더미 원두",
                        LocalDate.of(2026, 4, 28),
                        LocalDate.of(2026, 5, 2),
                        22000,
                        0,
                        CoffeeBeanStatus.FINISHED,
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
                        List.of(FlavorNote.MANGO, FlavorNote.PEACH, FlavorNote.PINEAPPLE),
                        List.of("열대 과일"),
                        "수정/삭제 확인용 더미 원두",
                        LocalDate.of(2026, 4, 25),
                        LocalDate.of(2026, 5, 1),
                        25000,
                        null,
                        CoffeeBeanStatus.CAFE,
                        cafe
                )
        ));

        brewRecordRepository.saveAll(List.of(
                BrewRecord.create(
                        coffeeBeans.get(0),
                        LocalDate.of(2026, 5, 3),
                        BrewMethod.V60,
                        BrewTemperatureType.HOT,
                        java.math.BigDecimal.valueOf(15),
                        java.math.BigDecimal.valueOf(240),
                        java.math.BigDecimal.valueOf(92),
                        720,
                        150,
                        List.of(
                                BrewPourStep.of(0, 40),
                                BrewPourStep.of(40, 80),
                                BrewPourStep.of(85, 80),
                                BrewPourStep.of(125, 40)
                        ),
                        4,
                        4,
                        4,
                        2,
                        3,
                        5,
                        4,
                        List.of(BrewFeelingTag.CLEAN, BrewFeelingTag.BRIGHT, BrewFeelingTag.SWEET_FINISH),
                        List.of("맑은 여운"),
                        "향이 화사하고 마무리가 깨끗했다."
                ),
                BrewRecord.create(
                        coffeeBeans.get(1),
                        LocalDate.of(2026, 5, 2),
                        BrewMethod.ORIGAMI,
                        BrewTemperatureType.ICE,
                        java.math.BigDecimal.valueOf(16),
                        java.math.BigDecimal.valueOf(250),
                        java.math.BigDecimal.valueOf(93),
                        680,
                        165,
                        List.of(
                                BrewPourStep.of(0, 45),
                                BrewPourStep.of(35, 80),
                                BrewPourStep.of(80, 80),
                                BrewPourStep.of(125, 45)
                        ),
                        5,
                        5,
                        4,
                        2,
                        3,
                        4,
                        4,
                        List.of(BrewFeelingTag.JUICY, BrewFeelingTag.LONG_AFTERTASTE),
                        List.of("선명함"),
                        "산미가 또렷하고 베리 계열 인상이 선명했다."
                )
        ));
    }
}
