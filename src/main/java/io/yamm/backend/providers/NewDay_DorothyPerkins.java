package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_DorothyPerkins extends NewDay {
    public static final String name = "Dorothy Perkins Mastercard";

    public NewDay_DorothyPerkins(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "dorothyperkins";
    }
}
