package com.hsg.coffee.domain.dashboard.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hsg.coffee.domain.brewRecord.dto.BrewRecordResponse;
import com.hsg.coffee.domain.brewRecord.service.BrewRecordService;
import com.hsg.coffee.domain.cafeFilterCoffee.service.CafeFilterCoffeeService;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanResponse;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CoffeeBeanService coffeeBeanService;
    private final BrewRecordService brewRecordService;
    private final CafeFilterCoffeeService cafeFilterCoffeeService;

    @GetMapping("/")
    public String home(Model model) {
        List<CoffeeBeanResponse> currentBeans = coffeeBeanService.getAll().stream()
                .filter(coffeeBean -> coffeeBean.getStatus() == CoffeeBeanStatus.CURRENT)
                .limit(4)
                .toList();
        List<BrewRecordResponse> recentBrews = brewRecordService.getAll().stream()
                .limit(3)
                .toList();
        List<BrewRecordResponse> recentCafeFilters = cafeFilterCoffeeService.getAll().stream()
                .limit(3)
                .toList();

        model.addAttribute("currentBeans", currentBeans);
        model.addAttribute("recentBrews", recentBrews);
        model.addAttribute("recentCafeFilters", recentCafeFilters);
        return "index";
    }
}
