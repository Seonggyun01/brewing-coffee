package com.hsg.coffee.domain.brewRecord.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrewPourStep {

    @Column(name = "pour_time_sec")
    private Integer timeSec;

    @Column(name = "pour_amount_ml")
    private Integer amountMl;

    private BrewPourStep(Integer timeSec, Integer amountMl) {
        this.timeSec = timeSec;
        this.amountMl = amountMl;
    }

    public static BrewPourStep of(Integer timeSec, Integer amountMl) {
        return new BrewPourStep(timeSec, amountMl);
    }
}
