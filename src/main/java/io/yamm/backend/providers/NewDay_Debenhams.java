package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Debenhams extends NewDay {
    public static final String name = "Debenhams Credit Card";

    public NewDay_Debenhams(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "debenhams";
    }
}
