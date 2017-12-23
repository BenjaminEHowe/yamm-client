package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Outfit extends NewDay {
    public static final String name = "Outfit Mastercard";

    public NewDay_Outfit(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "outfit";
    }
}
