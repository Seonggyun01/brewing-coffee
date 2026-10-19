package com.hsg.coffee.domain.tasteSpace.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.brewRecord.entity.BrewRecord;
import com.hsg.coffee.domain.brewRecord.repository.BrewRecordRepository;
import com.hsg.coffee.domain.coffeeBean.dto.CustomFlavorNoteResponse;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.coffeeBean.service.CustomFlavorNoteService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TasteSpaceDocumentBuilder {

    private final CoffeeBeanRepository coffeeBeanRepository;
    private final BrewRecordRepository brewRecordRepository;
    private final CustomFlavorNoteService customFlavorNoteService;

    @Transactional(readOnly = true)
    public List<TasteSpaceDocument> build(Long ownerId) {
        List<CoffeeBean> beans = coffeeBeanRepository.findByOwnerIdOrderByIdDesc(ownerId);
        Map<Long, List<BrewRecord>> recordsByBean = new LinkedHashMap<>();
        for (BrewRecord record : brewRecordRepository.findByCoffeeBeanOwnerIdOrderByBrewedDateDescIdDesc(ownerId)) {
            recordsByBean.computeIfAbsent(record.getCoffeeBean().getId(), ignored -> new ArrayList<>()).add(record);
        }

        return beans.stream()
                .sorted(Comparator.comparing(CoffeeBean::getId))
                .map(bean -> buildDocument(bean, recordsByBean.getOrDefault(bean.getId(), List.of())))
                .toList();
    }

    private TasteSpaceDocument buildDocument(CoffeeBean bean, List<BrewRecord> records) {
        List<CustomFlavorNoteResponse> customNotes = customFlavorNoteService.findDetails(bean.getCustomFlavorNotes());
        List<String> flavorNames = new ArrayList<>(bean.getFlavorNotes().stream().map(note -> note.getDisplayName()).toList());
        flavorNames.addAll(customNotes.stream().map(CustomFlavorNoteResponse::name).toList());
        List<String> colors = new ArrayList<>(bean.getFlavorNotes().stream().map(note -> note.getColor()).toList());
        colors.addAll(customNotes.stream().map(CustomFlavorNoteResponse::color).toList());
        if (colors.isEmpty()) {
            colors.add("#8A9A8C");
        }

        Double averageRating = records.stream()
                .map(BrewRecord::getRating)
                .filter(value -> value != null)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);

        String color = colors.getFirst();

        StringBuilder text = new StringBuilder();
        append(text, "원두", bean.getName());
        append(text, "로스터리", bean.getRoastery());
        append(text, "국가", bean.getCountry());
        append(text, "지역", bean.getRegion());
        append(text, "농장", bean.getFarm());
        append(text, "품종", bean.getVariety());
        append(text, "가공", bean.getProcessType() != null ? bean.getProcessType().name() : null);
        append(text, "향미", String.join(", ", flavorNames));
        append(text, "메모", bean.getMemo());
        if (!records.isEmpty()) {
            append(text, "평균 만족도", averageRating != null ? String.format("%.1f", averageRating) : null);
            append(text, "브루잉 경험", records.stream()
                    .limit(5)
                    .map(record -> String.join(" ",
                            nullable(record.getMemo()),
                            record.getFeelingTags().stream().map(tag -> tag.name()).toList().toString(),
                            record.getCustomFeelingTags().toString()))
                    .toList()
                    .toString());
        }

        String value = text.toString();
        return new TasteSpaceDocument(
                bean,
                sha256(value),
                value,
                color,
                List.copyOf(colors),
                List.copyOf(flavorNames),
                averageRating,
                records.size()
        );
    }

    private void append(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(label).append(": ").append(value.trim()).append("\n");
        }
    }

    private String nullable(String value) {
        return value == null ? "" : value;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
