package io.yamm.backend;

import org.apache.http.HttpException;

import java.util.Date;

public interface CreditCard extends Account {
    Long getCreditLimit() throws HttpException;
    Date getNextStatementDate() throws HttpException;
    Statement[] getStatements() throws HttpException;
}
