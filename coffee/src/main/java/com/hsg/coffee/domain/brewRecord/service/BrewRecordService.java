package com.hsg.coffee.domain.brewRecord.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.dto.BrewRecordForm;
import com.hsg.coffee.domain.brewRecord.dto.BrewPourStepForm;
import com.hsg.coffee.domain.brewRecord.dto.BrewRecordResponse;
import com.hsg.coffee.domain.brewRecord.entity.BrewPourStep;
import com.hsg.coffee.domain.brewRecord.entity.BrewRecord;
import com.hsg.coffee.domain.brewRecord.repository.BrewRecordRepository;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrewRecordService {

    private static final Pattern TAG_DELIMITER = Pattern.compile("[,\\n]");

    private final BrewRecordRepository brewRecordRepository;
    private final CoffeeBeanRepository coffeeBeanRepository;

    @Transactional
    public Long create(BrewRecordForm form) {
        CoffeeBean coffeeBean = findCoffeeBean(form.getCoffeeBeanId());
        BrewRecord brewRecord = brewRecordRepository.save(BrewRecord.create(
                coffeeBean,
                form.getBrewedDate(),
                form.getBrewMethod(),
                form.getTemperatureType(),
                form.getBeanAmount(),
                form.getWaterAmount(),
                form.getWaterTemperature(),
                form.getGrindSizeMicron(),
                form.getBrewTimeSec(),
                normalizePourSteps(form.getPourSteps(), form.getBrewTimeSec()),
                form.getRating(),
                form.getAcidity(),
                form.getSweetness(),
                form.getBitterness(),
                form.getBody(),
                form.getAroma(),
                form.getBalance(),
                form.getFeelingTags(),
                parseTags(form.getCustomFeelingTagsText()),
                form.getMemo()
        ));
        brewRecord.updateInventoryDeductedWeight(coffeeBean.useWeight(toWholeGram(form.getBeanAmount())));
        return brewRecord.getId();
    }

    public BrewRecordResponse get(Long id) {
        return BrewRecordResponse.from(findEntity(id));
    }

    public List<BrewRecordResponse> getAll() {
        return brewRecordRepository.findAllByOrderByBrewedDateDescIdDesc()
                .stream()
                .map(BrewRecordResponse::from)
                .toList();
    }

    public List<BrewRecordResponse> getByCoffeeBean(Long coffeeBeanId) {
        return brewRecordRepository.findByCoffeeBeanIdOrderByBrewedDateDescIdDesc(coffeeBeanId)
                .stream()
                .map(BrewRecordResponse::from)
                .toList();
    }

    public BrewRecordForm getUpdateForm(Long id) {
        return BrewRecordForm.from(findEntity(id));
    }

    @Transactional
    public void update(Long id, BrewRecordForm form) {
        BrewRecord brewRecord = findEntity(id);
        CoffeeBean previousCoffeeBean = brewRecord.getCoffeeBean();
        previousCoffeeBean.restoreWeight(brewRecord.getInventoryDeductedWeight());

        CoffeeBean coffeeBean = findCoffeeBean(form.getCoffeeBeanId());
        brewRecord.updateCoffeeBean(coffeeBean);
        brewRecord.update(
                form.getBrewedDate(),
                form.getBrewMethod(),
                form.getTemperatureType(),
                form.getBeanAmount(),
                form.getWaterAmount(),
                form.getWaterTemperature(),
                form.getGrindSizeMicron(),
                form.getBrewTimeSec(),
                normalizePourSteps(form.getPourSteps(), form.getBrewTimeSec()),
                form.getRating(),
                form.getAcidity(),
                form.getSweetness(),
                form.getBitterness(),
                form.getBody(),
                form.getAroma(),
                form.getBalance(),
                form.getFeelingTags(),
                parseTags(form.getCustomFeelingTagsText()),
                form.getMemo()
        );
        brewRecord.updateInventoryDeductedWeight(coffeeBean.useWeight(toWholeGram(form.getBeanAmount())));
    }

    @Transactional
    public void delete(Long id) {
        BrewRecord brewRecord = findEntity(id);
        brewRecord.getCoffeeBean().restoreWeight(brewRecord.getInventoryDeductedWeight());
        brewRecordRepository.delete(brewRecord);
    }

    private BrewRecord findEntity(Long id) {
        return brewRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("브루잉 기록을 찾을 수 없습니다. id=" + id));
    }

    private CoffeeBean findCoffeeBean(Long id) {
        return coffeeBeanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("원두를 찾을 수 없습니다. id=" + id));
    }

    private List<String> parseTags(String tagsText) {
        if (tagsText == null || tagsText.isBlank()) {
            return List.of();
        }

        return TAG_DELIMITER.splitAsStream(tagsText)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .limit(12)
                .toList();
    }

    private List<BrewPourStep> normalizePourSteps(List<BrewPourStepForm> pourStepForms, Integer brewTimeSec) {
        if (pourStepForms == null) {
            return List.of();
        }

        int timelineEndSec = brewTimeSec != null && brewTimeSec > 0 ? brewTimeSec : 180;
        return pourStepForms.stream()
                .filter(BrewPourStepForm::isFilled)
                .filter(step -> step.getTimeSec() != null && step.getAmountMl() != null)
                .map(step -> BrewPourStep.of(clampToTimelineEnd(snapToFiveSeconds(step.getTimeSec()), timelineEndSec), step.getAmountMl()))
                .sorted((left, right) -> left.getTimeSec().compareTo(right.getTimeSec()))
                .limit(20)
                .toList();
    }

    private Integer snapToFiveSeconds(Integer timeSec) {
        if (timeSec == null) {
            return null;
        }
        return Math.max(0, Math.round(timeSec / 5.0f) * 5);
    }

    private Integer clampToTimelineEnd(Integer timeSec, int timelineEndSec) {
        if (timeSec == null) {
            return null;
        }
        return Math.min(timeSec, timelineEndSec);
    }

    private Integer toWholeGram(BigDecimal beanAmount) {
        if (beanAmount == null || beanAmount.signum() <= 0) {
            return null;
        }
        return beanAmount.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
