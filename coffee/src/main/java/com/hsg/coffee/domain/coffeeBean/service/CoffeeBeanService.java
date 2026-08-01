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
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.service.PurchasePlaceService;
import com.hsg.coffee.global.country.CountryInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoffeeBeanService {

    private static final Pattern TAG_DELIMITER = Pattern.compile("[,\\n]");

    private final CoffeeBeanRepository coffeeBeanRepository;
    private final PurchasePlaceService purchasePlaceService;
    private final CustomFlavorNoteService customFlavorNoteService;

    @Transactional
    public Long create(CoffeeBeanCreateForm form) {
        PurchasePlace purchasePlace = purchasePlaceService.selectOrCreateIfPresent(
                form.getPurchasePlaceId(),
                form.getPurchasePlaceName(),
                form.getPurchasePlaceType(),
                form.getPurchasePlaceAddress(),
                form.getPurchasePlaceUrl(),
                form.getPurchasePlaceLatitude(),
                form.getPurchasePlaceLongitude(),
                form.getPurchasePlaceMemo()
        );
        CountryInfo originCountry = CountryInfo.findByCode(form.getOriginCountryCode());
        String country = originCountry != null ? originCountry.getEnglishName() : clean(form.getCountry());
        List<String> customFlavorNotes = customFlavorNoteService.ensureAll(parseTags(form.getCustomFlavorNotesText()));

        CoffeeBean coffeeBean = coffeeBeanRepository.save(CoffeeBean.create(
                clean(form.getName()),
                clean(form.getRoastery()),
                country,
                form.getOriginCountryCode(),
                clean(form.getRegion()),
                clean(form.getFarm()),
                clean(form.getVariety()),
                clean(form.getAltitude()),
                form.getProcessType(),
                form.getFlavorNotes(),
                customFlavorNotes,
                clean(form.getMemo()),
                form.getRoastedDate(),
                form.getPurchasedDate(),
                form.getPrice(),
                form.getWeight(),
                form.getStatus(),
                purchasePlace
        ));
        return coffeeBean.getId();
    }

    @Transactional
    public CoffeeBeanResponse get(Long id) {
        CoffeeBean coffeeBean = findEntity(id);
        coffeeBean.syncStatusWithWeight();
        return toResponse(coffeeBean);
    }

    @Transactional
    public List<CoffeeBeanResponse> getAll() {
        return toResponsesWithSyncedStatus(coffeeBeanRepository.findAllByOrderByIdDesc());
    }

    @Transactional
    public List<CoffeeBeanResponse> getInventoryBeans() {
        List<CoffeeBean> coffeeBeans = coffeeBeanRepository.findAllByOrderByIdDesc();
        syncStatusesWithWeight(coffeeBeans);

        return coffeeBeans.stream()
                .filter(coffeeBean -> coffeeBean.getStatus() != CoffeeBeanStatus.CAFE)
                .map(this::toResponse)
                .toList();
    }

    public List<CoffeeBeanResponse> searchByName(String keyword) {
        return findCoffeeBeans(keyword, null);
    }

    @Transactional
    public List<CoffeeBeanResponse> findCoffeeBeans(String keyword, String countryCode) {
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedCountryCode = normalizeCountryCode(countryCode);

        List<CoffeeBean> coffeeBeans;
        if (normalizedCountryCode != null && trimmedKeyword != null) {
            coffeeBeans = coffeeBeanRepository.findByOriginCountryCodeAndNameContainingIgnoreCaseOrderByIdDesc(
                    normalizedCountryCode,
                    trimmedKeyword
            );
        } else if (normalizedCountryCode != null) {
            coffeeBeans = coffeeBeanRepository.findByOriginCountryCodeOrderByIdDesc(normalizedCountryCode);
        } else if (trimmedKeyword != null) {
            coffeeBeans = coffeeBeanRepository.findByNameContainingIgnoreCaseOrderByIdDesc(trimmedKeyword);
        } else {
            coffeeBeans = coffeeBeanRepository.findAllByOrderByIdDesc();
        }

        return toResponsesWithSyncedStatus(coffeeBeans);
    }

    @Transactional
    public List<CoffeeBeanResponse> searchByRoastery(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getAll();
        }

        return toResponsesWithSyncedStatus(
                coffeeBeanRepository.findByRoasteryContainingIgnoreCaseOrderByIdDesc(keyword.trim())
        );
    }

    @Transactional
    public CoffeeBeanUpdateForm getUpdateForm(Long id) {
        CoffeeBean coffeeBean = findEntity(id);
        coffeeBean.syncStatusWithWeight();
        return CoffeeBeanUpdateForm.from(coffeeBean);
    }

    @Transactional
    public void update(Long id, CoffeeBeanUpdateForm form) {
        CoffeeBean coffeeBean = findEntity(id);
        PurchasePlace purchasePlace = purchasePlaceService.selectOrCreateIfPresent(
                form.getPurchasePlaceId(),
                form.getPurchasePlaceName(),
                form.getPurchasePlaceType(),
                form.getPurchasePlaceAddress(),
                form.getPurchasePlaceUrl(),
                form.getPurchasePlaceLatitude(),
                form.getPurchasePlaceLongitude(),
                form.getPurchasePlaceMemo()
        );
        CountryInfo originCountry = CountryInfo.findByCode(form.getOriginCountryCode());
        String country = originCountry != null ? originCountry.getEnglishName() : clean(form.getCountry());
        List<String> customFlavorNotes = customFlavorNoteService.ensureAll(parseTags(form.getCustomFlavorNotesText()));

        coffeeBean.update(
                clean(form.getName()),
                clean(form.getRoastery()),
                country,
                form.getOriginCountryCode(),
                clean(form.getRegion()),
                clean(form.getFarm()),
                clean(form.getVariety()),
                clean(form.getAltitude()),
                form.getProcessType(),
                form.getFlavorNotes(),
                customFlavorNotes,
                clean(form.getMemo()),
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

    private CoffeeBeanResponse toResponse(CoffeeBean coffeeBean) {
        return CoffeeBeanResponse.from(
                coffeeBean,
                customFlavorNoteService.findDetails(coffeeBean.getCustomFlavorNotes())
        );
    }

    private List<CoffeeBeanResponse> toResponsesWithSyncedStatus(List<CoffeeBean> coffeeBeans) {
        syncStatusesWithWeight(coffeeBeans);
        return coffeeBeans.stream()
                .map(this::toResponse)
                .toList();
    }

    private void syncStatusesWithWeight(List<CoffeeBean> coffeeBeans) {
        coffeeBeans.forEach(CoffeeBean::syncStatusWithWeight);
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

    private String normalizeCountryCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return null;
        }
        return countryCode.trim().toUpperCase();
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
