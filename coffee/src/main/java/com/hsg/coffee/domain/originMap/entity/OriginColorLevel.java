package com.hsg.coffee.domain.originMap.entity;

public enum OriginColorLevel {
    NONE(0, "#E8E1D5"),
    LOW(1, "#E8C28A"),
    MEDIUM(2, "#C98A3B"),
    HIGH(4, "#5C2E1F");

    private final int minimumBeanCount;
    private final String fillColor;

    OriginColorLevel(int minimumBeanCount, String fillColor) {
        this.minimumBeanCount = minimumBeanCount;
        this.fillColor = fillColor;
    }

    public int getMinimumBeanCount() {
        return minimumBeanCount;
    }

    public String getFillColor() {
        return fillColor;
    }

    public static OriginColorLevel fromBeanCount(long beanCount) {
        if (beanCount >= HIGH.minimumBeanCount) {
            return HIGH;
        }
        if (beanCount >= MEDIUM.minimumBeanCount) {
            return MEDIUM;
        }
        if (beanCount >= LOW.minimumBeanCount) {
            return LOW;
        }
        return NONE;
    }
}
