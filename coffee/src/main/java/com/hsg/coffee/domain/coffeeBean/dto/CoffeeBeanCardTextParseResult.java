package com.hsg.coffee.domain.coffeeBean.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CoffeeBeanCardTextParseResult {

    private String name;
    private String roastery;
    private String country;
    private String originCountryCode;
    private String region;
    private String farm;
    private String variety;
    private ProcessType processType;
    private List<FlavorNote> flavorNotes = new ArrayList<>();
    private String customFlavorNotesText;
    private Integer weight;
    private Integer price;
    private LocalDate roastedDate;
    private List<String> warnings = new ArrayList<>();
}
