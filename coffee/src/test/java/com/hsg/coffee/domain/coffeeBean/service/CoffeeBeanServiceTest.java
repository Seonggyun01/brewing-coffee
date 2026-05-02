package com.hsg.coffee.domain.coffeeBean.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;

@Transactional
@SpringBootTest
class CoffeeBeanServiceTest {

    @Autowired
    private CoffeeBeanService coffeeBeanService;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @BeforeEach
    void setUp() {
        coffeeBeanRepository.deleteAll();
    }

    @Test
    void createAndGet() {
        Long id = coffeeBeanService.create(createForm("에티오피아 구지", "브루잉 로스터스"));

        CoffeeBeanResponse response = coffeeBeanService.get(id);

        printCoffeeBean("등록/단건 조회", response);
        assertEquals("에티오피아 구지", response.getName());
        assertEquals("브루잉 로스터스", response.getRoastery());
        assertEquals(ProcessType.WASHED, response.getProcessType());
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
        updateForm.setPrice(21000);
        coffeeBeanService.update(id, updateForm);

        CoffeeBeanResponse response = coffeeBeanService.get(id);

        printCoffeeBean("수정", response);
        assertEquals("에티오피아 구지 내추럴", response.getName());
        assertEquals(ProcessType.NATURAL, response.getProcessType());
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
        form.setFlavorNotes("floral, citrus, tea-like");
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
