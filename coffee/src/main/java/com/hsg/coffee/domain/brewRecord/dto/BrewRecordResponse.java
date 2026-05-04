package com.hsg.coffee.domain.brewRecord.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.hsg.coffee.domain.brewRecord.entity.BrewFeelingTag;
import com.hsg.coffee.domain.brewRecord.entity.BrewMethod;
import com.hsg.coffee.domain.brewRecord.entity.BrewRecord;
import com.hsg.coffee.domain.brewRecord.entity.BrewTemperatureType;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;

import lombok.Getter;

@Getter
public class BrewRecordResponse {

    private final Long id;
    private final Long coffeeBeanId;
    private final String coffeeBeanName;
    private final String roastery;
    private final LocalDate brewedDate;
    private final BrewMethod brewMethod;
    private final BrewTemperatureType temperatureType;
    private final BigDecimal beanAmount;
    private final BigDecimal waterAmount;
    private final BigDecimal waterTemperature;
    private final Integer grindSizeMicron;
    private final Integer brewTimeSec;
    private final List<BrewPourStepForm> pourSteps;
    private final Integer rating;
    private final Integer acidity;
    private final Integer sweetness;
    private final Integer bitterness;
    private final Integer body;
    private final Integer aroma;
    private final Integer balance;
    private final List<FlavorNote> flavorNotes;
    private final List<BrewFeelingTag> feelingTags;
    private final List<String> customFlavorNotes;
    private final List<String> customFeelingTags;
    private final String memo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private BrewRecordResponse(BrewRecord brewRecord) {
        this.id = brewRecord.getId();
        this.coffeeBeanId = brewRecord.getCoffeeBean().getId();
        this.coffeeBeanName = brewRecord.getCoffeeBean().getName();
        this.roastery = brewRecord.getCoffeeBean().getRoastery();
        this.brewedDate = brewRecord.getBrewedDate();
        this.brewMethod = brewRecord.getBrewMethod();
        this.temperatureType = brewRecord.getTemperatureType();
        this.beanAmount = brewRecord.getBeanAmount();
        this.waterAmount = brewRecord.getWaterAmount();
        this.waterTemperature = brewRecord.getWaterTemperature();
        this.grindSizeMicron = brewRecord.getGrindSizeMicron();
        this.brewTimeSec = brewRecord.getBrewTimeSec();
        this.pourSteps = brewRecord.getPourSteps().stream()
                .map(BrewPourStepForm::from)
                .toList();
        this.rating = brewRecord.getRating();
        this.acidity = brewRecord.getAcidity();
        this.sweetness = brewRecord.getSweetness();
        this.bitterness = brewRecord.getBitterness();
        this.body = brewRecord.getBody();
        this.aroma = brewRecord.getAroma();
        this.balance = brewRecord.getBalance();
        this.flavorNotes = List.copyOf(brewRecord.getCoffeeBean().getFlavorNotes());
        this.feelingTags = List.copyOf(brewRecord.getFeelingTags());
        this.customFlavorNotes = List.copyOf(brewRecord.getCoffeeBean().getCustomFlavorNotes());
        this.customFeelingTags = List.copyOf(brewRecord.getCustomFeelingTags());
        this.memo = brewRecord.getMemo();
        this.createdAt = brewRecord.getCreatedAt();
        this.updatedAt = brewRecord.getUpdatedAt();
    }

    public static BrewRecordResponse from(BrewRecord brewRecord) {
        return new BrewRecordResponse(brewRecord);
    }

    public String getFlavorGradientStyle() {
        if (flavorNotes.isEmpty()) {
            return "background: #ded4c8;";
        }

        String colors = flavorNotes.stream()
                .map(FlavorNote::getColor)
                .collect(Collectors.joining(", "));
        return "background: linear-gradient(90deg, " + colors + ");";
    }

    public String getFlavorNoteSummary() {
        if (flavorNotes.isEmpty() && customFlavorNotes.isEmpty()) {
            return "-";
        }

        String selectedNotes = flavorNotes.stream()
                .map(FlavorNote::getDisplayName)
                .collect(Collectors.joining(", "));
        String customNotes = String.join(", ", customFlavorNotes);

        if (selectedNotes.isBlank()) {
            return customNotes;
        }
        if (customNotes.isBlank()) {
            return selectedNotes;
        }
        return selectedNotes + ", " + customNotes;
    }

    public String getTasteChartValues() {
        return Arrays.asList(acidity, sweetness, bitterness, body, aroma, balance).stream()
                .map(score -> score != null ? String.valueOf(score) : "0")
                .collect(Collectors.joining(","));
    }

    public String getGrindSizeDisplay() {
        if (grindSizeMicron == null) {
            return "-";
        }
        return grindSizeMicron + " μm";
    }

    public Integer getPourTimelineDurationSec() {
        return brewTimeSec != null && brewTimeSec > 0 ? brewTimeSec : 180;
    }
}
