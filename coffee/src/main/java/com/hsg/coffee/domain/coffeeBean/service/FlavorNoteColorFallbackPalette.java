package com.hsg.coffee.domain.coffeeBean.service;

final class FlavorNoteColorFallbackPalette {

    private static final String DEFAULT_COLOR = "#7A5038";

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
        if (containsAny(normalized, "망고스틴", "mangosteen")) {
            return "#814B73";
        }
        if (containsAny(normalized, "구아바", "용과", "드래곤프루트", "guava", "dragon fruit")) {
            return "#E56F87";
        }
        if (containsAny(normalized, "망고", "파인애플", "패션프루트", "열대과일", "mango", "pineapple", "passion fruit", "tropical")) {
            return "#E8A43A";
        }
        if (containsAny(normalized, "청포도", "백포도", "화이트 그레이프", "포도", "grape", "muscat")) {
            return "#A9BD5A";
        }
        if (containsAny(normalized, "베리", "딸기", "라즈", "블루베리", "berry", "strawberry", "blueberry")) {
            return "#C94B70";
        }
        if (containsAny(normalized, "밀키스", "요구르트", "요거트", "크림 소다", "우유", "milkis", "yogurt", "cream soda", "milky")) {
            return "#9FC9C7";
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
        return DEFAULT_COLOR;
    }

    static String replaceGenericDefault(String flavorNoteName, String storedColor) {
        String resolvedColor = resolve(flavorNoteName);
        if (storedColor == null || storedColor.isBlank()) {
            return resolvedColor;
        }
        if (DEFAULT_COLOR.equalsIgnoreCase(storedColor.trim()) && !DEFAULT_COLOR.equals(resolvedColor)) {
            return resolvedColor;
        }
        return storedColor;
    }

    static boolean isGenericDefault(String color) {
        return color != null && DEFAULT_COLOR.equalsIgnoreCase(color.trim());
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
