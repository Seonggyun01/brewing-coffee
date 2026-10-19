package com.hsg.coffee.domain.tasteSpace.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PcaProjectionServiceTest {

    private final PcaProjectionService projectionService = new PcaProjectionService();

    @Test
    void placesSingleBeanAtCenter() {
        double[][] coordinates = projectionService.project(List.of(new double[]{0.1, 0.2, 0.3}));

        assertThat(coordinates).hasDimensions(1, 3);
        assertThat(coordinates[0]).containsExactly(0.0, 0.0, 0.0);
    }

    @Test
    void projectsEmbeddingVectorsIntoNormalizedThreeDimensions() {
        double[][] coordinates = projectionService.project(List.of(
                new double[]{1.0, 0.0, 0.2, 0.1},
                new double[]{0.8, 0.1, 0.3, 0.0},
                new double[]{0.0, 1.0, 0.1, 0.4},
                new double[]{0.1, 0.8, 0.0, 0.5}
        ));

        assertThat(coordinates).hasDimensions(4, 3);
        for (double[] coordinate : coordinates) {
            for (double value : coordinate) {
                assertThat(Math.abs(value)).isLessThanOrEqualTo(1.0);
            }
        }
        assertThat(coordinates[0]).isNotEqualTo(coordinates[2]);
    }
}
