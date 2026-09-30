package com.automation.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for JSON request bodies, keeping step definitions free of raw map construction.
 */
public final class PayloadBuilder {

    private final Map<String, Object> payload = new LinkedHashMap<>();

    private PayloadBuilder() {
    }

    public static PayloadBuilder newPayload() {
        return new PayloadBuilder();
    }

    public PayloadBuilder with(String field, Object value) {
        payload.put(field, value);
        return this;
    }

    public Map<String, Object> build() {
        return payload;
    }
}
