package com.hsg.coffee.domain.coffeeBean.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanService;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class CoffeeBeanControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void listPage() throws Exception {
        mockMvc.perform(get("/coffee-beans"))
                .andExpect(status().isOk())
                .andExpect(view().name("coffee-beans/list"))
                .andExpect(model().attributeExists("coffeeBeans"));
    }

    @Test
    void createFormContainsPurchasePlaces() throws Exception {
        purchasePlaceRepository.save(PurchasePlace.create(
                "프릳츠 원서점",
                PurchasePlaceType.ROASTERY,
                "서울 종로구 율곡로 83",
                null,
                null,
                "기존 구매처"
        ));

        mockMvc.perform(get("/coffee-beans/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("coffee-beans/form"))
                .andExpect(model().attributeExists("coffeeBeanStatuses", "purchasePlaces"));
    }

    @Test
    void createAndDetailPage() throws Exception {
        MvcResult result = mockMvc.perform(post("/coffee-beans")
                        .param("name", "에티오피아 구지")
                        .param("roastery", "브루잉 로스터스")
                        .param("country", "Ethiopia")
                        .param("region", "Guji")
                        .param("farm", "Hambela")
                        .param("variety", "Heirloom")
                        .param("altitude", "1900-2100m")
                        .param("processType", ProcessType.WASHED.name())
                        .param("flavorNotes", FlavorNote.JASMINE.name(), FlavorNote.LEMON.name(), FlavorNote.PEACH.name())
                        .param("customFlavorNotesText", "오렌지 껍질")
                        .param("memo", "컨트롤러 등록 테스트")
                        .param("roastedDate", "2026-05-01")
                        .param("purchasedDate", "2026-05-02")
                        .param("price", "18000")
                        .param("weight", "200")
                        .param("purchasePlaceName", "프릳츠 원서점")
                        .param("purchasePlaceType", PurchasePlaceType.ROASTERY.name())
                        .param("purchasePlaceAddress", "서울 종로구 율곡로 83")
                        .param("purchasePlaceMemo", "컨트롤러 구매처 등록 테스트"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String detailUrl = result.getResponse().getRedirectedUrl();

        System.out.println("=== CoffeeBeanController 등록 테스트 결과 ===");
        System.out.println("상세 redirect URL: " + detailUrl);

        mockMvc.perform(get(detailUrl))
                .andExpect(status().isOk())
                .andExpect(view().name("coffee-beans/detail"))
                .andExpect(model().attributeExists("coffeeBean"));

        Long id = Long.valueOf(detailUrl.substring(detailUrl.lastIndexOf("/") + 1));
        assertTrue("프릳츠 원서점".equals(coffeeBeanService.get(id).getPurchasePlaceName()));
    }

    @Test
    void createWithExistingPurchasePlace() throws Exception {
        PurchasePlace purchasePlace = purchasePlaceRepository.save(PurchasePlace.create(
                "프릳츠 원서점",
                PurchasePlaceType.ROASTERY,
                "서울 종로구 율곡로 83",
                null,
                null,
                "기존 구매처"
        ));

        MvcResult result = mockMvc.perform(post("/coffee-beans")
                        .param("name", "에티오피아 구지")
                        .param("roastery", "브루잉 로스터스")
                        .param("processType", ProcessType.WASHED.name())
                        .param("price", "18000")
                        .param("weight", "200")
                        .param("purchasePlaceId", String.valueOf(purchasePlace.getId())))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String detailUrl = result.getResponse().getRedirectedUrl();
        Long id = Long.valueOf(detailUrl.substring(detailUrl.lastIndexOf("/") + 1));

        assertTrue("프릳츠 원서점".equals(coffeeBeanService.get(id).getPurchasePlaceName()));
    }

    @Test
    void update() throws Exception {
        Long id = coffeeBeanService.create(createForm());

        mockMvc.perform(post("/coffee-beans/{id}/edit", id)
                        .param("name", "에티오피아 구지 내추럴")
                        .param("roastery", "브루잉 로스터스")
                        .param("country", "Ethiopia")
                        .param("region", "Guji")
                        .param("farm", "Hambela")
                        .param("variety", "Heirloom")
                        .param("altitude", "1900-2100m")
                        .param("processType", ProcessType.NATURAL.name())
                        .param("flavorNotes", FlavorNote.BLUEBERRY.name(), FlavorNote.JASMINE.name())
                        .param("customFlavorNotesText", "베리잼")
                        .param("memo", "컨트롤러 수정 테스트")
                        .param("roastedDate", "2026-05-01")
                        .param("purchasedDate", "2026-05-02")
                        .param("price", "21000")
                        .param("weight", "200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/coffee-beans/" + id));

        System.out.println("=== CoffeeBeanController 수정 테스트 결과 ===");
        System.out.println("수정된 원두 이름: " + coffeeBeanService.get(id).getName());

        assertTrue(coffeeBeanService.get(id).getName().contains("내추럴"));
    }

    @Test
    void delete() throws Exception {
        Long id = coffeeBeanService.create(createForm());

        mockMvc.perform(post("/coffee-beans/{id}/delete", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/coffee-beans"));

        System.out.println("=== CoffeeBeanController 삭제 테스트 결과 ===");
        System.out.println("삭제 ID: " + id);
        System.out.println("존재 여부: " + coffeeBeanService.exists(id));

        assertFalse(coffeeBeanService.exists(id));
    }

    private CoffeeBeanCreateForm createForm() {
        CoffeeBeanCreateForm form = new CoffeeBeanCreateForm();
        form.setName("에티오피아 구지");
        form.setRoastery("브루잉 로스터스");
        form.setCountry("Ethiopia");
        form.setRegion("Guji");
        form.setFarm("Hambela");
        form.setVariety("Heirloom");
        form.setAltitude("1900-2100m");
        form.setProcessType(ProcessType.WASHED);
        form.setFlavorNotes(java.util.List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH));
        form.setCustomFlavorNotesText("오렌지 껍질");
        form.setMemo("컨트롤러 테스트 기록");
        form.setPrice(18000);
        form.setWeight(200);
        return form;
    }
}
