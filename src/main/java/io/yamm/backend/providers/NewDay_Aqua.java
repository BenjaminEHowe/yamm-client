package io.yamm.backend.providers;

import io.yamm.backend.Statement;
import io.yamm.backend.Transaction;
import io.yamm.backend.YAMM;
import org.apache.http.HttpException;

import java.util.Currency;
import java.util.UUID;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Aqua extends NewDay {
    public static final String name = "Aqua";

    public NewDay_Aqua(char[][] credentials, YAMM yamm) throws HttpException {
        super(credentials, yamm);
    }

    public NewDay_Aqua(char[][] credentials,
                  Currency currency,
                  String nickname,
                  Statement[] statements,
                  Transaction[] transactions,
                  UUID uuid,
                  YAMM yamm) throws HttpException {
        super(credentials, currency, nickname, statements, transactions, uuid, yamm);
    }

    protected String getSlug() {
        return "aqua";
    }
}
