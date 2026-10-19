package com.hsg.coffee.domain.frontend.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.hsg.coffee.domain.coffeeBean.dto.*;
import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanCardExtractionService;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendExtractionApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean CoffeeBeanCardExtractionService extraction;
    @Test void extractionReturnsCandidatesWithoutSavingOrAutoFillingPurchaseDate() throws Exception {
        var form = new CoffeeBeanCreateForm(); form.setName("사진 원두"); form.setFarm("ZARZA"); form.setPurchasedDate(LocalDate.of(2026, 1, 1));
        when(extraction.extract(any(), any())).thenReturn(new CoffeeBeanCardExtractResult("카드 텍스트", form, List.of()));
        mvc.perform(multipart("/api/front/extraction").file(new MockMultipartFile("image", "bean.jpg", "image/jpeg", new byte[]{1})).param("extractionProgressId", "frontend-extract-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.form.name").value("사진 원두"))
                .andExpect(jsonPath("$.form.farm").value("ZARZA")).andExpect(jsonPath("$.form.purchasedDate").doesNotExist());
        verify(extraction).extract(any(), any());
    }
    @Test void failedExtractionReturns422ForManualEntryFallback() throws Exception {
        when(extraction.extract(any(), any())).thenThrow(new IllegalArgumentException("사진을 읽을 수 없습니다."));
        mvc.perform(multipart("/api/front/extraction").file(new MockMultipartFile("image", "bean.jpg", "image/jpeg", new byte[]{1})).param("extractionProgressId", "frontend-failed-test"))
                .andExpect(status().isUnprocessableEntity());
    }
}
