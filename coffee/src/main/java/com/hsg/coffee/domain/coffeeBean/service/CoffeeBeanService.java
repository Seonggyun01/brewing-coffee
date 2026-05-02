package com.hsg.coffee.domain.coffeeBean.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanResponse;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanUpdateForm;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoffeeBeanService {

    private final CoffeeBeanRepository coffeeBeanRepository;

    @Transactional
    public Long create(CoffeeBeanCreateForm form) {
        CoffeeBean coffeeBean = coffeeBeanRepository.save(form.toEntity());
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
                form.getMemo(),
                form.getRoastedDate(),
                form.getPurchasedDate(),
                form.getPrice(),
                form.getWeight()
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
}
