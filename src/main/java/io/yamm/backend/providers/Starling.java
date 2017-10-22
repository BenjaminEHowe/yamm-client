package io.yamm.backend.providers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.yamm.backend.*;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.*;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class Starling implements BankAccount {
    private char[] accessToken;
    private String accountNumber;
    private CachedValue<Long, Date> availableToSpend;
    private CachedValue<Long, Date> balance;
    private String bic;
    private Currency currency;
    private String iban;
    private String nickname = "";
    private String sortCode;
    private CachedValue<List<Transaction>, Date> transactions;
    private UUID uuid;
    private YAMM yamm;

    public static final String name = "Starling Bank";
    public static final String[] requiredCredentials = new String[] {"personal access token"};

    public Starling(char[][] credentials, YAMM yamm) throws RemoteException {
        accessToken = Arrays.copyOf(credentials[0], credentials[0].length);
        this.yamm = yamm;
        callAccountEndpoint();
    }

    public Starling(char[][] credentials,
                    String accountNumber,
                    CachedValue<Long, Date> availableToSpend,
                    CachedValue<Long, Date> balance,
                    String bic,
                    Currency currency,
                    String iban,
                    String nickname,
                    String sortCode,
                    CachedValue<List<Transaction>, Date> transactions,
                    UUID uuid,
                    YAMM yamm) {
        accessToken = Arrays.copyOf(credentials[0], credentials[0].length);
        this.accountNumber = accountNumber;
        this.availableToSpend = availableToSpend;
        this.balance = balance;
        this.bic = bic;
        this.currency = currency;
        this.iban = iban;
        this.nickname = nickname;
        this.sortCode = sortCode;
        this.transactions = transactions;
        this.uuid = uuid;
        this.yamm = yamm;
    }

    private void callAccountEndpoint() throws RemoteException {
        JSONObject json = callEndpoint("v1/accounts").getBody().getObject();
        nickname = json.getString("name");
        accountNumber = json.getString("accountNumber");
        sortCode = json.getString("sortCode");
        iban = json.getString("iban");
        bic = json.getString("bic");
        uuid = UUID.fromString(json.getString("id"));
    }

    private void callBalanceEndpoint() throws RemoteException {
        JSONObject json = callEndpoint("v1/accounts/balance").getBody().getObject();

        // set currency
        currency = Currency.getInstance(json.getString("currency"));

        BigDecimal multiplier = new BigDecimal (Math.pow(10, currency.getDefaultFractionDigits()));
        BigDecimal tmpBigDec;

        // set available balance
        tmpBigDec = json.getBigDecimal("availableToSpend");
        availableToSpend = new CachedValue<>(
                tmpBigDec.multiply(multiplier).longValue(),
                new Date());

        // set balance
        tmpBigDec = json.getBigDecimal("clearedBalance");
        balance = new CachedValue<>(
                tmpBigDec.multiply(multiplier).longValue(),
                new Date());
    }

    private HttpResponse<JsonNode> callEndpoint(String endpoint) throws RemoteException {
        // make request
        HttpResponse<JsonNode> json;
        try {
            json = Unirest.get("https://api.starlingbank.com/api/" + endpoint)
                    .header("Authorization", "Bearer " + new String(accessToken))
                    .asJson();
        } catch (UnirestException e) {
            // TODO: handle this better (connection timeouts etc.)
            throw new RemoteException("UnirestException", e);
        }

        // check status was 200 OK
        if (json.getStatus() == 200) {
            return json;
        } else {
            throw new RemoteException("Starling API failure: status code " + json.getStatus());
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Long getAvailableToSpend() throws RemoteException {
        try {
            // cache for 60 seconds
            if (YAMM.secondsBetween(new Date(), availableToSpend.updated) < 60) {
                return availableToSpend.value;
            } else {
                callBalanceEndpoint();
                return availableToSpend.value;
            }
        } catch (NullPointerException e) {
            callBalanceEndpoint();
            return availableToSpend.value;
        }
    }

    public Long getBalance() throws RemoteException {
        try {
            // cache for 60 seconds
            if (YAMM.secondsBetween(new Date(), balance.updated) < 60) {
                return balance.value;
            } else {
                callBalanceEndpoint();
                return balance.value;
            }
        } catch (NullPointerException e) {
            callBalanceEndpoint();
            return balance.value;
        }
    }

    public String getBIC() {
        return bic;
    }

    public Currency getCurrency() throws RemoteException {
        try {
            return currency;
        } catch (NullPointerException e) {
            callBalanceEndpoint();
            return currency;
        }
    }

    public String getIBAN() {
        return iban;
    }

    public String getNickname() {
        return nickname;
    }

    public String getSortCode() {
        return sortCode;
    }

    public Transaction[] getTransactions() throws RemoteException { // TODO: implement this
        throw new UnsupportedOperationException();
    }

    public UUID getUUID() {
        return uuid;
    }

    public void overwriteSensitiveData() {
        accessToken = yamm.generateSecureRandom(accessToken.length);
    }
}
