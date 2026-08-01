package com.hsg.coffee.domain.coffeeBean.entity;

import com.hsg.coffee.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "custom_flavor_notes",
        uniqueConstraints = @UniqueConstraint(name = "uk_custom_flavor_notes_normalized_name", columnNames = "normalized_name")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomFlavorNote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 80)
    private String normalizedName;

    @Column(nullable = false, length = 7)
    private String color;

    public static CustomFlavorNote of(String name, String normalizedName, String color) {
        CustomFlavorNote note = new CustomFlavorNote();
        note.name = name;
        note.normalizedName = normalizedName;
        note.color = color;
        return note;
    }
}
