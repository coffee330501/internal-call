package io.github.coffee330501.utils;

import java.util.Map;

public class InternalCallContext {
    private static final ThreadLocal<Map<String, String>> CONTEXT = new ThreadLocal<>();

    public synchronized static void set(Map<String, String> value) {
        CONTEXT.set(value);
    }

    public static Map<String, String> get() {
        return CONTEXT.get();
    }
}
