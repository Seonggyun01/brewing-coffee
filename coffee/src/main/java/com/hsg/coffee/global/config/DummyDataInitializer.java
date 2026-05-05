package com.hsg.coffee.global.config;

import java.math.BigDecimal;
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
                ),
                CoffeeBean.create(
                        "과테말라 엘 소코로",
                        "프릳츠",
                        "Guatemala",
                        "Palencia",
                        "El Socorro",
                        "Bourbon",
                        "1500-1700m",
                        ProcessType.WASHED,
                        List.of(FlavorNote.ORANGE, FlavorNote.CARAMEL, FlavorNote.HAZELNUT),
                        List.of("밀크티"),
                        "아침용으로 편하게 마시기 좋은 원두",
                        LocalDate.of(2026, 5, 3),
                        LocalDate.of(2026, 5, 4),
                        19500,
                        180,
                        CoffeeBeanStatus.CURRENT,
                        fritz
                ),
                CoffeeBean.create(
                        "브라질 파젠다 산타 이네스",
                        "데일리 로스터스",
                        "Brazil",
                        "Minas Gerais",
                        "Fazenda Santa Ines",
                        "Yellow Bourbon",
                        "1100-1300m",
                        ProcessType.NATURAL,
                        List.of(FlavorNote.MILK_CHOCOLATE, FlavorNote.ROASTED_NUTS, FlavorNote.BROWN_SUGAR),
                        List.of("고소한 단맛"),
                        "라떼와 아이스 브루잉 테스트용",
                        LocalDate.of(2026, 4, 30),
                        LocalDate.of(2026, 5, 1),
                        16000,
                        120,
                        CoffeeBeanStatus.CURRENT,
                        smartStore
                ),
                CoffeeBean.create(
                        "르완다 부산제",
                        "브루잉 로스터스",
                        "Rwanda",
                        "Nyamasheke",
                        "Busanze",
                        "Red Bourbon",
                        "1700-1900m",
                        ProcessType.WASHED,
                        List.of(FlavorNote.CRANBERRY, FlavorNote.RED_APPLE, FlavorNote.HONEY),
                        List.of("홍차"),
                        "산미 변화 비교용 소진 원두",
                        LocalDate.of(2026, 4, 15),
                        LocalDate.of(2026, 4, 17),
                        21000,
                        0,
                        CoffeeBeanStatus.FINISHED,
                        fritz
                ),
                CoffeeBean.create(
                        "코스타리카 라스 라하스",
                        "샘플 로스터리",
                        "Costa Rica",
                        "Central Valley",
                        "Las Lajas",
                        "Caturra, Catuai",
                        "1450m",
                        ProcessType.HONEY,
                        List.of(FlavorNote.GRAPE, FlavorNote.MAPLE_SYRUP, FlavorNote.PLUM),
                        List.of("잘 익은 과실"),
                        "허니 프로세스 기록 보관용",
                        LocalDate.of(2026, 4, 10),
                        LocalDate.of(2026, 4, 12),
                        23500,
                        0,
                        CoffeeBeanStatus.FINISHED,
                        smartStore
                ),
                CoffeeBean.create(
                        "파나마 게이샤 내추럴",
                        "카페 오로라",
                        "Panama",
                        "Boquete",
                        "Janson",
                        "Geisha",
                        "1600m",
                        ProcessType.NATURAL,
                        List.of(FlavorNote.JASMINE_TEA, FlavorNote.WHITE_PEACH, FlavorNote.LYCHEE),
                        List.of("청포도"),
                        "카페에서 시식한 고가 원두",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 3),
                        38000,
                        null,
                        CoffeeBeanStatus.CAFE,
                        cafe
                ),
                CoffeeBean.create(
                        "인도네시아 아체 가요",
                        "카페 오로라",
                        "Indonesia",
                        "Aceh",
                        "Gayo",
                        "Typica",
                        "1400-1600m",
                        ProcessType.NATURAL,
                        List.of(FlavorNote.CEDAR, FlavorNote.DARK_CHOCOLATE, FlavorNote.CLOVE),
                        List.of("묵직한 향신료"),
                        "카페 시식 비교용",
                        LocalDate.of(2026, 4, 22),
                        LocalDate.of(2026, 5, 2),
                        19000,
                        null,
                        CoffeeBeanStatus.CAFE,
                        cafe
                ),
                CoffeeBean.create(
                        "에티오피아 예가체프 코체레",
                        "브루잉 로스터스",
                        "Ethiopia",
                        "Yirgacheffe",
                        "Kochere",
                        "Heirloom",
                        "1900-2200m",
                        ProcessType.WASHED,
                        List.of(FlavorNote.JASMINE_TEA, FlavorNote.LEMON, FlavorNote.HONEY),
                        List.of("베르가못"),
                        "원산지 지도 색상 단계 확인용 더미 원두",
                        LocalDate.of(2026, 5, 5),
                        LocalDate.of(2026, 5, 5),
                        21000,
                        180,
                        CoffeeBeanStatus.CURRENT,
                        fritz
                ),
                CoffeeBean.create(
                        "에티오피아 시다마 벤사",
                        "데일리 로스터스",
                        "Ethiopia",
                        "Sidama",
                        "Bensa",
                        "74158",
                        "2000m",
                        ProcessType.NATURAL,
                        List.of(FlavorNote.STRAWBERRY, FlavorNote.PEACH, FlavorNote.BLACK_TEA),
                        List.of("딸기잼"),
                        "원산지 지도 색상 단계 확인용 더미 원두",
                        LocalDate.of(2026, 5, 4),
                        LocalDate.of(2026, 5, 5),
                        23000,
                        0,
                        CoffeeBeanStatus.FINISHED,
                        smartStore
                ),
                CoffeeBean.create(
                        "에티오피아 리무 게라",
                        "샘플 로스터리",
                        "Ethiopia",
                        "Limu",
                        "Gera",
                        "Heirloom",
                        "1800-2100m",
                        ProcessType.HONEY,
                        List.of(FlavorNote.ORANGE_BLOSSOM, FlavorNote.APRICOT, FlavorNote.CARAMEL),
                        List.of("살구청"),
                        "원산지 지도 색상 단계 확인용 더미 원두",
                        LocalDate.of(2026, 5, 2),
                        LocalDate.of(2026, 5, 5),
                        20500,
                        null,
                        CoffeeBeanStatus.CAFE,
                        cafe
                ),
                CoffeeBean.create(
                        "에티오피아 아리차 내추럴",
                        "브루잉 로스터스",
                        "Ethiopia",
                        "Yirgacheffe",
                        "Aricha",
                        "Heirloom",
                        "1950-2100m",
                        ProcessType.NATURAL,
                        List.of(FlavorNote.BLUEBERRY, FlavorNote.LYCHEE, FlavorNote.HONEY),
                        List.of("블루베리 콤포트"),
                        "패널 내부 스크롤 확인용 더미 원두",
                        LocalDate.of(2026, 5, 6),
                        LocalDate.of(2026, 5, 6),
                        24000,
                        160,
                        CoffeeBeanStatus.CURRENT,
                        fritz
                ),
                CoffeeBean.create(
                        "에티오피아 부쿠 아벨 워시드",
                        "데일리 로스터스",
                        "Ethiopia",
                        "Sidama",
                        "Buku Abel",
                        "74110",
                        "2100m",
                        ProcessType.WASHED,
                        List.of(FlavorNote.EARL_GREY, FlavorNote.LEMON, FlavorNote.WHITE_PEACH),
                        List.of("레몬 티"),
                        "패널 내부 스크롤 확인용 더미 원두",
                        LocalDate.of(2026, 5, 6),
                        LocalDate.of(2026, 5, 6),
                        22500,
                        0,
                        CoffeeBeanStatus.FINISHED,
                        smartStore
                ),
                CoffeeBean.create(
                        "에티오피아 하로 수케",
                        "카페 오로라",
                        "Ethiopia",
                        "Guji",
                        "Haro Suke",
                        "Heirloom",
                        "2000m",
                        ProcessType.HONEY,
                        List.of(FlavorNote.MANGO, FlavorNote.ORANGE_BLOSSOM, FlavorNote.BLACK_TEA),
                        List.of("망고 티"),
                        "패널 내부 스크롤 확인용 더미 원두",
                        LocalDate.of(2026, 5, 5),
                        LocalDate.of(2026, 5, 6),
                        26000,
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
                ),
                BrewRecord.create(
                        coffeeBeans.get(3),
                        LocalDate.of(2026, 5, 4),
                        BrewMethod.KALITA,
                        BrewTemperatureType.HOT,
                        BigDecimal.valueOf(16),
                        BigDecimal.valueOf(250),
                        BigDecimal.valueOf(91),
                        760,
                        170,
                        List.of(
                                BrewPourStep.of(0, 45),
                                BrewPourStep.of(35, 70),
                                BrewPourStep.of(75, 70),
                                BrewPourStep.of(125, 65)
                        ),
                        4,
                        3,
                        4,
                        2,
                        4,
                        4,
                        5,
                        List.of(BrewFeelingTag.SMOOTH, BrewFeelingTag.SWEET_FINISH),
                        List.of("편안함"),
                        "카라멜 단맛과 너티함이 안정적으로 이어졌다."
                ),
                BrewRecord.create(
                        coffeeBeans.get(4),
                        LocalDate.of(2026, 5, 4),
                        BrewMethod.AEROPRESS,
                        BrewTemperatureType.ICE,
                        BigDecimal.valueOf(18),
                        BigDecimal.valueOf(220),
                        BigDecimal.valueOf(88),
                        620,
                        135,
                        List.of(
                                BrewPourStep.of(0, 80),
                                BrewPourStep.of(45, 70),
                                BrewPourStep.of(95, 70)
                        ),
                        4,
                        2,
                        5,
                        3,
                        5,
                        3,
                        4,
                        List.of(BrewFeelingTag.RICH, BrewFeelingTag.CREAMY),
                        List.of("초콜릿"),
                        "차갑게 마셔도 바디감이 유지되고 단맛이 좋았다."
                ),
                BrewRecord.create(
                        coffeeBeans.get(5),
                        LocalDate.of(2026, 4, 24),
                        BrewMethod.V60,
                        BrewTemperatureType.HOT,
                        BigDecimal.valueOf(15),
                        BigDecimal.valueOf(230),
                        BigDecimal.valueOf(93),
                        700,
                        145,
                        List.of(
                                BrewPourStep.of(0, 35),
                                BrewPourStep.of(30, 65),
                                BrewPourStep.of(70, 65),
                                BrewPourStep.of(110, 65)
                        ),
                        4,
                        4,
                        4,
                        2,
                        3,
                        4,
                        4,
                        List.of(BrewFeelingTag.CLEAN, BrewFeelingTag.LIGHT),
                        List.of("차 같은 질감"),
                        "크랜베리 산미와 꿀 같은 단맛이 가볍게 남았다."
                ),
                BrewRecord.create(
                        coffeeBeans.get(7),
                        LocalDate.of(2026, 5, 3),
                        BrewMethod.ORIGAMI,
                        BrewTemperatureType.HOT,
                        BigDecimal.valueOf(15),
                        BigDecimal.valueOf(225),
                        BigDecimal.valueOf(90),
                        730,
                        155,
                        List.of(
                                BrewPourStep.of(0, 40),
                                BrewPourStep.of(35, 60),
                                BrewPourStep.of(75, 65),
                                BrewPourStep.of(120, 60)
                        ),
                        5,
                        4,
                        5,
                        1,
                        3,
                        5,
                        4,
                        List.of(BrewFeelingTag.BRIGHT, BrewFeelingTag.SILKY, BrewFeelingTag.LONG_AFTERTASTE),
                        List.of("화사함"),
                        "자스민과 복숭아 향이 선명했고 질감이 부드러웠다."
                )
        ));
    }
}
