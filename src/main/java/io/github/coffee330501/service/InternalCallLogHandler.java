package io.github.coffee330501.service;

import java.util.HashMap;
import java.util.Map;

public abstract class InternalCallLogHandler{
    public abstract void log(Map<String, Object> map);

    public void log(LogBuilder logBuilder) {
        Map<String, Object> map = logBuilder.getMap();
        log(map);
    }

    public static LogBuilder createLogBuilder() {
        return new LogBuilder();
    }

    public static class LogBuilder {
        private final Map<String, Object> map = new HashMap<>();

        public LogBuilder add(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public Map<String, Object> getMap() {
            return map;
        }
    }
}
