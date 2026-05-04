package com.hsg.coffee.domain.brewRecord.entity;

public enum BrewTemperatureType {
    HOT("핫"),
    ICE("아이스");

    private final String displayName;

    BrewTemperatureType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
