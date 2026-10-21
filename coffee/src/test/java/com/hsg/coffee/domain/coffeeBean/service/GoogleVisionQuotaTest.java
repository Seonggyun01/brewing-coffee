package com.hsg.coffee.domain.coffeeBean.service;

import com.hsg.coffee.domain.apiusage.service.ApiLimitException;
import com.hsg.coffee.domain.apiusage.service.ApiUsageService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleVisionQuotaTest {
    @Test void rejectsBeforeCreatingGoogleClientWhenMonthlyQuotaIsZero(@TempDir Path dir) {
        var preprocessor = mock(CoffeeBeanCardImagePreprocessor.class);
        var image = new MockMultipartFile("image", "card.png", "image/png", new byte[]{1});
        when(preprocessor.prepare(image)).thenReturn(new PreparedCoffeeBeanCardImage(new byte[]{1}, "card.png", "image/png"));
        var client = new GoogleVisionCoffeeBeanCardOcrClient(preprocessor, "missing-credentials.json");
        var usage = new ApiUsageService(dir.resolve("usage.jsonl").toString(), "groq", "embedding", 200000, 50, 0);
        ReflectionTestUtils.setField(client, "apiUsage", usage);
        assertThatThrownBy(() -> client.extractText(image)).isInstanceOf(ApiLimitException.class)
                .hasMessageContaining("Google Vision");
    }
}
