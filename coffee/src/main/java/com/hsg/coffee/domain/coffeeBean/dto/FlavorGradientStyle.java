package com.hsg.coffee.domain.coffeeBean.dto;

import java.util.ArrayList;
import java.util.Collection;
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

        if (colors.isEmpty()) {
            return "background: " + DEFAULT_COLOR + ";";
        }
        if (colors.size() == 1) {
            return "background: " + colors.getFirst() + ";";
        }
        return "background: linear-gradient(90deg, " + String.join(", ", colors) + ");";
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
}
