package com.hsg.coffee.domain.cafeMap.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CafeMapController {

    @Value("${brewlog.kakao.javascript-key:}")
    private String kakaoJavascriptKey;

    @GetMapping("/cafes/map")
    public String cafeMap(Model model) {
        model.addAttribute("kakaoJavascriptKey", kakaoJavascriptKey);
        model.addAttribute("hasKakaoMapKey", StringUtils.hasText(kakaoJavascriptKey));
        return "maps/cafe-map";
    }
}
