package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_MissSelfridge extends NewDay {
    public static final String name = "Miss Selfridge Account Card";

    public NewDay_MissSelfridge(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "missselfridge";
    }
}
