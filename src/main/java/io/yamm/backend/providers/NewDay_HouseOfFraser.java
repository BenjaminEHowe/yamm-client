package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_HouseOfFraser extends NewDay {
    public static final String name = "House of Fraser Recognition Mastercard";

    public NewDay_HouseOfFraser(char[][] credentials, YAMM yamm) {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "houseoffraser";
    }
}
