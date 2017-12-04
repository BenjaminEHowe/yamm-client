package io.yamm.backend;

import java.rmi.RemoteException;
import java.util.Currency;
import java.util.UUID;

public interface Account {
    Long getAvailableToSpend() throws RemoteException;
    Long getBalance() throws RemoteException;
    Currency getCurrency() throws RemoteException;
    String getNickname();
    Transaction[] getTransactions() throws RemoteException;
    UUID getUUID();
    void overwriteSensitiveData();
    void setNickname(String newNickname);
}
