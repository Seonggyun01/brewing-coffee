package com.hsg.coffee.domain.brewRecord.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.global.entity.BaseTimeEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "brew_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrewRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coffee_bean_id", nullable = false)
    private CoffeeBean coffeeBean;

    private LocalDate brewedDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BrewMethod brewMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BrewTemperatureType temperatureType;

    private BigDecimal beanAmount;

    private Integer inventoryDeductedWeight;

    private BigDecimal waterAmount;

    private BigDecimal waterTemperature;

    private Integer grindSizeMicron;

    private Integer brewTimeSec;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "brew_record_pour_steps", joinColumns = @JoinColumn(name = "brew_record_id"))
    @OrderColumn(name = "sort_order")
    private List<BrewPourStep> pourSteps = new ArrayList<>();

    private Integer rating;

    private Integer acidity;

    private Integer sweetness;

    private Integer bitterness;

    private Integer body;

    private Integer aroma;

    private Integer balance;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "brew_record_feeling_tags", joinColumns = @JoinColumn(name = "brew_record_id"))
    @OrderColumn(name = "sort_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "feeling_tag", length = 60)
    private List<BrewFeelingTag> feelingTags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "brew_record_custom_feeling_tags", joinColumns = @JoinColumn(name = "brew_record_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "custom_feeling_tag", length = 60)
    private List<String> customFeelingTags = new ArrayList<>();

    @Column(length = 1000)
    private String memo;

    public static BrewRecord create(
            CoffeeBean coffeeBean,
            LocalDate brewedDate,
            BrewMethod brewMethod,
            BrewTemperatureType temperatureType,
            BigDecimal beanAmount,
            BigDecimal waterAmount,
            BigDecimal waterTemperature,
            Integer grindSizeMicron,
            Integer brewTimeSec,
            List<BrewPourStep> pourSteps,
            Integer rating,
            Integer acidity,
            Integer sweetness,
            Integer bitterness,
            Integer body,
            Integer aroma,
            Integer balance,
            List<BrewFeelingTag> feelingTags,
            List<String> customFeelingTags,
            String memo
    ) {
        BrewRecord brewRecord = new BrewRecord();
        brewRecord.coffeeBean = coffeeBean;
        brewRecord.update(
                brewedDate,
                brewMethod,
                temperatureType,
                beanAmount,
                waterAmount,
                waterTemperature,
                grindSizeMicron,
                brewTimeSec,
                pourSteps,
                rating,
                acidity,
                sweetness,
                bitterness,
                body,
                aroma,
                balance,
                feelingTags,
                customFeelingTags,
                memo
        );
        return brewRecord;
    }

    public void updateCoffeeBean(CoffeeBean coffeeBean) {
        this.coffeeBean = coffeeBean;
    }

    public void updateInventoryDeductedWeight(Integer inventoryDeductedWeight) {
        this.inventoryDeductedWeight = inventoryDeductedWeight;
    }

    public void update(
            LocalDate brewedDate,
            BrewMethod brewMethod,
            BrewTemperatureType temperatureType,
            BigDecimal beanAmount,
            BigDecimal waterAmount,
            BigDecimal waterTemperature,
            Integer grindSizeMicron,
            Integer brewTimeSec,
            List<BrewPourStep> pourSteps,
            Integer rating,
            Integer acidity,
            Integer sweetness,
            Integer bitterness,
            Integer body,
            Integer aroma,
            Integer balance,
            List<BrewFeelingTag> feelingTags,
            List<String> customFeelingTags,
            String memo
    ) {
        this.brewedDate = brewedDate;
        this.brewMethod = brewMethod;
        this.temperatureType = temperatureType;
        this.beanAmount = beanAmount;
        this.waterAmount = waterAmount;
        this.waterTemperature = waterTemperature;
        this.grindSizeMicron = grindSizeMicron;
        this.brewTimeSec = brewTimeSec;
        this.pourSteps.clear();
        if (pourSteps != null) {
            this.pourSteps.addAll(pourSteps);
        }
        this.rating = rating;
        this.acidity = acidity;
        this.sweetness = sweetness;
        this.bitterness = bitterness;
        this.body = body;
        this.aroma = aroma;
        this.balance = balance;
        this.feelingTags.clear();
        if (feelingTags != null) {
            this.feelingTags.addAll(feelingTags);
        }
        this.customFeelingTags.clear();
        if (customFeelingTags != null) {
            this.customFeelingTags.addAll(customFeelingTags);
        }
        this.memo = memo;
    }
}
