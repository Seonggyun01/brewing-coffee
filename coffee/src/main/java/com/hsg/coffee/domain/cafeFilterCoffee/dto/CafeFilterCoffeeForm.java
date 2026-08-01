package com.hsg.coffee.domain.cafeFilterCoffee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.hsg.coffee.domain.brewRecord.dto.BrewPourStepForm;
import com.hsg.coffee.domain.brewRecord.entity.BrewFeelingTag;
import com.hsg.coffee.domain.brewRecord.entity.BrewMethod;
import com.hsg.coffee.domain.brewRecord.entity.BrewTemperatureType;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CafeFilterCoffeeForm {

    @NotBlank(message = "원두 이름은 필수입니다.")
    @Size(max = 100, message = "원두 이름은 100자 이하로 입력해주세요.")
    private String name;

    @Size(max = 100, message = "로스터리는 100자 이하로 입력해주세요.")
    private String roastery;

    @Size(max = 2, message = "국가 코드는 2자 이하로 입력해주세요.")
    private String originCountryCode;

    @Size(max = 100, message = "지역은 100자 이하로 입력해주세요.")
    private String region;

    @Size(max = 100, message = "품종은 100자 이하로 입력해주세요.")
    private String variety;

    private ProcessType processType;

    @Size(max = 8, message = "향미 노트는 최대 8개까지 선택할 수 있어요.")
    private List<FlavorNote> flavorNotes = new ArrayList<>();

    @Size(max = 500, message = "직접 입력한 향미 노트는 500자 이하로 입력해주세요.")
    private String customFlavorNotesText;

    @Size(max = 100, message = "카페 이름은 100자 이하로 입력해주세요.")
    private String cafeName;

    @Size(max = 300, message = "주소는 300자 이하로 입력해주세요.")
    private String cafeAddress;

    @Size(max = 500, message = "장소 링크는 500자 이하로 입력해주세요.")
    private String cafeUrl;

    private Double cafeLatitude;

    private Double cafeLongitude;

    private LocalDate visitedDate = LocalDate.now();

    @NotNull(message = "핫/아이스를 선택해주세요.")
    private BrewTemperatureType temperatureType = BrewTemperatureType.HOT;

    private boolean recordRecipe;

    private BrewMethod brewMethod;

    @DecimalMin(value = "0.0", inclusive = false, message = "원두량은 0보다 커야 합니다.")
    private BigDecimal beanAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "물량은 0보다 커야 합니다.")
    private BigDecimal waterAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "물 온도는 0보다 커야 합니다.")
    private BigDecimal waterTemperature;

    @Min(value = 1, message = "분쇄도는 1μm 이상이어야 합니다.")
    @Max(value = 5000, message = "분쇄도는 5000μm 이하로 입력해주세요.")
    private Integer grindSizeMicron;

    @Min(value = 1, message = "종료 시간은 1초 이상이어야 합니다.")
    private Integer brewTimeSec;

    @Valid
    private List<BrewPourStepForm> pourSteps = new ArrayList<>();

    @Min(value = 1, message = "만족도는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "만족도는 5점 이하이어야 합니다.")
    private Integer rating;

    private List<BrewFeelingTag> feelingTags = new ArrayList<>();

    @Size(max = 500, message = "직접 입력한 느낌 태그는 500자 이하로 입력해주세요.")
    private String customFeelingTagsText;

    @Size(max = 1000, message = "메모는 1000자 이하로 입력해주세요.")
    private String memo;
}
