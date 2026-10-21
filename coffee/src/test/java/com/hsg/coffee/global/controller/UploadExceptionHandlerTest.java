package com.hsg.coffee.global.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class UploadExceptionHandlerTest {
    @RestController
    static class UploadProbe {
        @GetMapping("/oversized")
        void upload() { throw new MaxUploadSizeExceededException(20 * 1024 * 1024); }
    }

    @Test void returnsActionableJsonWith413() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new UploadProbe())
                .setControllerAdvice(new UploadExceptionHandler()).build();
        mvc.perform(get("/oversized"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("사진 용량이 너무 큽니다. 20MB 이하 사진을 선택해주세요."));
    }
}
