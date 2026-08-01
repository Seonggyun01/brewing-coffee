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
        for (String name : cleanedNames) {
            String normalizedName = normalize(name);
            customFlavorNoteRepository.findByNormalizedName(normalizedName)
                    .orElseGet(() -> customFlavorNoteRepository.save(CustomFlavorNote.of(
                            name,
                            normalizedName,
                            colorRecommender.recommendColor(name)
                    )));
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
                    String color = note != null ? note.getColor() : "#7A5038";
                    return new CustomFlavorNoteResponse(name, color);
                })
                .toList();
    }

    private List<String> cleanNames(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }

        Set<String> cleanedNames = new LinkedHashSet<>();
        for (String name : names) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            String cleanedName = name.trim().replaceAll("\\s+", " ");
            if (!cleanedName.isBlank()) {
                cleanedNames.add(cleanedName);
            }
        }
        return cleanedNames.stream().limit(12).toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "").toLowerCase();
    }
}
