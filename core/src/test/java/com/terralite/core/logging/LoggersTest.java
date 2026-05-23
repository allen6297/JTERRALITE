package com.terralite.core.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggersTest {
    @Test
    void createsNamedSlf4jLoggers() {
        Logger logger = Loggers.get("terralite.test");

        assertEquals("terralite.test", logger.getName());
    }
}
