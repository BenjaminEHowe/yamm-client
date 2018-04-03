package io.yamm.backend;

import org.apache.http.HttpException;

import java.util.Currency;
import java.util.UUID;

public interface Account {
    Long getAvailableToSpend() throws YAMMRuntimeException;
    Long getBalance() throws YAMMRuntimeException;
    Currency getCurrency() throws YAMMRuntimeException;
    String getNickname() throws YAMMRuntimeException;
    Transaction[] getTransactions() throws YAMMRuntimeException;
    UUID getUUID();
    void overwriteSensitiveData();
    void setNickname(String newNickname);
}
