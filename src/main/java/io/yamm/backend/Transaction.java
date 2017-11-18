package io.yamm.backend;

import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.UUID;

public class Transaction {
    public final Long amount;
    public final Long balance;
    public final TransactionCategory category;
    public final Counterparty counterparty;
    public final ZonedDateTime created;
    public final DeclineReason declineReason;
    public final String description;
    public final UUID id;
    public final Long localAmount;
    public final Currency localCurrency;
    public final String mcc; // ISO 18245 merchant category code
    public final String providerId;
    public final ZonedDateTime settled;
    public final TransactionType type;

    public Transaction(Long amount,
                       Long balance,
                       TransactionCategory category,
                       Counterparty counterparty,
                       ZonedDateTime created,
                       DeclineReason declineReason,
                       String description,
                       UUID id,
                       Long localAmount,
                       Currency localCurrency,
                       String mcc,
                       String providerId,
                       ZonedDateTime settled,
                       TransactionType type) {
        this.amount = amount;
        this.balance = balance;
        this.category = category;
        this.counterparty = counterparty;
        this.created = created;
        this.declineReason = declineReason;
        this.description = description;
        this.id = id;
        this.localAmount = localAmount;
        this.localCurrency = localCurrency;
        this.mcc = mcc;
        this.providerId = providerId;
        this.settled = settled;
        this.type = type;
    }
}
