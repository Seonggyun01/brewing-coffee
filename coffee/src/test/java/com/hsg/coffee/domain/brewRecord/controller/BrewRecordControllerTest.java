package com.hsg.coffee.domain.brewRecord.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.entity.BrewFeelingTag;
import com.hsg.coffee.domain.brewRecord.entity.BrewMethod;
import com.hsg.coffee.domain.brewRecord.entity.BrewTemperatureType;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.brewRecord.repository.BrewRecordRepository;
import com.hsg.coffee.domain.brewRecord.service.BrewRecordService;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class BrewRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrewRecordService brewRecordService;

    @Autowired
    private BrewRecordRepository brewRecordRepository;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    private Long coffeeBeanId;

    @BeforeEach
    void setUp() {
        brewRecordRepository.deleteAll();
        coffeeBeanRepository.deleteAll();

        CoffeeBean coffeeBean = coffeeBeanRepository.save(CoffeeBean.create(
                "에티오피아 구지",
                "브루잉 로스터스",
                "Ethiopia",
                "Guji",
                "Hambela",
                "Heirloom",
                "1900-2100m",
                ProcessType.WASHED,
                java.util.List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH),
                java.util.List.of("청포도", "오렌지 껍질"),
                "컨트롤러 테스트용 원두",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                18000,
                200
        ));
        coffeeBeanId = coffeeBean.getId();
    }

    @Test
    void listPage() throws Exception {
        mockMvc.perform(get("/brew-records"))
                .andExpect(status().isOk())
                .andExpect(view().name("brew-records/list"))
                .andExpect(model().attributeExists("brewRecords"));
    }

    @Test
    void createForm() throws Exception {
        mockMvc.perform(get("/brew-records/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("brew-records/form"))
                .andExpect(model().attributeExists("coffeeBeans", "temperatureTypes", "feelingTags"));
    }

    @Test
    void createAndDetailPage() throws Exception {
        MvcResult result = mockMvc.perform(post("/brew-records")
                        .param("coffeeBeanId", String.valueOf(coffeeBeanId))
                        .param("brewedDate", "2026-05-03")
                        .param("brewMethod", BrewMethod.V60.name())
                        .param("temperatureType", BrewTemperatureType.HOT.name())
                        .param("beanAmount", "15")
                        .param("waterAmount", "240")
                        .param("waterTemperature", "92")
                        .param("grindSizeMicron", "720")
                        .param("brewTimeSec", "150")
                        .param("pourSteps[0].timeSec", "0")
                        .param("pourSteps[0].amountMl", "40")
                        .param("pourSteps[1].timeSec", "40")
                        .param("pourSteps[1].amountMl", "80")
                        .param("pourSteps[2].timeSec", "85")
                        .param("pourSteps[2].amountMl", "80")
                        .param("rating", "4")
                        .param("acidity", "4")
                        .param("sweetness", "4")
                        .param("bitterness", "2")
                        .param("body", "3")
                        .param("aroma", "5")
                        .param("balance", "4")
                        .param("feelingTags", BrewFeelingTag.CLEAN.name(), BrewFeelingTag.BRIGHT.name())
                        .param("customFeelingTagsText", "맑은 여운")
                        .param("memo", "컨트롤러 등록 테스트"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String detailUrl = result.getResponse().getRedirectedUrl();

        mockMvc.perform(get(detailUrl))
                .andExpect(status().isOk())
                .andExpect(view().name("brew-records/detail"))
                .andExpect(model().attributeExists("brewRecord"));

        Long id = Long.valueOf(detailUrl.substring(detailUrl.lastIndexOf("/") + 1));
        assertEquals("자스민, 레몬, 복숭아, 청포도, 오렌지 껍질", brewRecordService.get(id).getFlavorNoteSummary());
    }

    @Test
    void update() throws Exception {
        MvcResult result = mockMvc.perform(post("/brew-records")
                        .param("coffeeBeanId", String.valueOf(coffeeBeanId))
                        .param("brewedDate", "2026-05-03")
                        .param("brewMethod", BrewMethod.V60.name())
                        .param("temperatureType", BrewTemperatureType.HOT.name())
                        .param("rating", "4"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Long id = Long.valueOf(result.getResponse().getRedirectedUrl().replace("/brew-records/", ""));

        mockMvc.perform(post("/brew-records/{id}/edit", id)
                        .param("coffeeBeanId", String.valueOf(coffeeBeanId))
                        .param("brewedDate", "2026-05-04")
                        .param("brewMethod", BrewMethod.ORIGAMI.name())
                        .param("temperatureType", BrewTemperatureType.ICE.name())
                        .param("rating", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/brew-records/" + id));

        assertEquals(5, brewRecordService.get(id).getRating());
        assertEquals(BrewTemperatureType.ICE, brewRecordService.get(id).getTemperatureType());
        assertEquals(java.util.List.of("청포도", "오렌지 껍질"), brewRecordService.get(id).getCustomFlavorNotes());
    }
}
