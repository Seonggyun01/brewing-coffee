package com.hsg.coffee.global.logging;

import java.time.Instant;
import java.util.LinkedHashMap;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

/** One JSON object per stage, only while an extraction request is in progress. */
public final class ExtractionTrace {
    public static final String REQUEST_ID = "extractionId";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("coffee.extraction.trace");
    private static final ObjectMapper JSON = new ObjectMapper();
    private ExtractionTrace() {}

    public static void record(String stage, Object value) {
        String id = MDC.get(REQUEST_ID);
        if (id == null) return;
        try {
            var event = new LinkedHashMap<String, Object>();
            event.put("time", Instant.now().toString());
            event.put("requestId", id);
            event.put("stage", stage);
            event.put("value", value);
            // No request headers, credentials, session or image bytes are ever passed here.
            String serialized = JSON.writeValueAsString(event)
                    .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer [REDACTED]")
                    .replaceAll("sk-[A-Za-z0-9_-]{16,}", "[REDACTED]");
            LOG.info("{}", serialized);
        } catch (Exception ignored) {
            LOG.warn("Trace serialization failed: stage={}, requestId={}", stage, id);
        }
    }
}
