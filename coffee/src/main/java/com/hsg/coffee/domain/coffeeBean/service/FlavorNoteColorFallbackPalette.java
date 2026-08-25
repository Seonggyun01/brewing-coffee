package com.hsg.coffee.domain.coffeeBean.service;

final class FlavorNoteColorFallbackPalette {

    private FlavorNoteColorFallbackPalette() {
    }

    static String resolve(String flavorNoteName) {
        String normalized = flavorNoteName == null ? "" : flavorNoteName.toLowerCase();
        if (containsAny(normalized, "귤", "오렌지", "레몬", "라임", "시트러스", "citrus", "orange", "lemon", "lime")) {
            return "#F6A23A";
        }
        if (containsAny(normalized, "복숭", "살구", "자두", "peach", "apricot", "plum")) {
            return "#F4A261";
        }
        if (containsAny(normalized, "베리", "딸기", "라즈", "블루베리", "berry", "strawberry", "blueberry")) {
            return "#C94B70";
        }
        if (containsAny(normalized, "초콜", "카카오", "chocolate", "cacao", "cocoa")) {
            return "#6A4636";
        }
        if (containsAny(normalized, "견과", "아몬드", "헤이즐", "nut", "almond", "hazelnut")) {
            return "#A47148";
        }
        if (containsAny(normalized, "허브", "풀", "민트", "herb", "grass", "mint")) {
            return "#6F9A5B";
        }
        if (containsAny(normalized, "와인", "발효", "건포도", "wine", "fermented", "raisin")) {
            return "#7B2D43";
        }
        return "#7A5038";
    }

    private static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
