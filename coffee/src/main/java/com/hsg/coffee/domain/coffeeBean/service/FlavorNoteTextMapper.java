package com.hsg.coffee.domain.coffeeBean.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;

public final class FlavorNoteTextMapper {

    private static final double FUZZY_MATCH_THRESHOLD = 0.78;
    private static final Map<String, FlavorNote> ALIASES = createAliases();

    private FlavorNoteTextMapper() {
    }

    public static List<FlavorNote> findFlavorNotes(String text) {
        String normalizedText = normalize(text);
        if (!StringUtils.hasText(normalizedText)) {
            return List.of();
        }

        Set<FlavorNote> notes = new LinkedHashSet<>();
        FlavorNote aliasNote = ALIASES.get(normalizedText);
        if (aliasNote != null) {
            notes.add(aliasNote);
        }

        for (FlavorNote note : FlavorNote.values()) {
            String normalizedDisplayName = normalize(note.getDisplayName());
            String normalizedEnumName = normalize(note.name());
            if (containsKeyword(normalizedText, normalizedDisplayName)
                    || containsKeyword(normalizedText, normalizedEnumName)) {
                notes.add(note);
            }
        }

        if (notes.isEmpty()) {
            FlavorNote closestNote = findClosestNote(normalizedText);
            if (closestNote != null) {
                notes.add(closestNote);
            }
        }

        removeSubsumed(notes);
        return new ArrayList<>(notes);
    }

    public static boolean isKnownFlavorNote(String text, List<FlavorNote> matchedNotes) {
        String normalizedText = normalize(text);
        if (!StringUtils.hasText(normalizedText)) {
            return false;
        }

        for (FlavorNote note : matchedNotes) {
            if (isSameOrClose(normalizedText, note)) {
                return true;
            }
        }

        return findFlavorNotes(text).stream().anyMatch(matchedNotes::contains);
    }

    private static boolean isSameOrClose(String normalizedText, FlavorNote note) {
        String normalizedDisplayName = normalize(note.getDisplayName());
        String normalizedEnumName = normalize(note.name());
        return containsKeyword(normalizedText, normalizedDisplayName)
                || containsKeyword(normalizedText, normalizedEnumName)
                || normalizedText.equals(normalize(note.name()))
                || normalizedText.equals(normalizedDisplayName);
    }

    private static boolean containsKeyword(String normalizedText, String keyword) {
        return StringUtils.hasText(keyword)
                && normalizedText.contains(keyword);
    }

    private static FlavorNote findClosestNote(String normalizedText) {
        FlavorNote closestNote = null;
        double closestScore = 0;

        for (FlavorNote note : FlavorNote.values()) {
            double displayScore = similarity(normalizedText, normalize(note.getDisplayName()));
            double enumScore = similarity(normalizedText, normalize(note.name()));
            double score = Math.max(displayScore, enumScore);
            if (score > closestScore) {
                closestScore = score;
                closestNote = note;
            }
        }

        return closestScore >= FUZZY_MATCH_THRESHOLD ? closestNote : null;
    }

    private static double similarity(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return 0;
        }
        if (left.length() < 3 || right.length() < 3) {
            return left.equals(right) ? 1 : 0;
        }

        int maxLength = Math.max(left.length(), right.length());
        return 1 - ((double) editDistance(left, right) / maxLength);
    }

    private static int editDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int i = 0; i <= right.length(); i++) {
            previous[i] = i;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int substitutionCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + substitutionCost
                );
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()];
    }

    private static void removeSubsumed(Set<FlavorNote> notes) {
        Set<FlavorNote> notesToRemove = new LinkedHashSet<>();

        for (FlavorNote note : notes) {
            String keyword = normalize(note.name());
            for (FlavorNote otherNote : notes) {
                if (note == otherNote) {
                    continue;
                }

                String otherKeyword = normalize(otherNote.name());
                if (otherKeyword.contains(keyword)) {
                    notesToRemove.add(note);
                    break;
                }
            }
        }

        notes.removeAll(notesToRemove);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_\\-·,/|()\\[\\]{}]+", "");
    }

    private static Map<String, FlavorNote> createAliases() {
        Map<String, FlavorNote> aliases = new LinkedHashMap<>();
        putAliases(aliases, FlavorNote.HONEY, "허니");
        putAliases(aliases, FlavorNote.BROWN_SUGAR, "브라운슈가", "brown sugar", "cane sugar", "원당");
        putAliases(aliases, FlavorNote.MILK_CHOCOLATE, "밀크초콜릿", "밀크초코", "밀크초콜렛", "milk chocolate");
        putAliases(aliases, FlavorNote.DARK_CHOCOLATE, "다크초콜릿", "다크초코", "다크초콜렛", "dark chocolate");
        putAliases(aliases, FlavorNote.CACAO, "카카오닙스", "cacao nibs");
        putAliases(aliases, FlavorNote.RED_WINE, "레드와인", "red wine");
        putAliases(aliases, FlavorNote.WHITE_WINE, "화이트와인", "white wine");
        putAliases(aliases, FlavorNote.GRAPE, "청포도", "white grape", "green grape");
        putAliases(aliases, FlavorNote.BLACKCURRANT, "블랙커런트", "블랙커렌트", "black currant", "blackcurrant", "cassis", "카시스");
        putAliases(aliases, FlavorNote.ORANGE, "오렌지주스", "오렌지쥬스", "오렌지껍질", "오렌지필", "orange peel", "orange zest");
        putAliases(aliases, FlavorNote.LEMON, "레몬껍질", "레몬필", "lemon peel", "lemon zest");
        putAliases(aliases, FlavorNote.LIME, "라임껍질", "라임필", "lime peel", "lime zest");
        putAliases(aliases, FlavorNote.WHITE_PEACH, "백복숭아", "white peach");
        putAliases(aliases, FlavorNote.YELLOW_PEACH, "황복숭아", "황도", "yellow peach");
        putAliases(aliases, FlavorNote.PLUM, "건자두", "prune");
        putAliases(aliases, FlavorNote.MANGO, "말린망고", "dried mango");
        putAliases(aliases, FlavorNote.ROASTED_NUTS, "견과", "견과류", "구운견과", "nutty", "nuts");
        putAliases(aliases, FlavorNote.JASMINE_TEA, "자스민티", "jasmine tea");
        putAliases(aliases, FlavorNote.EARL_GREY, "얼그레이티", "bergamot", "베르가못");
        putAliases(aliases, FlavorNote.BLACK_TEA, "블랙티", "black tea");
        putAliases(aliases, FlavorNote.GREEN_TEA, "그린티", "green tea");
        return aliases;
    }

    private static void putAliases(Map<String, FlavorNote> aliases, FlavorNote note, String... values) {
        for (String value : values) {
            aliases.put(normalize(value), note);
        }
    }
}
