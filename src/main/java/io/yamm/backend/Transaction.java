package io.yamm.backend;

import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.UUID;

public class Transaction {
    public final Long amount;
    public final Long balance;
    public final ZonedDateTime created;
    public final Currency currency;
    public final String description;
    public final UUID id;
    // TODO: add type, counterparty, settlement

    public Transaction(Long amount,
                       Long balance,
                       ZonedDateTime created,
                       Currency currency,
                       String description,
                       UUID id) {
        this.amount = amount;
        this.balance = balance;
        this.created = created;
        this.currency = currency;
        this.description = description;
        this.id = id;
    }
}
