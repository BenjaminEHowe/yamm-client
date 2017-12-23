package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Evans extends NewDay {
    public static final String name = "Evans Mastercard";

    public NewDay_Evans(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "evans";
    }
}
