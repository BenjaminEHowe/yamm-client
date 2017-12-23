package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Amazon extends NewDay {
    public static final String name = "Amazon MasterCard";

    public NewDay_Amazon(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "amazon";
    }
}
