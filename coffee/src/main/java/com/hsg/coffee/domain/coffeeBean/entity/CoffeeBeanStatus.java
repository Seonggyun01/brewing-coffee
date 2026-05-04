package com.hsg.coffee.domain.coffeeBean.entity;

public enum CoffeeBeanStatus {
    CURRENT("보유 중"),
    FINISHED("소진 기록"),
    CAFE("카페 음용");

    private final String displayName;

    CoffeeBeanStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
