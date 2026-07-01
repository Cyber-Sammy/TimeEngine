package com.time_engine.engine.util;

import com.mojang.logging.LogUtils;
import com.time_engine.engine.config.TimeEngineConfig;
import org.slf4j.Logger;

public final class ModLog {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ModLog() {}

    public static void diagnostic(String message, Object... arguments) {
        if (TimeEngineConfig.diagnosticLogging()) {
            LOGGER.info("[Time Engine] " + message, arguments);
        }
    }

    public static void warn(String message, Object... arguments) {
        LOGGER.warn("[Time Engine] " + message, arguments);
    }

    public static void error(String message, Object... arguments) {
        LOGGER.error("[Time Engine] " + message, arguments);
    }
}
