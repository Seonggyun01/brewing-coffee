package com.hsg.coffee.domain.coffeeBean.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanResponse;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanUpdateForm;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.service.PurchasePlaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoffeeBeanService {

    private static final Pattern TAG_DELIMITER = Pattern.compile("[,\\n]");

    private final CoffeeBeanRepository coffeeBeanRepository;
    private final PurchasePlaceService purchasePlaceService;

    @Transactional
    public Long create(CoffeeBeanCreateForm form) {
        PurchasePlace purchasePlace = purchasePlaceService.selectOrCreateIfPresent(
                form.getPurchasePlaceId(),
                form.getPurchasePlaceName(),
                form.getPurchasePlaceType(),
                form.getPurchasePlaceAddress(),
                form.getPurchasePlaceMemo()
        );

        CoffeeBean coffeeBean = coffeeBeanRepository.save(CoffeeBean.create(
                form.getName(),
                form.getRoastery(),
                form.getCountry(),
                form.getRegion(),
                form.getFarm(),
                form.getVariety(),
                form.getAltitude(),
                form.getProcessType(),
                form.getFlavorNotes(),
                parseTags(form.getCustomFlavorNotesText()),
                form.getMemo(),
                form.getRoastedDate(),
                form.getPurchasedDate(),
                form.getPrice(),
                form.getWeight(),
                form.getStatus(),
                purchasePlace
        ));
        return coffeeBean.getId();
    }

    public CoffeeBeanResponse get(Long id) {
        return CoffeeBeanResponse.from(findEntity(id));
    }

    public List<CoffeeBeanResponse> getAll() {
        return coffeeBeanRepository.findAllByOrderByIdDesc()
                .stream()
                .map(CoffeeBeanResponse::from)
                .toList();
    }

    public List<CoffeeBeanResponse> searchByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getAll();
        }

        return coffeeBeanRepository.findByNameContainingIgnoreCaseOrderByIdDesc(keyword.trim())
                .stream()
                .map(CoffeeBeanResponse::from)
                .toList();
    }

    public List<CoffeeBeanResponse> searchByRoastery(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getAll();
        }

        return coffeeBeanRepository.findByRoasteryContainingIgnoreCaseOrderByIdDesc(keyword.trim())
                .stream()
                .map(CoffeeBeanResponse::from)
                .toList();
    }

    public CoffeeBeanUpdateForm getUpdateForm(Long id) {
        return CoffeeBeanUpdateForm.from(findEntity(id));
    }

    @Transactional
    public void update(Long id, CoffeeBeanUpdateForm form) {
        CoffeeBean coffeeBean = findEntity(id);
        PurchasePlace purchasePlace = purchasePlaceService.selectOrCreateIfPresent(
                form.getPurchasePlaceId(),
                form.getPurchasePlaceName(),
                form.getPurchasePlaceType(),
                form.getPurchasePlaceAddress(),
                form.getPurchasePlaceMemo()
        );

        coffeeBean.update(
                form.getName(),
                form.getRoastery(),
                form.getCountry(),
                form.getRegion(),
                form.getFarm(),
                form.getVariety(),
                form.getAltitude(),
                form.getProcessType(),
                form.getFlavorNotes(),
                parseTags(form.getCustomFlavorNotesText()),
                form.getMemo(),
                form.getRoastedDate(),
                form.getPurchasedDate(),
                form.getPrice(),
                form.getWeight(),
                form.getStatus(),
                purchasePlace
        );
    }

    @Transactional
    public void delete(Long id) {
        CoffeeBean coffeeBean = findEntity(id);
        coffeeBeanRepository.delete(coffeeBean);
    }

    public boolean exists(Long id) {
        return coffeeBeanRepository.existsById(id);
    }

    private CoffeeBean findEntity(Long id) {
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
