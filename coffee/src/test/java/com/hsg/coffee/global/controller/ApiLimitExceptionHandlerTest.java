package com.hsg.coffee.global.controller;

import com.hsg.coffee.domain.apiusage.service.ApiLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiLimitExceptionHandlerTest {
    @RestController
    static class Probe {
        @GetMapping("/quota") void quota() { throw ApiLimitException.exhausted("Groq"); }
        @GetMapping("/unavailable") void unavailable() { throw ApiLimitException.unavailable(); }
    }

    @Test void returnsQuotaMessageAndPersistenceFailureAsJson() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new Probe())
                .setControllerAdvice(new ApiLimitExceptionHandler()).build();
        mvc.perform(get("/quota")).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Groq")));
        mvc.perform(get("/unavailable")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("관리자")));
    }
}
