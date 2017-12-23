package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Topman extends NewDay {
    public static final String name = "Topman Card";

    public NewDay_Topman(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "topman";
    }
}
