package com.automation.utils;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe (ThreadLocal-backed) scenario context. Cucumber may execute scenarios on
 * different threads; storing the last response and any shared scenario data here avoids
 * cross-scenario leakage during parallel execution.
 */
public final class PayloadStore {

    private static final ThreadLocal<Map<String, Object>> STORE = ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Response> RESPONSE = new ThreadLocal<>();

    private PayloadStore() {
    }

    public static void put(String key, Object value) {
        STORE.get().put(key, value);
    }

    public static Object get(String key) {
        return STORE.get().get(key);
    }

    public static void setResponse(Response response) {
        RESPONSE.set(response);
    }

    public static Response getResponse() {
        return RESPONSE.get();
    }

    public static void clear() {
        STORE.get().clear();
        RESPONSE.remove();
    }
}
