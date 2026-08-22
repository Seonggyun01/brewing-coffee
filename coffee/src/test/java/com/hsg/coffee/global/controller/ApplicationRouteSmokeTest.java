package com.hsg.coffee.global.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationRouteSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allTopLevelPagesRenderSuccessfully() throws Exception {
        List<String> paths = List.of(
                "/",
                "/coffee-beans",
                "/coffee-beans/new",
                "/brew-records",
                "/brew-records/new",
                "/cafe-filter-coffees",
                "/cafe-filter-coffees/new",
                "/cafes/map"
        );

        for (String path : paths) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        }
    }

    @Test
    void allJsonApisRespondSuccessfully() throws Exception {
        List<String> paths = List.of(
                "/api/maps/cafes",
                "/api/origin-map/countries"
        );

        for (String path : paths) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }
}
