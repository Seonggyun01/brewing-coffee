package com.hsg.coffee.domain.cafeFilterCoffee.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.dto.BrewRecordForm;
import com.hsg.coffee.domain.brewRecord.dto.BrewRecordResponse;
import com.hsg.coffee.domain.brewRecord.service.BrewRecordService;
import com.hsg.coffee.domain.cafeFilterCoffee.dto.CafeFilterCoffeeForm;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanService;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CafeFilterCoffeeService {

    private final CoffeeBeanService coffeeBeanService;
    private final BrewRecordService brewRecordService;

    public List<BrewRecordResponse> getAll() {
        return brewRecordService.getCafeRecords();
    }

    public BrewRecordResponse get(Long id) {
        return brewRecordService.getCafeRecord(id);
    }

    @Transactional
    public Long create(CafeFilterCoffeeForm form) {
        Long coffeeBeanId = coffeeBeanService.create(toCoffeeBeanForm(form));
        return brewRecordService.create(toBrewRecordForm(form, coffeeBeanId));
    }

    @Transactional
    public void delete(Long id) {
        brewRecordService.delete(id);
    }

    private CoffeeBeanCreateForm toCoffeeBeanForm(CafeFilterCoffeeForm form) {
        CoffeeBeanCreateForm coffeeBeanForm = new CoffeeBeanCreateForm();
        coffeeBeanForm.setName(form.getName());
        coffeeBeanForm.setRoastery(form.getRoastery());
        coffeeBeanForm.setOriginCountryCode(form.getOriginCountryCode());
        coffeeBeanForm.setRegion(form.getRegion());
        coffeeBeanForm.setVariety(form.getVariety());
        coffeeBeanForm.setProcessType(form.getProcessType());
        coffeeBeanForm.setFlavorNotes(form.getFlavorNotes());
        coffeeBeanForm.setCustomFlavorNotesText(form.getCustomFlavorNotesText());
        coffeeBeanForm.setMemo(form.getMemo());
        coffeeBeanForm.setPurchasedDate(form.getVisitedDate());
        coffeeBeanForm.setStatus(CoffeeBeanStatus.CAFE);
        coffeeBeanForm.setPurchasePlaceName(form.getCafeName());
        coffeeBeanForm.setPurchasePlaceType(PurchasePlaceType.CAFE);
        coffeeBeanForm.setPurchasePlaceAddress(form.getCafeAddress());
        coffeeBeanForm.setPurchasePlaceUrl(form.getCafeUrl());
        coffeeBeanForm.setPurchasePlaceLatitude(form.getCafeLatitude());
        coffeeBeanForm.setPurchasePlaceLongitude(form.getCafeLongitude());
        return coffeeBeanForm;
    }

    private BrewRecordForm toBrewRecordForm(CafeFilterCoffeeForm form, Long coffeeBeanId) {
        BrewRecordForm brewRecordForm = new BrewRecordForm();
        brewRecordForm.setCoffeeBeanId(coffeeBeanId);
        brewRecordForm.setBrewedDate(form.getVisitedDate());
        brewRecordForm.setTemperatureType(form.getTemperatureType());
        brewRecordForm.setBrewMethod(form.isRecordRecipe() ? form.getBrewMethod() : null);
        brewRecordForm.setBeanAmount(form.isRecordRecipe() ? form.getBeanAmount() : null);
        brewRecordForm.setWaterAmount(form.isRecordRecipe() ? form.getWaterAmount() : null);
        brewRecordForm.setWaterTemperature(form.isRecordRecipe() ? form.getWaterTemperature() : null);
        brewRecordForm.setGrindSizeMicron(form.isRecordRecipe() ? form.getGrindSizeMicron() : null);
        brewRecordForm.setBrewTimeSec(form.isRecordRecipe() ? form.getBrewTimeSec() : null);
        brewRecordForm.setPourSteps(form.isRecordRecipe() ? form.getPourSteps() : List.of());
        brewRecordForm.setRating(form.getRating());
        brewRecordForm.setFeelingTags(form.getFeelingTags());
        brewRecordForm.setCustomFeelingTagsText(form.getCustomFeelingTagsText());
        brewRecordForm.setMemo(form.getMemo());
        return brewRecordForm;
    }
}
