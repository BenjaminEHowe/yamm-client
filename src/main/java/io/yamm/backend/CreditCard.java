package io.yamm.backend;

import java.rmi.RemoteException;
import java.util.Date;

public interface CreditCard extends Account {
    Long getCreditLimit() throws RemoteException;
    Date getNextStatementDate() throws RemoteException;
    Statement[] getStatements() throws RemoteException;
}
