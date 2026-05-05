package com.hsg.coffee.global.country;

import java.util.Arrays;
import java.util.Locale;

public enum CountryInfo {

    ETHIOPIA("ET", "Ethiopia", "에티오피아"),
    COLOMBIA("CO", "Colombia", "콜롬비아"),
    BRAZIL("BR", "Brazil", "브라질"),
    KENYA("KE", "Kenya", "케냐"),
    GUATEMALA("GT", "Guatemala", "과테말라"),
    COSTA_RICA("CR", "Costa Rica", "코스타리카"),
    PANAMA("PA", "Panama", "파나마"),
    RWANDA("RW", "Rwanda", "르완다"),
    HONDURAS("HN", "Honduras", "온두라스"),
    EL_SALVADOR("SV", "El Salvador", "엘살바도르"),
    PERU("PE", "Peru", "페루"),
    INDONESIA("ID", "Indonesia", "인도네시아"),
    YEMEN("YE", "Yemen", "예멘"),
    MEXICO("MX", "Mexico", "멕시코");

    private final String code;
    private final String englishName;
    private final String koreanName;

    CountryInfo(String code, String englishName, String koreanName) {
        this.code = code;
        this.englishName = englishName;
        this.koreanName = koreanName;
    }

    public String getCode() {
        return code;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getDisplayName() {
        return koreanName + " (" + englishName + ")";
    }

    public static CountryInfo findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(countryInfo -> countryInfo.code.equals(normalizedCode))
                .findFirst()
                .orElse(null);
    }

    public static String findCodeByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String normalizedName = normalize(name);
        return Arrays.stream(values())
                .filter(countryInfo -> normalize(countryInfo.englishName).equals(normalizedName)
                        || normalize(countryInfo.koreanName).equals(normalizedName)
                        || countryInfo.code.equalsIgnoreCase(name.trim()))
                .map(CountryInfo::getCode)
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String value) {
        return value.trim()
                .replace(" ", "")
                .replace("-", "")
                .toLowerCase(Locale.ROOT);
    }
}
