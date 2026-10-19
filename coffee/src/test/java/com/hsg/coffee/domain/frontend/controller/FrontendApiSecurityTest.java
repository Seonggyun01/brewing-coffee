package com.hsg.coffee.domain.frontend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"brewlog.security.enabled=true", "server.forward-headers-strategy=framework"})
@AutoConfigureMockMvc
class FrontendApiSecurityTest {
    @Autowired MockMvc mvc;
    @Test void anonymousApiReturns401InsteadOfLoginHtml() throws Exception {
        mvc.perform(get("/api/front/beans")).andExpect(status().isUnauthorized());
    }
    @Test void sessionProvidesCsrfWithoutRequiringLogin() throws Exception {
        mvc.perform(get("/api/front/session")).andExpect(status().isOk()).andExpect(jsonPath("$.csrf.token").isNotEmpty());
    }
    @Test void oauthUsesFrontendOriginForCallback() throws Exception {
        mvc.perform(get("/oauth2/authorization/kakao").header("X-Forwarded-Host", "localhost:3000").header("X-Forwarded-Proto", "http"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("redirect_uri=http://localhost:3000/login/oauth2/code/kakao")));
    }
    @Test void mutationsRequireCsrf() throws Exception {
        mvc.perform(post("/api/front/beans").contentType("application/json").content("{\"name\":\"test\"}"))
                .andExpect(status().isForbidden());
    }
}
