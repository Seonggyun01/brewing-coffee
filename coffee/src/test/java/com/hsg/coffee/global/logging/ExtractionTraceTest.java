package com.hsg.coffee.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.ObjectMapper;
import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanCardExtractionService;
import com.hsg.coffee.domain.llmparsing.service.BeanMappingService;
import com.hsg.coffee.domain.llmparsing.dto.LlmParsingResponse;

@SpringBootTest
class ExtractionTraceTest {
    @Autowired CoffeeBeanCardExtractionService extraction;
    @MockitoBean BeanMappingService mapping;

    @Test void correlatesStagesAndRestoresRequestContext() {
        when(mapping.parseOcrText(anyString())).thenReturn(new LlmParsingResponse("Ethiopia Yirgacheffe Kochere", "Fritz Coffee", "", "", "", "", "", "", "", "", "", "", "", java.util.List.of()));
        Logger logger = (Logger) LoggerFactory.getLogger("coffee.extraction.trace");
        var capture = new ListAppender<ILoggingEvent>();
        capture.start(); logger.addAppender(capture);
        try {
            extraction.extract(new MockMultipartFile("image", "trace-test.png", "image/png", new byte[]{1}));
            var json = new ObjectMapper();
            var events = capture.list.stream().map(e -> json.readTree(e.getFormattedMessage())).toList();
            assertThat(events.stream().map(e -> e.path("stage").asText())).contains("ocr.result", "llm.result", "ocr.fallback.result", "form.result", "request.complete");
            assertThat(events.stream().map(e -> e.path("requestId").asText()).distinct().count()).isEqualTo(1);
            assertThat(MDC.get(ExtractionTrace.REQUEST_ID)).isNull();
            assertThatThrownBy(() -> extraction.extract(new MockMultipartFile("image", new byte[]{}))).isInstanceOf(IllegalArgumentException.class);
            assertThat(capture.list.getLast().getFormattedMessage()).contains("request.failed");
            assertThat(MDC.get(ExtractionTrace.REQUEST_ID)).isNull();
        } finally { logger.detachAppender(capture); capture.stop(); }
    }
}
