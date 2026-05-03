package com.hsg.coffee.domain.brewRecord.entity;

public enum FlavorNote {
    JASMINE("자스민", FlavorCategory.FLORAL, "#C8A2C8"),
    ROSE("장미", FlavorCategory.FLORAL, "#D9A0B8"),
    LAVENDER("라벤더", FlavorCategory.FLORAL, "#B497D6"),
    ORANGE_BLOSSOM("오렌지 블라썸", FlavorCategory.FLORAL, "#F4C27A"),
    CHAMOMILE("캐모마일", FlavorCategory.FLORAL, "#E8D889"),
    HIBISCUS("히비스커스", FlavorCategory.FLORAL, "#C94F7C"),
    LEMON("레몬", FlavorCategory.CITRUS, "#F6C945"),
    LIME("라임", FlavorCategory.CITRUS, "#B7D968"),
    ORANGE("오렌지", FlavorCategory.CITRUS, "#F79A3E"),
    GRAPEFRUIT("자몽", FlavorCategory.CITRUS, "#F06F61"),
    YUZU("유자", FlavorCategory.CITRUS, "#F7D84A"),
    TANGERINE("귤", FlavorCategory.CITRUS, "#F58B3C"),
    STRAWBERRY("딸기", FlavorCategory.BERRY, "#D94B6A"),
    RASPBERRY("라즈베리", FlavorCategory.BERRY, "#C72D62"),
    BLUEBERRY("블루베리", FlavorCategory.BERRY, "#4C3A8C"),
    BLACKBERRY("블랙베리", FlavorCategory.BERRY, "#3E244C"),
    CRANBERRY("크랜베리", FlavorCategory.BERRY, "#B21F45"),
    BLACKCURRANT("블랙커런트", FlavorCategory.BERRY, "#5A174A"),
    CHERRY("체리", FlavorCategory.BERRY, "#B5233F"),
    PEACH("복숭아", FlavorCategory.STONE_FRUIT, "#F4A261"),
    APRICOT("살구", FlavorCategory.STONE_FRUIT, "#F5B66A"),
    PLUM("자두", FlavorCategory.STONE_FRUIT, "#7D416A"),
    NECTARINE("천도복숭아", FlavorCategory.STONE_FRUIT, "#E97752"),
    YELLOW_PEACH("황도", FlavorCategory.STONE_FRUIT, "#F7C75D"),
    WHITE_PEACH("백도", FlavorCategory.STONE_FRUIT, "#F8C7B8"),
    MANGO("망고", FlavorCategory.TROPICAL_FRUIT, "#F9A03F"),
    PINEAPPLE("파인애플", FlavorCategory.TROPICAL_FRUIT, "#EBC84A"),
    PASSION_FRUIT("패션프루트", FlavorCategory.TROPICAL_FRUIT, "#F28C28"),
    PAPAYA("파파야", FlavorCategory.TROPICAL_FRUIT, "#F4A146"),
    GUAVA("구아바", FlavorCategory.TROPICAL_FRUIT, "#F07C78"),
    LYCHEE("리치", FlavorCategory.TROPICAL_FRUIT, "#F2B8AA"),
    BANANA("바나나", FlavorCategory.TROPICAL_FRUIT, "#E8D25A"),
    APPLE("사과", FlavorCategory.APPLE_PEAR, "#C9D96A"),
    GREEN_APPLE("청사과", FlavorCategory.APPLE_PEAR, "#9CCB5B"),
    RED_APPLE("빨간 사과", FlavorCategory.APPLE_PEAR, "#C84D4D"),
    PEAR("배", FlavorCategory.APPLE_PEAR, "#D7D987"),
    GRAPE("포도", FlavorCategory.APPLE_PEAR, "#7E5AA6"),
    HONEY("꿀", FlavorCategory.SWEET, "#D6A04D"),
    BROWN_SUGAR("흑설탕", FlavorCategory.SWEET, "#A96F3D"),
    MAPLE_SYRUP("메이플 시럽", FlavorCategory.SWEET, "#B87936"),
    CARAMEL("카라멜", FlavorCategory.SWEET, "#C47A32"),
    VANILLA("바닐라", FlavorCategory.SWEET, "#E0C176"),
    TOFFEE("토피", FlavorCategory.SWEET, "#B66A32"),
    MILK_CHOCOLATE("밀크 초콜릿", FlavorCategory.CHOCOLATE, "#7B4B37"),
    DARK_CHOCOLATE("다크 초콜릿", FlavorCategory.CHOCOLATE, "#5C3A2E"),
    CACAO("카카오", FlavorCategory.CHOCOLATE, "#4B2E26"),
    COCOA("코코아", FlavorCategory.CHOCOLATE, "#6A4636"),
    BROWNIE("브라우니", FlavorCategory.CHOCOLATE, "#4A3028"),
    ALMOND("아몬드", FlavorCategory.NUTTY, "#A47148"),
    HAZELNUT("헤이즐넛", FlavorCategory.NUTTY, "#8E5E3C"),
    PEANUT("땅콩", FlavorCategory.NUTTY, "#B88A56"),
    WALNUT("호두", FlavorCategory.NUTTY, "#7A5038"),
    CHESTNUT("밤", FlavorCategory.NUTTY, "#A86E45"),
    TOAST("토스트", FlavorCategory.ROASTED_SMOKY, "#8A5A3C"),
    ROASTED_NUTS("구운 견과", FlavorCategory.ROASTED_SMOKY, "#6C4630"),
    SMOKE("스모크", FlavorCategory.ROASTED_SMOKY, "#3B2F2F"),
    TOBACCO("담배", FlavorCategory.ROASTED_SMOKY, "#5A4636"),
    BAKED_BREAD("구운 빵", FlavorCategory.ROASTED_SMOKY, "#A16F42"),
    CINNAMON("시나몬", FlavorCategory.SPICE, "#B5532F"),
    CLOVE("정향", FlavorCategory.SPICE, "#7B3B2F"),
    NUTMEG("넛맥", FlavorCategory.SPICE, "#9D5C3A"),
    CARDAMOM("카다멈", FlavorCategory.SPICE, "#7F9F6B"),
    BLACK_PEPPER("흑후추", FlavorCategory.SPICE, "#3F352E"),
    GINGER("생강", FlavorCategory.SPICE, "#D39A4A"),
    BLACK_TEA("홍차", FlavorCategory.TEA_LIKE, "#8A5A3B"),
    GREEN_TEA("녹차", FlavorCategory.TEA_LIKE, "#8A9A5B"),
    EARL_GREY("얼그레이", FlavorCategory.TEA_LIKE, "#66728C"),
    OOLONG("우롱차", FlavorCategory.TEA_LIKE, "#947449"),
    JASMINE_TEA("자스민 차", FlavorCategory.TEA_LIKE, "#B8B877"),
    HERBAL_TEA("허브티", FlavorCategory.TEA_LIKE, "#7A9860"),
    RED_WINE("레드 와인", FlavorCategory.WINEY_FERMENTED, "#7B2D43"),
    WHITE_WINE("화이트 와인", FlavorCategory.WINEY_FERMENTED, "#D7C978"),
    BRANDY("브랜디", FlavorCategory.WINEY_FERMENTED, "#9B5635"),
    FERMENTED_FRUIT("발효 과일", FlavorCategory.WINEY_FERMENTED, "#8F3D54"),
    RAISIN("건포도", FlavorCategory.WINEY_FERMENTED, "#5B3144"),
    DRIED_FRUIT("말린 과일", FlavorCategory.WINEY_FERMENTED, "#9E5D3D"),
    HERBAL("허브", FlavorCategory.EARTHY_HERBAL, "#6B705C"),
    GRASS("풀", FlavorCategory.EARTHY_HERBAL, "#7FA35A"),
    EARTH("흙", FlavorCategory.EARTHY_HERBAL, "#6B5A48"),
    CEDAR("시더우드", FlavorCategory.EARTHY_HERBAL, "#8A5D3B"),
    PINE("소나무", FlavorCategory.EARTHY_HERBAL, "#4F7654"),
    MINT("민트", FlavorCategory.EARTHY_HERBAL, "#72A980");

    private final String displayName;
    private final FlavorCategory category;
    private final String color;

    FlavorNote(String displayName, FlavorCategory category, String color) {
        this.displayName = displayName;
        this.category = category;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FlavorCategory getCategory() {
        return category;
    }

    public String getColor() {
        return color;
    }
}
