package com.hsg.coffee.domain.tasteSpace.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TasteSpaceController {

    @GetMapping("/taste-space")
    public String page() {
        return "redirect:/coffee-beans#taste-space";
    }
}
