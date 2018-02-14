package io.yamm.backend;

import org.apache.http.HttpException;

import java.util.Currency;
import java.util.UUID;

public interface Account {
    Long getAvailableToSpend() throws HttpException;
    Long getBalance() throws HttpException;
    Currency getCurrency() throws HttpException;
    String getNickname() throws HttpException;
    Transaction[] getTransactions() throws HttpException;
    UUID getUUID();
    void overwriteSensitiveData();
    void setNickname(String newNickname);
}
