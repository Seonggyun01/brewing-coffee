package com.hsg.coffee.domain.brewRecord.dto;

import com.hsg.coffee.domain.brewRecord.entity.BrewPourStep;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BrewPourStepForm {

    @Min(value = 0, message = "푸어링 시간은 0초 이상이어야 합니다.")
    @Max(value = 3600, message = "푸어링 시간은 3600초 이하로 입력해주세요.")
    private Integer timeSec;

    @Min(value = 1, message = "푸어링량은 1ml 이상이어야 합니다.")
    @Max(value = 3000, message = "푸어링량은 3000ml 이하로 입력해주세요.")
    private Integer amountMl;

    public BrewPourStepForm(Integer timeSec, Integer amountMl) {
        this.timeSec = timeSec;
        this.amountMl = amountMl;
    }

    public static BrewPourStepForm from(BrewPourStep pourStep) {
        return new BrewPourStepForm(pourStep.getTimeSec(), pourStep.getAmountMl());
    }

    public boolean isFilled() {
        return timeSec != null || amountMl != null;
    }

    public BrewPourStep toEntity() {
        return BrewPourStep.of(timeSec, amountMl);
    }
}
