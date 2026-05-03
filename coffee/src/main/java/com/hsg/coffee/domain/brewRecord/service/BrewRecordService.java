package com.hsg.coffee.domain.brewRecord.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.dto.BrewRecordForm;
import com.hsg.coffee.domain.brewRecord.dto.BrewRecordResponse;
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
                form.getBeanAmount(),
                form.getWaterAmount(),
                form.getWaterTemperature(),
                form.getGrindSize(),
                form.getBrewTimeSec(),
                form.getRecipe(),
                form.getRating(),
                form.getAcidity(),
                form.getSweetness(),
                form.getBitterness(),
                form.getBody(),
                form.getAroma(),
                form.getBalance(),
                form.getFlavorNotes(),
                form.getFeelingTags(),
                parseTags(form.getCustomFlavorNotesText()),
                parseTags(form.getCustomFeelingTagsText()),
                form.getMemo()
        ));
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
        CoffeeBean coffeeBean = findCoffeeBean(form.getCoffeeBeanId());
        brewRecord.updateCoffeeBean(coffeeBean);
        brewRecord.update(
                form.getBrewedDate(),
                form.getBrewMethod(),
                form.getBeanAmount(),
                form.getWaterAmount(),
                form.getWaterTemperature(),
                form.getGrindSize(),
                form.getBrewTimeSec(),
                form.getRecipe(),
                form.getRating(),
                form.getAcidity(),
                form.getSweetness(),
                form.getBitterness(),
                form.getBody(),
                form.getAroma(),
                form.getBalance(),
                form.getFlavorNotes(),
                form.getFeelingTags(),
                parseTags(form.getCustomFlavorNotesText()),
                parseTags(form.getCustomFeelingTagsText()),
                form.getMemo()
        );
    }

    @Transactional
    public void delete(Long id) {
        brewRecordRepository.delete(findEntity(id));
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
}
