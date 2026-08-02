package com.hsg.coffee.domain.cafeFilterCoffee.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.brewRecord.dto.BrewRecordForm;
import com.hsg.coffee.domain.brewRecord.dto.BrewRecordResponse;
import com.hsg.coffee.domain.brewRecord.service.BrewRecordService;
import com.hsg.coffee.domain.cafeFilterCoffee.dto.CafeFilterCoffeeForm;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanUpdateForm;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanService;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.service.PurchasePlaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CafeFilterCoffeeService {

    private final CoffeeBeanService coffeeBeanService;
    private final BrewRecordService brewRecordService;
    private final PurchasePlaceService purchasePlaceService;

    public List<BrewRecordResponse> getAll() {
        return brewRecordService.getCafeRecords();
    }

    public List<BrewRecordResponse> getByCafe(Long cafeId) {
        if (cafeId == null) {
            return getAll();
        }

        return brewRecordService.getCafeRecordsByPurchasePlaces(purchasePlaceService.getEquivalentCafePlaceIds(cafeId));
    }

    public List<BrewRecordResponse> getByCafeAndQuery(Long cafeId, String query) {
        List<BrewRecordResponse> records = getByCafe(cafeId);
        if (!StringUtils.hasText(query)) {
            return records;
        }

        String normalizedQuery = normalize(query);
        return records.stream()
                .filter(record -> contains(record.getCoffeeBeanName(), normalizedQuery)
                        || contains(record.getPurchasePlaceName(), normalizedQuery)
                        || contains(record.getRoastery(), normalizedQuery)
                        || contains(record.getMemo(), normalizedQuery)
                        || contains(record.getFlavorNoteSummary(), normalizedQuery))
                .toList();
    }

    public List<PurchasePlace> getFilterCafes() {
        return purchasePlaceService.getCafePlacesWithFilterRecords();
    }

    public BrewRecordResponse get(Long id) {
        return brewRecordService.getCafeRecord(id);
    }

    public CafeFilterCoffeeForm getUpdateForm(Long id) {
        BrewRecordResponse record = get(id);
        CoffeeBeanUpdateForm coffeeBeanForm = coffeeBeanService.getUpdateForm(record.getCoffeeBeanId());

        CafeFilterCoffeeForm form = new CafeFilterCoffeeForm();
        form.setName(coffeeBeanForm.getName());
        form.setRoastery(coffeeBeanForm.getRoastery());
        form.setOriginCountryCode(coffeeBeanForm.getOriginCountryCode());
        form.setRegion(coffeeBeanForm.getRegion());
        form.setVariety(coffeeBeanForm.getVariety());
        form.setProcessType(coffeeBeanForm.getProcessType());
        form.setFlavorNotes(coffeeBeanForm.getFlavorNotes());
        form.setCustomFlavorNotesText(coffeeBeanForm.getCustomFlavorNotesText());
        form.setCafeName(coffeeBeanForm.getPurchasePlaceName());
        form.setCafeAddress(coffeeBeanForm.getPurchasePlaceAddress());
        form.setCafeUrl(coffeeBeanForm.getPurchasePlaceUrl());
        form.setCafeLatitude(coffeeBeanForm.getPurchasePlaceLatitude());
        form.setCafeLongitude(coffeeBeanForm.getPurchasePlaceLongitude());
        form.setVisitedDate(record.getBrewedDate());
        form.setTemperatureType(record.getTemperatureType());
        form.setRecordRecipe(hasRecipe(record));
        form.setBrewMethod(record.getBrewMethod());
        form.setBeanAmount(record.getBeanAmount());
        form.setWaterAmount(record.getWaterAmount());
        form.setWaterTemperature(record.getWaterTemperature());
        form.setGrindSizeMicron(record.getGrindSizeMicron());
        form.setBrewTimeSec(record.getBrewTimeSec());
        form.setPourSteps(record.getPourSteps());
        form.setRating(record.getRating());
        form.setAcidity(record.getAcidity());
        form.setSweetness(record.getSweetness());
        form.setBitterness(record.getBitterness());
        form.setBody(record.getBody());
        form.setAroma(record.getAroma());
        form.setBalance(record.getBalance());
        form.setFeelingTags(record.getFeelingTags());
        form.setCustomFeelingTagsText(String.join(", ", record.getCustomFeelingTags()));
        form.setMemo(record.getMemo());
        return form;
    }

    private boolean contains(String value, String normalizedQuery) {
        return StringUtils.hasText(value) && normalize(value).contains(normalizedQuery);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    @Transactional
    public Long create(CafeFilterCoffeeForm form) {
        Long coffeeBeanId = coffeeBeanService.create(toCoffeeBeanForm(form));
        return brewRecordService.create(toBrewRecordForm(form, coffeeBeanId));
    }

    @Transactional
    public void update(Long id, CafeFilterCoffeeForm form) {
        BrewRecordResponse record = get(id);
        CoffeeBeanUpdateForm coffeeBeanForm = coffeeBeanService.getUpdateForm(record.getCoffeeBeanId());
        coffeeBeanService.update(record.getCoffeeBeanId(), toCoffeeBeanUpdateForm(form, coffeeBeanForm));
        brewRecordService.update(id, toBrewRecordForm(form, record.getCoffeeBeanId()));
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
        brewRecordForm.setAcidity(form.getAcidity());
        brewRecordForm.setSweetness(form.getSweetness());
        brewRecordForm.setBitterness(form.getBitterness());
        brewRecordForm.setBody(form.getBody());
        brewRecordForm.setAroma(form.getAroma());
        brewRecordForm.setBalance(form.getBalance());
        brewRecordForm.setFeelingTags(form.getFeelingTags());
        brewRecordForm.setCustomFeelingTagsText(form.getCustomFeelingTagsText());
        brewRecordForm.setMemo(form.getMemo());
        return brewRecordForm;
    }

    private CoffeeBeanUpdateForm toCoffeeBeanUpdateForm(CafeFilterCoffeeForm form, CoffeeBeanUpdateForm coffeeBeanForm) {
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
        coffeeBeanForm.setPurchasePlaceId(null);
        coffeeBeanForm.setPurchasePlaceName(form.getCafeName());
        coffeeBeanForm.setPurchasePlaceType(PurchasePlaceType.CAFE);
        coffeeBeanForm.setPurchasePlaceAddress(form.getCafeAddress());
        coffeeBeanForm.setPurchasePlaceUrl(form.getCafeUrl());
        coffeeBeanForm.setPurchasePlaceLatitude(form.getCafeLatitude());
        coffeeBeanForm.setPurchasePlaceLongitude(form.getCafeLongitude());
        return coffeeBeanForm;
    }

    private boolean hasRecipe(BrewRecordResponse record) {
        return record.getBrewMethod() != null
                || record.getBeanAmount() != null
                || record.getWaterAmount() != null
                || record.getWaterTemperature() != null
                || record.getGrindSizeMicron() != null
                || record.getBrewTimeSec() != null
                || !record.getPourSteps().isEmpty();
    }
}
