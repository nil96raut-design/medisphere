package com.healthtrack.util;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class DateUtils {

    private DateUtils() {}

    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
