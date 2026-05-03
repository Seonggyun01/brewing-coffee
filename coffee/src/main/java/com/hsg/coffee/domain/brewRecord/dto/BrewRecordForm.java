package com.hsg.coffee.domain.brewRecord.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.hsg.coffee.domain.brewRecord.entity.BrewFeelingTag;
import com.hsg.coffee.domain.brewRecord.entity.BrewMethod;
import com.hsg.coffee.domain.brewRecord.entity.BrewRecord;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BrewRecordForm {

    @NotNull(message = "원두를 선택해주세요.")
    private Long coffeeBeanId;

    private LocalDate brewedDate = LocalDate.now();

    private BrewMethod brewMethod;

    @DecimalMin(value = "0.0", inclusive = false, message = "원두량은 0보다 커야 합니다.")
    private BigDecimal beanAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "물량은 0보다 커야 합니다.")
    private BigDecimal waterAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "물 온도는 0보다 커야 합니다.")
    private BigDecimal waterTemperature;

    @Size(max = 50, message = "분쇄도는 50자 이하로 입력해주세요.")
    private String grindSize;

    @Min(value = 0, message = "추출 시간은 0초 이상이어야 합니다.")
    private Integer brewTimeSec;

    @Size(max = 1000, message = "레시피는 1000자 이하로 입력해주세요.")
    private String recipe;

    @Min(value = 1, message = "만족도는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "만족도는 5점 이하이어야 합니다.")
    private Integer rating;

    @Min(1)
    @Max(5)
    private Integer acidity;

    @Min(1)
    @Max(5)
    private Integer sweetness;

    @Min(1)
    @Max(5)
    private Integer bitterness;

    @Min(1)
    @Max(5)
    private Integer body;

    @Min(1)
    @Max(5)
    private Integer aroma;

    @Min(1)
    @Max(5)
    private Integer balance;

    @Size(max = 8, message = "향미 노트는 최대 8개까지 선택할 수 있어요.")
    private List<FlavorNote> flavorNotes = new ArrayList<>();

    private List<BrewFeelingTag> feelingTags = new ArrayList<>();

    @Size(max = 500, message = "직접 입력한 향미 노트는 500자 이하로 입력해주세요.")
    private String customFlavorNotesText;

    @Size(max = 500, message = "직접 입력한 느낌 태그는 500자 이하로 입력해주세요.")
    private String customFeelingTagsText;

    @Size(max = 1000, message = "메모는 1000자 이하로 입력해주세요.")
    private String memo;

    public static BrewRecordForm from(BrewRecord brewRecord) {
        BrewRecordForm form = new BrewRecordForm();
        form.setCoffeeBeanId(brewRecord.getCoffeeBean().getId());
        form.setBrewedDate(brewRecord.getBrewedDate());
        form.setBrewMethod(brewRecord.getBrewMethod());
        form.setBeanAmount(brewRecord.getBeanAmount());
        form.setWaterAmount(brewRecord.getWaterAmount());
        form.setWaterTemperature(brewRecord.getWaterTemperature());
        form.setGrindSize(brewRecord.getGrindSize());
        form.setBrewTimeSec(brewRecord.getBrewTimeSec());
        form.setRecipe(brewRecord.getRecipe());
        form.setRating(brewRecord.getRating());
        form.setAcidity(brewRecord.getAcidity());
        form.setSweetness(brewRecord.getSweetness());
        form.setBitterness(brewRecord.getBitterness());
        form.setBody(brewRecord.getBody());
        form.setAroma(brewRecord.getAroma());
        form.setBalance(brewRecord.getBalance());
        form.setFlavorNotes(new ArrayList<>(brewRecord.getFlavorNotes()));
        form.setFeelingTags(new ArrayList<>(brewRecord.getFeelingTags()));
        form.setCustomFlavorNotesText(String.join(", ", brewRecord.getCustomFlavorNotes()));
        form.setCustomFeelingTagsText(String.join(", ", brewRecord.getCustomFeelingTags()));
        form.setMemo(brewRecord.getMemo());
        return form;
    }
}
