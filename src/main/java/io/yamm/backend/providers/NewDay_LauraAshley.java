package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_LauraAshley extends NewDay {
    public static final String name = "Laura Ashley Mastercard";

    public NewDay_LauraAshley(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "lauraashley";
    }
}
