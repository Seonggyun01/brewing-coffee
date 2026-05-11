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
    BURUNDI("BI", "Burundi", "부룬디"),
    UGANDA("UG", "Uganda", "우간다"),
    HONDURAS("HN", "Honduras", "온두라스"),
    EL_SALVADOR("SV", "El Salvador", "엘살바도르"),
    NICARAGUA("NI", "Nicaragua", "니카라과"),
    PERU("PE", "Peru", "페루"),
    BOLIVIA("BO", "Bolivia", "볼리비아"),
    ECUADOR("EC", "Ecuador", "에콰도르"),
    VENEZUELA("VE", "Venezuela", "베네수엘라"),
    INDONESIA("ID", "Indonesia", "인도네시아"),
    VIETNAM("VN", "Vietnam", "베트남"),
    THAILAND("TH", "Thailand", "태국"),
    LAOS("LA", "Laos", "라오스"),
    MYANMAR("MM", "Myanmar", "미얀마"),
    CHINA("CN", "China", "중국"),
    TAIWAN("TW", "Taiwan", "대만"),
    INDIA("IN", "India", "인도"),
    PHILIPPINES("PH", "Philippines", "필리핀"),
    PAPUA_NEW_GUINEA("PG", "Papua New Guinea", "파푸아뉴기니"),
    NEPAL("NP", "Nepal", "네팔"),
    YEMEN("YE", "Yemen", "예멘"),
    MEXICO("MX", "Mexico", "멕시코"),
    TANZANIA("TZ", "Tanzania", "탄자니아"),
    MALAWI("MW", "Malawi", "말라위"),
    ZAMBIA("ZM", "Zambia", "잠비아"),
    ZIMBABWE("ZW", "Zimbabwe", "짐바브웨"),
    CAMEROON("CM", "Cameroon", "카메룬"),
    ANGOLA("AO", "Angola", "앙골라"),
    MADAGASCAR("MG", "Madagascar", "마다가스카르"),
    DEMOCRATIC_REPUBLIC_OF_THE_CONGO("CD", "Democratic Republic of the Congo", "콩고민주공화국"),
    DOMINICAN_REPUBLIC("DO", "Dominican Republic", "도미니카공화국"),
    JAMAICA("JM", "Jamaica", "자메이카"),
    CUBA("CU", "Cuba", "쿠바"),
    HAITI("HT", "Haiti", "아이티");

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
