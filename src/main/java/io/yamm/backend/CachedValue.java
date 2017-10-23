package io.yamm.backend;

import java.time.ZonedDateTime;

public class CachedValue<Object, ZonedDateTime> {
    public final Object value;
    public final ZonedDateTime updated;

    public CachedValue(Object value) {
        this(value, (ZonedDateTime) java.time.ZonedDateTime.now());
    }

    public CachedValue(Object value, ZonedDateTime updated) {
        this.value = value;
        this.updated = updated;
    }
}
