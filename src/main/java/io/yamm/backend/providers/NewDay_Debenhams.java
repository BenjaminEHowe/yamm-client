package io.yamm.backend.providers;

import io.yamm.backend.Transaction;
import io.yamm.backend.YAMM;
import io.yamm.backend.YAMMRuntimeException;

import java.util.Currency;
import java.util.UUID;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Debenhams extends NewDay {
    public static final String name = "Debenhams Credit Card";

    public NewDay_Debenhams(char[][] credentials, YAMM yamm) throws YAMMRuntimeException {
        super(credentials, yamm);
    }

    public NewDay_Debenhams(char[][] credentials,
                            Currency currency,
                            String nickname,
                            Transaction[] transactions,
                            UUID uuid,
                            YAMM yamm) throws YAMMRuntimeException {
        super(credentials, currency, nickname, transactions, uuid, yamm);
    }

    protected String getName() {
        return "Debenhams Credit Card";
    }

    protected String getSlug() {
        return "debenhams";
    }
}
