package com.hsg.coffee.domain.coffeeBean.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class OcrMetadataEvidenceTest {
    @Test void rejectsBackgroundMenuPricesAndUnlabeledDates() {
        String text = "FREE\n6,800\n5,000\nCOLOMBIA\nZARZA\nPACAMARA\nite. 26. 10. 2\n100g";
        assertThat(OcrMetadataEvidence.price(text, 6800)).isNull();
        assertThat(OcrMetadataEvidence.roastedDate(text, LocalDate.of(2026, 10, 2))).isNull();
    }

    @Test void keepsExactLabeledValuesOnSameOrNextLine() {
        assertThat(OcrMetadataEvidence.price("원두 가격: 18,000원", 18000)).isEqualTo(18000);
        assertThat(OcrMetadataEvidence.price("PRICE\n₩18,000", 18000)).isEqualTo(18000);
        assertThat(OcrMetadataEvidence.roastedDate("Roasted 2026.05.01", LocalDate.of(2026, 5, 1)))
                .isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(OcrMetadataEvidence.roastedDate("로스팅일\n26. 5. 1", LocalDate.of(2026, 5, 1)))
                .isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test void rejectsWrongValuesEvenWhenLabelExistsElsewhere() {
        assertThat(OcrMetadataEvidence.price("PRICE: 18,000원\n6,800", 6800)).isNull();
        assertThat(OcrMetadataEvidence.roastedDate("Roasted 2026.05.01\n2026.10.02", LocalDate.of(2026, 10, 2)))
                .isNull();
        assertThat(OcrMetadataEvidence.price("가격: 16800", 6800)).isNull();
    }
}
