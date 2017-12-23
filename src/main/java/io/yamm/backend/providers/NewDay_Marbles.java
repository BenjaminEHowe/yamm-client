package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Marbles extends NewDay {
    public static final String name = "marbles";

    public NewDay_Marbles(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "marbles";
    }
}
