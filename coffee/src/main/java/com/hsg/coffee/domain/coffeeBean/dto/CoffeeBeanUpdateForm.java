package com.hsg.coffee.domain.coffeeBean.dto;

import java.time.LocalDate;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlace;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CoffeeBeanUpdateForm {

    @NotBlank(message = "원두 이름은 필수입니다.")
    @Size(max = 100, message = "원두 이름은 100자 이하로 입력해주세요.")
    private String name;

    @Size(max = 100, message = "로스터리는 100자 이하로 입력해주세요.")
    private String roastery;

    @Size(max = 100, message = "국가는 100자 이하로 입력해주세요.")
    private String country;

    @Size(max = 100, message = "지역은 100자 이하로 입력해주세요.")
    private String region;

    @Size(max = 100, message = "농장/워싱스테이션은 100자 이하로 입력해주세요.")
    private String farm;

    @Size(max = 100, message = "품종은 100자 이하로 입력해주세요.")
    private String variety;

    @Size(max = 50, message = "고도는 50자 이하로 입력해주세요.")
    private String altitude;

    private ProcessType processType;

    @Size(max = 500, message = "향미 노트는 500자 이하로 입력해주세요.")
    private String flavorNotes;

    @Size(max = 1000, message = "메모는 1000자 이하로 입력해주세요.")
    private String memo;

    private LocalDate roastedDate;

    private LocalDate purchasedDate;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Integer price;

    @Min(value = 0, message = "용량은 0g 이상이어야 합니다.")
    private Integer weight;

    private Long purchasePlaceId;

    @Size(max = 100, message = "구매처 이름은 100자 이하로 입력해주세요.")
    private String purchasePlaceName;

    private PurchasePlaceType purchasePlaceType;

    @Size(max = 300, message = "구매처 주소는 300자 이하로 입력해주세요.")
    private String purchasePlaceAddress;

    @Size(max = 1000, message = "구매처 메모는 1000자 이하로 입력해주세요.")
    private String purchasePlaceMemo;

    public static CoffeeBeanUpdateForm from(CoffeeBean coffeeBean) {
        CoffeeBeanUpdateForm form = new CoffeeBeanUpdateForm();
        form.name = coffeeBean.getName();
        form.roastery = coffeeBean.getRoastery();
        form.country = coffeeBean.getCountry();
        form.region = coffeeBean.getRegion();
        form.farm = coffeeBean.getFarm();
        form.variety = coffeeBean.getVariety();
        form.altitude = coffeeBean.getAltitude();
        form.processType = coffeeBean.getProcessType();
        form.flavorNotes = coffeeBean.getFlavorNotes();
        form.memo = coffeeBean.getMemo();
        form.roastedDate = coffeeBean.getRoastedDate();
        form.purchasedDate = coffeeBean.getPurchasedDate();
        form.price = coffeeBean.getPrice();
        form.weight = coffeeBean.getWeight();

        PurchasePlace purchasePlace = coffeeBean.getPurchasePlace();
        if (purchasePlace != null) {
            form.purchasePlaceId = purchasePlace.getId();
            form.purchasePlaceName = purchasePlace.getName();
            form.purchasePlaceType = purchasePlace.getType();
            form.purchasePlaceAddress = purchasePlace.getAddress();
            form.purchasePlaceMemo = purchasePlace.getMemo();
        }

        return form;
    }
}
