package com.hsg.coffee.domain.coffeeBean.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hsg.coffee.domain.coffeeBean.dto.CustomFlavorNoteResponse;
import com.hsg.coffee.domain.coffeeBean.dto.FlavorGradientStyle;
import com.hsg.coffee.domain.coffeeBean.entity.CustomFlavorNote;
import com.hsg.coffee.domain.coffeeBean.repository.CustomFlavorNoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomFlavorNoteService {

    private final CustomFlavorNoteRepository customFlavorNoteRepository;
    private final CustomFlavorNoteColorRecommender colorRecommender;

    @Transactional
    public List<String> ensureAll(Collection<String> names) {
        List<String> cleanedNames = cleanNames(names);
        Map<String, CustomFlavorNote> existing = new LinkedHashMap<>();
        List<String> pending = new java.util.ArrayList<>();
        for (String name : cleanedNames) {
            var note = customFlavorNoteRepository.findByNormalizedName(normalize(name));
            note.ifPresent(value -> existing.put(name, value));
            if (note.isEmpty() || FlavorNoteColorFallbackPalette.isGenericDefault(note.get().getColor())) {
                pending.add(name);
            }
        }
        if (pending.isEmpty()) return cleanedNames;
        Map<String, String> recommended = colorRecommender.recommendColors(pending);
        for (String name : pending) {
            String color = FlavorGradientStyle.normalizeColor(recommended.getOrDefault(name,
                    FlavorNoteColorFallbackPalette.resolve(name)));
            CustomFlavorNote note = existing.get(name);
            if (note == null) {
                customFlavorNoteRepository.save(CustomFlavorNote.of(name, normalize(name), color));
            } else if (!FlavorNoteColorFallbackPalette.isGenericDefault(color)) {
                note.updateColor(color);
            }
        }
        return cleanedNames;
    }

    public List<CustomFlavorNoteResponse> findDetails(Collection<String> names) {
        List<String> cleanedNames = cleanNames(names);
        if (cleanedNames.isEmpty()) {
            return List.of();
        }

        Map<String, CustomFlavorNote> notesByNormalizedName = customFlavorNoteRepository.findByNormalizedNameIn(
                        cleanedNames.stream()
                                .map(this::normalize)
                                .toList()
                )
                .stream()
                .collect(
                        LinkedHashMap::new,
                        (map, note) -> map.put(note.getNormalizedName(), note),
                        LinkedHashMap::putAll
                );

        return cleanedNames.stream()
                .map(name -> {
                    CustomFlavorNote note = notesByNormalizedName.get(normalize(name));
                    String color = FlavorGradientStyle.normalizeColor(
                            FlavorNoteColorFallbackPalette.replaceGenericDefault(
                                    name,
                                    note != null ? note.getColor() : null
                            )
                    );
                    return new CustomFlavorNoteResponse(name, color);
                })
                .toList();
    }

    private List<String> cleanNames(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }

        Set<String> cleanedNames = new LinkedHashSet<>();
        Set<String> normalizedNames = new LinkedHashSet<>();
        for (String name : names) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            String cleanedName = name.trim().replaceAll("\\s+", " ");
            if (!cleanedName.isBlank() && normalizedNames.add(normalize(cleanedName))) {
                cleanedNames.add(cleanedName);
            }
        }
        return cleanedNames.stream().limit(12).toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "").toLowerCase();
    }

}
