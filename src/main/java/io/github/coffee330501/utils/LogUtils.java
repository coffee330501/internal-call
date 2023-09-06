package io.github.coffee330501.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogUtils {
    private LogUtils() {
    }

    private static String formatMsg(String level, Object... args) {
        try {
            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
            String className = stackTraceElement.getClassName();
            String methodName = stackTraceElement.getMethodName();
            String str = String.format("%n------------------INTERNAL-CALL-" + level + "------------------%nClass: %s%nMethod: %s%n", className, methodName);
            if (args != null) {
                StringBuilder builder = new StringBuilder(str);
                builder.append("Args: \n");
                for (Object arg : args) {
                    builder.append(arg.toString()).append("\n");
                }
                str = builder.toString();
            }
            return str;
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }

    public static void error(Throwable e, Object... args) {
        String errorMsg = LogUtils.formatMsg("ERROR", args);
        log.error(errorMsg, e);
    }

    public static void error(Object... args) {
        String errorMsg = LogUtils.formatMsg("ERROR", args);
        log.error(errorMsg);
    }

    public static void info(Object... args) {
        String msg = LogUtils.formatMsg("INFO", args);
        log.error(msg);
    }
}
