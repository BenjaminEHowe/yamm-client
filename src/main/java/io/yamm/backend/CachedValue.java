package io.yamm.backend;

public class CachedValue<Object, Date> {
    public final Object value;
    public final Date updated;

    public CachedValue(Object value, Date updated) {
        this.value = value;
        this.updated = updated;
    }
}
