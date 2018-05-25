package io.yamm.backend.providers;

import io.yamm.backend.Transaction;
import io.yamm.backend.YAMM;
import io.yamm.backend.YAMMRuntimeException;

import java.util.Currency;
import java.util.UUID;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_HouseOfFraser extends NewDay {
    public static final String name = "House of Fraser Recognition Mastercard";

    public NewDay_HouseOfFraser(char[][] credentials, YAMM yamm) throws YAMMRuntimeException {
        super(credentials, yamm);
    }

    public NewDay_HouseOfFraser(char[][] credentials,
                                Currency currency,
                                String nickname,
                                Transaction[] transactions,
                                UUID uuid,
                                YAMM yamm) throws YAMMRuntimeException {
        super(credentials, currency, nickname, transactions, uuid, yamm);
    }

    protected String getName() {
        return "House of Fraser Recognition Mastercard";
    }

    protected String getSlug() {
        return "houseoffraser";
    }
}
