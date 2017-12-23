package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Wallis extends NewDay {
    public static final String name = "Wallis Mastercard";

    public NewDay_Wallis(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "wallis";
    }
}
