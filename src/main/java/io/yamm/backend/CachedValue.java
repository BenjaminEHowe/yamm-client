package io.yamm.backend;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Stores a generic value. Also stores when it was updated.
 * @author Benjamin Howe
 */
public class CachedValue<Object> {
    public final Object value;
    public final ZonedDateTime updated;

    /**
     * Store a value, set the update time to now.
     * @param value The value to store.
     */
    public CachedValue(Object value) {
        this(value, ZonedDateTime.now());
    }

    /**
     * Store a value. If {@code init} is true, the updated time is set to 1 Jan 1970 UTC.
     * @param value The value to store.
     * @param init True if the value is only being initialised, false otherwise.
     */
    public CachedValue(Object value, boolean init) {
        this.value = value;
        if (init) {
            this.updated = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        } else {
            this.updated = ZonedDateTime.now();
        }
    }

    /**
     * Store a value with a custom update time.
     * @param value The value to store.
     * @param updated The time when the value was updated.
     */
    public CachedValue(Object value, ZonedDateTime updated) {
        this.value = value;
        this.updated = updated;
    }
}
