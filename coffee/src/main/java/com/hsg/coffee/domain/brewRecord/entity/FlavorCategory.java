package com.hsg.coffee.domain.brewRecord.entity;

public enum FlavorCategory {
    FLORAL("Floral", "꽃, 허브, 향긋한 인상", "#C8A2C8"),
    CITRUS("Citrus", "상큼하고 밝은 산미", "#F6C945"),
    BERRY("Berry", "베리류의 새콤달콤한 인상", "#9B1B5A"),
    STONE_FRUIT("Stone Fruit", "복숭아, 살구처럼 부드러운 과일감", "#F4A261"),
    TROPICAL_FRUIT("Tropical Fruit", "열대과일의 달고 풍성한 인상", "#F9A03F"),
    APPLE_PEAR("Apple / Pear", "사과, 배처럼 맑고 산뜻한 과일감", "#B7D968"),
    SWEET("Sweet", "설탕, 꿀, 시럽 같은 단맛", "#D6A04D"),
    CHOCOLATE("Chocolate", "초콜릿, 코코아 계열의 묵직한 단맛", "#5C3A2E"),
    NUTTY("Nutty", "견과류의 고소한 인상", "#A47148"),
    ROASTED_SMOKY("Roasted / Smoky", "로스팅, 스모키, 구운 향", "#3B2F2F"),
    SPICE("Spice", "향신료의 따뜻하고 자극적인 느낌", "#B5532F"),
    TEA_LIKE("Tea-like", "차처럼 맑고 우아한 느낌", "#8A9A5B"),
    WINEY_FERMENTED("Winey / Fermented", "와인, 발효, 숙성된 과일 느낌", "#7B2D43"),
    EARTHY_HERBAL("Earthy / Herbal", "흙, 허브, 식물성 느낌", "#6B705C");

    private final String displayName;
    private final String description;
    private final String color;

    FlavorCategory(String displayName, String description, String color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }
}
