package com.hsg.coffee.domain.brewRecord.entity;

public enum BrewFeelingTag {
    CLEAN("깔끔함"),
    JUICY("쥬시함"),
    SMOOTH("부드러움"),
    BRIGHT("밝음"),
    RICH("풍성함"),
    CREAMY("크리미함"),
    SILKY("실키함"),
    LIGHT("가벼움"),
    HEAVY("묵직함"),
    DRY("드라이함"),
    LONG_AFTERTASTE("긴 여운"),
    SWEET_FINISH("달콤한 마무리"),
    UNDER_EXTRACTED("과소추출 느낌"),
    OVER_EXTRACTED("과다추출 느낌");

    private final String displayName;

    BrewFeelingTag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
