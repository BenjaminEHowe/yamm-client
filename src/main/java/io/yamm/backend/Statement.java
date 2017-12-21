package io.yamm.backend;

import java.util.Date;
import java.util.UUID;

public class Statement {
    public final Long balance;
    public final Date due;
    public final UUID id;
    public final Date issued;
    public final Long minimumPayment;
    public final Long previousBalance;

    public Statement(Long balance,
                     Date due,
                     UUID id,
                     Date issued,
                     Long minimumPayment,
                     Long previousBalance) {
        this.balance = balance;
        this.due = due;
        this.id = id;
        this.issued = issued;
        this.minimumPayment = minimumPayment;
        this.previousBalance = previousBalance;
    }
}
