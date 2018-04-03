package io.yamm.backend.providers;

import io.yamm.backend.Statement;
import io.yamm.backend.Transaction;
import io.yamm.backend.YAMM;
import io.yamm.backend.YAMMRuntimeException;

import java.util.Currency;
import java.util.UUID;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Topshop extends NewDay {
    public static final String name = "Topshop Card";

    public NewDay_Topshop(char[][] credentials, YAMM yamm) throws YAMMRuntimeException {
        super(credentials, yamm);
    }

    public NewDay_Topshop(char[][] credentials,
                          Currency currency,
                          String nickname,
                          Statement[] statements,
                          Transaction[] transactions,
                          UUID uuid,
                          YAMM yamm) throws YAMMRuntimeException {
        super(credentials, currency, nickname, statements, transactions, uuid, yamm);
    }

    protected String getSlug() {
        return "topshop";
    }
}
