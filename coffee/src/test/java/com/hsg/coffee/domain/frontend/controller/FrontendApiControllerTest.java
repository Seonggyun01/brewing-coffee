package com.hsg.coffee.domain.frontend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FrontendApiControllerTest {
    @Autowired MockMvc mvc;
    long create(String path, String body) throws Exception {
        String json = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
    @Test void previewReturnsImageWithoutSaving() throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        var file = new org.springframework.mock.web.MockMultipartFile("image", "card.png", "image/png", output.toByteArray());
        mvc.perform(multipart("/api/front/image-preview").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
    @Test void previewRejectsUnsupportedFile() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile("image", "card.svg", "image/svg+xml", new byte[]{1});
        mvc.perform(multipart("/api/front/image-preview").file(file)).andExpect(status().isUnprocessableEntity());
    }
    @Test void validationReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/front/beans").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.name").exists());
    }
    @Test void beanCrudUsesExistingServices() throws Exception {
        long id = create("/api/front/beans", "{\"name\":\"SSR 원두\",\"weight\":200}");
        mvc.perform(get("/api/front/beans/" + id)).andExpect(jsonPath("$.name").value("SSR 원두"));
        mvc.perform(get("/api/front/beans/" + id + "/form")).andExpect(jsonPath("$.weight").value(200));
        mvc.perform(put("/api/front/beans/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"수정 원두\",\"weight\":180}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/front/beans/" + id)).andExpect(jsonPath("$.name").value("수정 원두"));
        mvc.perform(delete("/api/front/beans/" + id)).andExpect(status().isOk());
    }
    @Test void brewingPreservesInventoryDeductionAndRestoration() throws Exception {
        long bean = create("/api/front/beans", "{\"name\":\"재고 검증\",\"weight\":200}");
        long brew = create("/api/front/brews", "{\"coffeeBeanId\":" + bean + ",\"beanAmount\":20,\"temperatureType\":\"ICE\",\"acidity\":4}");
        mvc.perform(get("/api/front/beans/" + bean)).andExpect(jsonPath("$.weight").value(180));
        mvc.perform(get("/api/front/brews/" + brew)).andExpect(jsonPath("$.temperatureType").value("ICE")).andExpect(jsonPath("$.acidity").value(4));
        mvc.perform(put("/api/front/brews/" + brew).contentType(MediaType.APPLICATION_JSON).content("{\"coffeeBeanId\":" + bean + ",\"beanAmount\":10}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/front/beans/" + bean)).andExpect(jsonPath("$.weight").value(190));
        mvc.perform(delete("/api/front/brews/" + brew)).andExpect(status().isOk());
        mvc.perform(get("/api/front/beans/" + bean)).andExpect(jsonPath("$.weight").value(200));
    }
    @Test void cafeDoesNotStoreRecipeFromNewFrontend() throws Exception {
        long id = create("/api/front/cafes", "{\"name\":\"카페 원두\",\"cafeName\":\"테스트 카페\",\"recordRecipe\":true,\"beanAmount\":20,\"brewMethod\":\"V60\"}");
        mvc.perform(get("/api/front/cafes/" + id)).andExpect(jsonPath("$.beanAmount").doesNotExist()).andExpect(jsonPath("$.brewMethod").doesNotExist());
        mvc.perform(get("/api/front/cafes/" + id + "/form")).andExpect(jsonPath("$.cafeName").value("테스트 카페"));
    }
}
