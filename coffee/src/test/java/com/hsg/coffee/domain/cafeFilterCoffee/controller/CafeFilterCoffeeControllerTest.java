package com.hsg.coffee.domain.cafeFilterCoffee.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.entity.BrewTemperatureType;
import com.hsg.coffee.domain.brewRecord.repository.BrewRecordRepository;
import com.hsg.coffee.domain.cafeFilterCoffee.service.CafeFilterCoffeeService;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class CafeFilterCoffeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void listAndCreateFormPages() throws Exception {
        mockMvc.perform(get("/cafe-filter-coffees"))
                .andExpect(status().isOk())
                .andExpect(view().name("cafe-filter-coffees/list"))
                .andExpect(model().attributeExists("cafeFilterCoffees", "filterCafes"));

        mockMvc.perform(get("/cafe-filter-coffees/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("cafe-filter-coffees/form"))
                .andExpect(model().attributeExists("cafeFilterCoffeeForm", "temperatureTypes"));
    }

    @Test
    void createDetailUpdateAndDeleteKeepTheirRedirectContracts() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/cafe-filter-coffees")
                        .param("name", "카페 테스트 원두")
                        .param("cafeName", "테스트 카페")
                        .param("temperatureType", BrewTemperatureType.HOT.name()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String detailUrl = createResult.getResponse().getRedirectedUrl();
        Long id = Long.valueOf(detailUrl.substring(detailUrl.lastIndexOf('/') + 1));

        mockMvc.perform(get(detailUrl))
                .andExpect(status().isOk())
                .andExpect(view().name("cafe-filter-coffees/detail"));
        mockMvc.perform(get("/cafe-filter-coffees/{id}/edit", id))
                .andExpect(status().isOk())
                .andExpect(view().name("cafe-filter-coffees/form"));

        mockMvc.perform(post("/cafe-filter-coffees/{id}/edit", id)
                        .param("name", "수정한 카페 원두")
                        .param("cafeName", "테스트 카페")
                        .param("temperatureType", BrewTemperatureType.ICE.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(detailUrl));
        assertEquals("수정한 카페 원두", cafeFilterCoffeeService.get(id).getCoffeeBeanName());

        mockMvc.perform(post("/cafe-filter-coffees/{id}/delete", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cafe-filter-coffees"));
        assertEquals(0, brewRecordRepository.count());
    }

    @Test
    void invalidCardExtractionReturnsTheFormWithAWarning() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "coffee-card.txt",
                "text/plain",
                "not image".getBytes()
        );

        mockMvc.perform(multipart("/cafe-filter-coffees/card-extraction").file(image))
                .andExpect(status().isOk())
                .andExpect(view().name("cafe-filter-coffees/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("자동 입력 확인이 필요해요")))
                .andExpect(model().attributeExists("cafeFilterCoffeeForm", "extractionWarnings"));
    }
}
