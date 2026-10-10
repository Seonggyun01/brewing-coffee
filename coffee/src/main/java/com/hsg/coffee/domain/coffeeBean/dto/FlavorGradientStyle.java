package com.hsg.coffee.domain.coffeeBean.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;

public final class FlavorGradientStyle {

    private static final String DEFAULT_COLOR = "#DED4C8";
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private FlavorGradientStyle() {
    }

    public static String from(
            Collection<FlavorNote> flavorNotes,
            Collection<CustomFlavorNoteResponse> customFlavorNotes
    ) {
        List<String> colors = new ArrayList<>();
        if (flavorNotes != null) {
            flavorNotes.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(FlavorNote::getColor)
                    .map(FlavorGradientStyle::validColorOrNull)
                    .filter(java.util.Objects::nonNull)
                    .forEach(colors::add);
        }
        if (customFlavorNotes != null) {
            customFlavorNotes.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(CustomFlavorNoteResponse::color)
                    .map(FlavorGradientStyle::validColorOrNull)
                    .filter(java.util.Objects::nonNull)
                    .forEach(colors::add);
        }

        colors = colors.stream().distinct().toList();

        if (colors.isEmpty()) {
            return "background: " + DEFAULT_COLOR + ";";
        }
        if (colors.size() == 1) {
            return "background: " + colors.getFirst() + ";";
        }
        return "background: linear-gradient(90deg, "
                + String.join(", ", sortByRgbProximity(colors))
                + ");";
    }

    public static String normalizeColor(String color) {
        String normalizedColor = validColorOrNull(color);
        return normalizedColor != null ? normalizedColor : DEFAULT_COLOR;
    }

    private static String validColorOrNull(String color) {
        if (color == null) {
            return null;
        }
        String normalizedColor = color.trim().toUpperCase();
        return HEX_COLOR.matcher(normalizedColor).matches() ? normalizedColor : null;
    }

    private static List<String> sortByRgbProximity(List<String> colors) {
        List<RgbColor> remaining = colors.stream()
                .map(RgbColor::from)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<String> sorted = new ArrayList<>(remaining.size());

        RgbColor current = remaining.stream()
                .min(Comparator.comparingInt(RgbColor::packedValue))
                .orElseThrow();
        remaining.remove(current);
        sorted.add(current.hex());

        while (!remaining.isEmpty()) {
            RgbColor previous = current;
            current = remaining.stream()
                    .min(Comparator
                            .comparingLong((RgbColor color) -> previous.distanceSquared(color))
                            .thenComparingInt(RgbColor::packedValue))
                    .orElseThrow();
            remaining.remove(current);
            sorted.add(current.hex());
        }
        return sorted;
    }

    private record RgbColor(String hex, int red, int green, int blue) {

        private static RgbColor from(String hex) {
            return new RgbColor(
                    hex,
                    Integer.parseInt(hex.substring(1, 3), 16),
                    Integer.parseInt(hex.substring(3, 5), 16),
                    Integer.parseInt(hex.substring(5, 7), 16)
            );
        }

        private int packedValue() {
            return (red << 16) | (green << 8) | blue;
        }

        private long distanceSquared(RgbColor other) {
            long redDifference = red - other.red;
            long greenDifference = green - other.green;
            long blueDifference = blue - other.blue;
            return redDifference * redDifference
                    + greenDifference * greenDifference
                    + blueDifference * blueDifference;
        }
    }
}
