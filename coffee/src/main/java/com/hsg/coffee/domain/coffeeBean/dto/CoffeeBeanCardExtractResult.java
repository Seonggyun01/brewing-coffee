package com.hsg.coffee.domain.coffeeBean.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CoffeeBeanCardExtractResult {

    private String rawText;
    private CoffeeBeanCreateForm form;
    private List<String> warnings;
}
