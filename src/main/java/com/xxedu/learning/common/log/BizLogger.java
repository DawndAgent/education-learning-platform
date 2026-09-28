package com.xxedu.learning.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BizLogger {

    private static final Logger log = LoggerFactory.getLogger("BIZ");

    private BizLogger() {
    }

    public static void info(String action, String pattern, Object... args) {
        if (pattern == null || pattern.isEmpty()) {
            log.info("action={}", action);
            return;
        }
        Object[] values = new Object[args.length + 1];
        values[0] = action;
        System.arraycopy(args, 0, values, 1, args.length);
        log.info("action={} " + pattern, values);
    }

    public static void warn(String action, String pattern, Object... args) {
        if (pattern == null || pattern.isEmpty()) {
            log.warn("action={}", action);
            return;
        }
        Object[] values = new Object[args.length + 1];
        values[0] = action;
        System.arraycopy(args, 0, values, 1, args.length);
        log.warn("action={} " + pattern, values);
    }
}
