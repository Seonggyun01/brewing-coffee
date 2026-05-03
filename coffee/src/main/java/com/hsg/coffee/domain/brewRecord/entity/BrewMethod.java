package com.hsg.coffee.domain.brewRecord.entity;

public enum BrewMethod {
    V60("V60"),
    KALITA("칼리타"),
    ORIGAMI("오리가미"),
    CHEMEX("케멕스"),
    AEROPRESS("에어로프레스"),
    FRENCH_PRESS("프렌치프레스"),
    ESPRESSO("에스프레소"),
    COLD_BREW("콜드브루"),
    OTHER("기타");

    private final String displayName;

    BrewMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
