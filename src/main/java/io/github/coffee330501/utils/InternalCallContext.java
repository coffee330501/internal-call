package io.github.coffee330501.utils;

import java.util.HashMap;
import java.util.Map;

public class InternalCallContext {
    private static final ThreadLocal<Map<String, String>> CONTEXT = new ThreadLocal<>();

    public synchronized static void set(String key, String value) {
        Map<String, String> map = CONTEXT.get();
        if (map == null) map = new HashMap<>();
        map.put(key, value);
        CONTEXT.set(map);
    }

    public static String get(String key) {
        Map<String, String> map = CONTEXT.get();
        return map == null ? null : map.get(key);
    }
}
