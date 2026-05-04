package com.hsg.coffee.domain.brewRecord.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.dto.BrewRecordForm;
import com.hsg.coffee.domain.brewRecord.dto.BrewPourStepForm;
import com.hsg.coffee.domain.brewRecord.dto.BrewRecordResponse;
import com.hsg.coffee.domain.brewRecord.entity.BrewFeelingTag;
import com.hsg.coffee.domain.brewRecord.entity.BrewMethod;
import com.hsg.coffee.domain.brewRecord.entity.BrewTemperatureType;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.brewRecord.repository.BrewRecordRepository;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;

@Transactional
@SpringBootTest
class BrewRecordServiceTest {

    @Autowired
    private BrewRecordService brewRecordService;

    @Autowired
    private BrewRecordRepository brewRecordRepository;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    private Long coffeeBeanId;

    @BeforeEach
    void setUp() {
        brewRecordRepository.deleteAll();
        coffeeBeanRepository.deleteAll();

        CoffeeBean coffeeBean = coffeeBeanRepository.save(CoffeeBean.create(
                "에티오피아 구지",
                "브루잉 로스터스",
                "Ethiopia",
                "Guji",
                "Hambela",
                "Heirloom",
                "1900-2100m",
                ProcessType.WASHED,
                List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH),
                List.of("청포도", "오렌지 껍질"),
                "브루잉 테스트용 원두",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                18000,
                200
        ));
        coffeeBeanId = coffeeBean.getId();
    }

    @Test
    void createAndGet() {
        Long id = brewRecordService.create(createForm());

        BrewRecordResponse response = brewRecordService.get(id);

        assertEquals("에티오피아 구지", response.getCoffeeBeanName());
        assertEquals(BrewMethod.V60, response.getBrewMethod());
        assertEquals(BrewTemperatureType.HOT, response.getTemperatureType());
        assertEquals(4, response.getRating());
        assertEquals(4, response.getPourSteps().size());
        assertEquals(40, response.getPourSteps().getFirst().getAmountMl());
        assertEquals(List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH), response.getFlavorNotes());
        assertEquals(List.of("청포도", "오렌지 껍질"), response.getCustomFlavorNotes());
        assertEquals(List.of("맑은 여운"), response.getCustomFeelingTags());
        assertEquals("자스민, 레몬, 복숭아, 청포도, 오렌지 껍질", response.getFlavorNoteSummary());
        assertEquals(185, coffeeBeanRepository.findById(coffeeBeanId).orElseThrow().getWeight());
    }

    @Test
    void getAllAndByCoffeeBean() {
        brewRecordService.create(createForm());

        List<BrewRecordResponse> all = brewRecordService.getAll();
        List<BrewRecordResponse> byCoffeeBean = brewRecordService.getByCoffeeBean(coffeeBeanId);

        assertEquals(1, all.size());
        assertEquals(1, byCoffeeBean.size());
        assertEquals(coffeeBeanId, byCoffeeBean.getFirst().getCoffeeBeanId());
    }

    @Test
    void update() {
        Long id = brewRecordService.create(createForm());
        BrewRecordForm updateForm = brewRecordService.getUpdateForm(id);
        updateForm.setRating(5);
        updateForm.setAcidity(5);
        updateForm.setBeanAmount(BigDecimal.valueOf(20));

        brewRecordService.update(id, updateForm);

        BrewRecordResponse response = brewRecordService.get(id);
        assertEquals(5, response.getRating());
        assertEquals(5, response.getAcidity());
        assertEquals(180, coffeeBeanRepository.findById(coffeeBeanId).orElseThrow().getWeight());
        assertEquals(List.of(FlavorNote.JASMINE, FlavorNote.LEMON, FlavorNote.PEACH), response.getFlavorNotes());
        assertEquals(List.of("청포도", "오렌지 껍질"), response.getCustomFlavorNotes());
    }

    @Test
    void delete() {
        Long id = brewRecordService.create(createForm());

        brewRecordService.delete(id);

        assertFalse(brewRecordRepository.existsById(id));
        assertEquals(200, coffeeBeanRepository.findById(coffeeBeanId).orElseThrow().getWeight());
    }

    @Test
    void createWithFinishedBeanDoesNotUpdateRemainingWeight() {
        CoffeeBean coffeeBean = coffeeBeanRepository.findById(coffeeBeanId).orElseThrow();
        coffeeBean.update(
                coffeeBean.getName(),
                coffeeBean.getRoastery(),
                coffeeBean.getCountry(),
                coffeeBean.getRegion(),
                coffeeBean.getFarm(),
                coffeeBean.getVariety(),
                coffeeBean.getAltitude(),
                coffeeBean.getProcessType(),
                coffeeBean.getFlavorNotes(),
                coffeeBean.getCustomFlavorNotes(),
                coffeeBean.getMemo(),
                coffeeBean.getRoastedDate(),
                coffeeBean.getPurchasedDate(),
                coffeeBean.getPrice(),
                0,
                CoffeeBeanStatus.FINISHED,
                coffeeBean.getPurchasePlace()
        );

        Long id = brewRecordService.create(createForm());
        brewRecordService.delete(id);

        CoffeeBean updatedCoffeeBean = coffeeBeanRepository.findById(coffeeBeanId).orElseThrow();
        assertEquals(0, updatedCoffeeBean.getWeight());
        assertEquals(CoffeeBeanStatus.FINISHED, updatedCoffeeBean.getStatus());
    }

    @Test
    void createWithMissingCoffeeBean() {
        BrewRecordForm form = createForm();
        form.setCoffeeBeanId(999L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> brewRecordService.create(form)
        );

        assertEquals("원두를 찾을 수 없습니다. id=999", exception.getMessage());
    }

    private BrewRecordForm createForm() {
        BrewRecordForm form = new BrewRecordForm();
        form.setCoffeeBeanId(coffeeBeanId);
        form.setBrewedDate(LocalDate.of(2026, 5, 3));
        form.setBrewMethod(BrewMethod.V60);
        form.setTemperatureType(BrewTemperatureType.HOT);
        form.setBeanAmount(BigDecimal.valueOf(15));
        form.setWaterAmount(BigDecimal.valueOf(240));
        form.setWaterTemperature(BigDecimal.valueOf(92));
        form.setGrindSizeMicron(720);
        form.setBrewTimeSec(150);
        form.setPourSteps(List.of(
                new BrewPourStepForm(0, 40),
                new BrewPourStepForm(40, 80),
                new BrewPourStepForm(85, 80),
                new BrewPourStepForm(125, 40)
        ));
        form.setRating(4);
        form.setAcidity(4);
        form.setSweetness(4);
        form.setBitterness(2);
        form.setBody(3);
        form.setAroma(5);
        form.setBalance(4);
        form.setFeelingTags(List.of(BrewFeelingTag.CLEAN, BrewFeelingTag.BRIGHT));
        form.setCustomFeelingTagsText("맑은 여운");
        form.setMemo("향이 화사하고 마무리가 깨끗했다.");
        return form;
    }
}
