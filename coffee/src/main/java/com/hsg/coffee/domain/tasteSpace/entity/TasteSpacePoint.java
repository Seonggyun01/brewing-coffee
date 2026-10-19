package com.hsg.coffee.domain.tasteSpace.entity;

import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBean;
import com.hsg.coffee.domain.user.entity.AppUser;
import com.hsg.coffee.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "taste_space_points",
        uniqueConstraints = @UniqueConstraint(name = "uk_taste_space_owner_bean", columnNames = {"owner_id", "coffee_bean_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TasteSpacePoint extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coffee_bean_id", nullable = false)
    private CoffeeBean coffeeBean;

    @Column(nullable = false, length = 100)
    private String modelId;

    @Column(nullable = false, length = 64)
    private String sourceHash;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String embeddingJson;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double z;

    public static TasteSpacePoint create(
            AppUser owner,
            CoffeeBean coffeeBean,
            String modelId,
            String sourceHash,
            String embeddingJson,
            double x,
            double y,
            double z
    ) {
        TasteSpacePoint point = new TasteSpacePoint();
        point.owner = owner;
        point.coffeeBean = coffeeBean;
        point.modelId = modelId;
        point.sourceHash = sourceHash;
        point.embeddingJson = embeddingJson;
        point.x = x;
        point.y = y;
        point.z = z;
        return point;
    }
}
