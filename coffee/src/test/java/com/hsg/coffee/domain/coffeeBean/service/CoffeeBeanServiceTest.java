package com.hsg.coffee.domain.coffeeBean.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanResponse;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanUpdateForm;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;

@Transactional
@SpringBootTest
class CoffeeBeanServiceTest {

    @Autowired
    private CoffeeBeanService coffeeBeanService;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @Autowired
    private PurchasePlaceRepository purchasePlaceRepository;

    @BeforeEach
    void setUp() {
        coffeeBeanRepository.deleteAll();
        purchasePlaceRepository.deleteAll();
    }

    @Test
    void createAndGet() {
        Long id = coffeeBeanService.create(createForm("에티오피아 구지", "브루잉 로스터스"));

        CoffeeBeanResponse response = coffeeBeanService.get(id);

        printCoffeeBean("등록/단건 조회", response);
        assertEquals("에티오피아 구지", response.getName());
        assertEquals("브루잉 로스터스", response.getRoastery());
        assertEquals(ProcessType.WASHED, response.getProcessType());
        assertEquals(CoffeeBeanStatus.CURRENT, response.getStatus());
        assertEquals(List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH), response.getFlavorNotes());
        assertEquals(List.of("오렌지 껍질"), response.getCustomFlavorNotes());
    }

    @Test
    void createWithPurchasePlace() {
        CoffeeBeanCreateForm form = createForm("에티오피아 구지", "브루잉 로스터스");
        form.setPurchasePlaceName("프릳츠 원서점");
        form.setPurchasePlaceType(PurchasePlaceType.ROASTERY);
        form.setPurchasePlaceAddress("서울 종로구 율곡로 83");
        form.setPurchasePlaceMemo("지도 표시를 위해 주소만 저장하는 구매처");

        Long id = coffeeBeanService.create(form);

        CoffeeBeanResponse response = coffeeBeanService.get(id);

        System.out.println("=== CoffeeBeanService 구매처 등록 테스트 결과 ===");
        System.out.println("구매처 이름: " + response.getPurchasePlaceName());
        System.out.println("구매처 유형: " + response.getPurchasePlaceType());
        System.out.println("구매처 주소: " + response.getPurchasePlaceAddress());
        System.out.println("좌표: " + response.getPurchasePlaceLatitude() + ", " + response.getPurchasePlaceLongitude());

        assertEquals("프릳츠 원서점", response.getPurchasePlaceName());
        assertEquals(PurchasePlaceType.ROASTERY, response.getPurchasePlaceType());
        assertEquals("서울 종로구 율곡로 83", response.getPurchasePlaceAddress());
        assertNull(response.getPurchasePlaceLatitude());
        assertNull(response.getPurchasePlaceLongitude());
    }

    @Test
    void createWithoutPurchasePlace() {
        CoffeeBeanCreateForm form = createForm("에티오피아 구지", "브루잉 로스터스");

        Long id = coffeeBeanService.create(form);

        CoffeeBeanResponse response = coffeeBeanService.get(id);

        System.out.println("=== CoffeeBeanService 구매처 미입력 테스트 결과 ===");
        System.out.println("원두 이름: " + response.getName());
        System.out.println("구매처 이름: " + response.getPurchasePlaceName());

        assertNull(response.getPurchasePlaceName());
    }

    @Test
    void createWithExistingPurchasePlace() {
        PurchasePlace purchasePlace = purchasePlaceRepository.save(PurchasePlace.create(
                "프릳츠 원서점",
                PurchasePlaceType.ROASTERY,
                "서울 종로구 율곡로 83",
                null,
                null,
                "기존 구매처"
        ));
        CoffeeBeanCreateForm form = createForm("에티오피아 구지", "브루잉 로스터스");
        form.setPurchasePlaceId(purchasePlace.getId());

        Long id = coffeeBeanService.create(form);

        CoffeeBeanResponse response = coffeeBeanService.get(id);

        System.out.println("=== CoffeeBeanService 기존 구매처 선택 테스트 결과 ===");
        System.out.println("선택한 구매처 ID: " + response.getPurchasePlaceId());
        System.out.println("선택한 구매처 이름: " + response.getPurchasePlaceName());

        assertEquals(purchasePlace.getId(), response.getPurchasePlaceId());
        assertEquals("프릳츠 원서점", response.getPurchasePlaceName());
    }

    @Test
    void getAllAndSearch() {
        coffeeBeanService.create(createForm("에티오피아 구지", "브루잉 로스터스"));
        coffeeBeanService.create(createForm("케냐 키린야가", "데일리 로스터스"));

        List<CoffeeBeanResponse> all = coffeeBeanService.getAll();
        List<CoffeeBeanResponse> nameSearchResult = coffeeBeanService.searchByName("구지");
        List<CoffeeBeanResponse> roasterySearchResult = coffeeBeanService.searchByRoastery("데일리");

        System.out.println("=== CoffeeBeanService 목록/검색 테스트 결과 ===");
        System.out.println("전체 개수: " + all.size());
        System.out.println("이름 검색 결과: " + nameSearchResult.getFirst().getName());
        System.out.println("로스터리 검색 결과: " + roasterySearchResult.getFirst().getRoastery());

        assertEquals(2, all.size());
        assertEquals("에티오피아 구지", nameSearchResult.getFirst().getName());
        assertEquals("데일리 로스터스", roasterySearchResult.getFirst().getRoastery());
    }

    @Test
    void update() {
        Long id = coffeeBeanService.create(createForm("에티오피아 구지", "브루잉 로스터스"));

        CoffeeBeanUpdateForm updateForm = coffeeBeanService.getUpdateForm(id);
        updateForm.setName("에티오피아 구지 내추럴");
        updateForm.setProcessType(ProcessType.NATURAL);
        updateForm.setStatus(CoffeeBeanStatus.FINISHED);
        updateForm.setPrice(21000);
        coffeeBeanService.update(id, updateForm);

        CoffeeBeanResponse response = coffeeBeanService.get(id);

        printCoffeeBean("수정", response);
        assertEquals("에티오피아 구지 내추럴", response.getName());
        assertEquals(ProcessType.NATURAL, response.getProcessType());
        assertEquals(CoffeeBeanStatus.FINISHED, response.getStatus());
        assertEquals(21000, response.getPrice());
    }

    @Test
    void delete() {
        Long id = coffeeBeanService.create(createForm("에티오피아 구지", "브루잉 로스터스"));

        coffeeBeanService.delete(id);

        System.out.println("=== CoffeeBeanService 삭제 테스트 결과 ===");
        System.out.println("삭제 ID: " + id);
        System.out.println("존재 여부: " + coffeeBeanService.exists(id));

        assertFalse(coffeeBeanService.exists(id));
    }

    @Test
    void getNotFound() {
        Long notFoundId = 999L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> coffeeBeanService.get(notFoundId)
        );

        System.out.println("=== CoffeeBeanService 예외 테스트 결과 ===");
        System.out.println("예외 메시지: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("원두를 찾을 수 없습니다."));
    }

    private CoffeeBeanCreateForm createForm(String name, String roastery) {
        CoffeeBeanCreateForm form = new CoffeeBeanCreateForm();
        form.setName(name);
        form.setRoastery(roastery);
        form.setCountry("Ethiopia");
        form.setRegion("Guji");
        form.setFarm("Hambela");
        form.setVariety("Heirloom");
        form.setAltitude("1900-2100m");
        form.setProcessType(ProcessType.WASHED);
        form.setFlavorNotes(List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH));
        form.setCustomFlavorNotesText("오렌지 껍질");
        form.setMemo("서비스 테스트 기록");
        form.setRoastedDate(LocalDate.of(2026, 5, 1));
        form.setPurchasedDate(LocalDate.of(2026, 5, 2));
        form.setPrice(18000);
        form.setWeight(200);
        return form;
    }

    private void printCoffeeBean(String title, CoffeeBeanResponse response) {
        System.out.println("=== CoffeeBeanService " + title + " 테스트 결과 ===");
        System.out.println("ID: " + response.getId());
        System.out.println("원두 이름: " + response.getName());
        System.out.println("로스터리: " + response.getRoastery());
        System.out.println("가공 방식: " + response.getProcessType());
        System.out.println("가격: " + response.getPrice());
        System.out.println("용량: " + response.getWeight());
    }
}
