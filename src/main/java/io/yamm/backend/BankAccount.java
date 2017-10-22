package io.yamm.backend;

public interface BankAccount extends Account {
    String getAccountNumber();
    String getBIC();
    String getIBAN();
    String getSortCode();
}
