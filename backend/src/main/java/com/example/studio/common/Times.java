package com.example.studio.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Times {

    private Times() {
    }

    /** 与 DATETIME(3) 精度一致，保证写入值与读回值相等。 */
    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
