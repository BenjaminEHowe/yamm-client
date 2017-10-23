package io.yamm.backend.providers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.yamm.backend.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.rmi.RemoteException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class Starling implements BankAccount {
    private char[] accessToken;
    private String accountNumber;
    private CachedValue<Long, ZonedDateTime> availableToSpend;
    private CachedValue<Long, ZonedDateTime> balance;
    private String bic;
    private Currency currency;
    private String iban;
    private String nickname = "";
    private String sortCode;
    private CachedValue<List<Transaction>, ZonedDateTime> transactions;
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
                    String bic,
                    Currency currency,
                    String iban,
                    String nickname,
                    String sortCode,
                    Transaction[] transactions,
                    UUID uuid,
                    YAMM yamm) {
        accessToken = Arrays.copyOf(credentials[0], credentials[0].length);
        this.accountNumber = accountNumber;
        this.bic = bic;
        this.currency = currency;
        this.iban = iban;
        this.nickname = nickname;
        this.sortCode = sortCode;
        // transactions: set the cache expiry to the epoch in order to force (partial) refresh
        this.transactions = new CachedValue<>(new ArrayList<>(Arrays.asList(transactions)),
                Instant.ofEpochSecond(0).atZone(ZoneId.of("UTC")));
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

        // set available balance
        availableToSpend = new CachedValue<>(YAMM.currencyInMinorUnits(currency, json.getBigDecimal("availableToSpend")));

        // set balance
        balance = new CachedValue<>(YAMM.currencyInMinorUnits(currency, json.getBigDecimal("clearedBalance")));
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

    private void callTransactionEndpoint() throws RemoteException {
        try {
            // TODO: improve this to handle transactions which take a while to settle
            Transaction newestTransaction = transactions.value.get(transactions.value.size() - 1);
            callTransactionEndpoint(newestTransaction.created, ZonedDateTime.now());
        } catch (IndexOutOfBoundsException|NullPointerException e) {
            // by default, get transactions from the epoch to now
            callTransactionEndpoint(Instant.ofEpochSecond(0).atZone(ZoneId.of("UTC")), ZonedDateTime.now());
        }
    }

    private void callTransactionEndpoint(ZonedDateTime from, ZonedDateTime to) throws RemoteException {
        String fromStr = from.getYear() + "-" + String.format("%02d", from.getMonthValue()) + "-" + String.format("%02d", from.getDayOfMonth());
        String toStr = to.getYear() + "-" + String.format("%02d", to.getMonthValue()) + "-" + String.format("%02d", to.getDayOfMonth());
        JSONObject json = callEndpoint("v1/transactions?from=" + fromStr + "&to=" + toStr).getBody().getObject();

        List<Transaction> transactions;
        try {
            transactions = this.transactions.value;
        } catch (NullPointerException e) {
            transactions = new ArrayList<>();
        }

        JSONArray jsonTransactions = json.getJSONObject("_embedded").getJSONArray("transactions");
        // iterate through transactions backwards (i.e. oldest first)
        for (int i = jsonTransactions.length() - 1; i >= 0; --i) {
            JSONObject jsonTransaction = jsonTransactions.getJSONObject(i);
            UUID id = UUID.fromString(jsonTransaction.getString("id"));
            Currency currency = Currency.getInstance(jsonTransaction.getString("currency"));
            Long amount = YAMM.currencyInMinorUnits(currency, jsonTransaction.getBigDecimal("amount"));
            Long balance = YAMM.currencyInMinorUnits(currency, jsonTransaction.getBigDecimal("balance"));
            ZonedDateTime created = ZonedDateTime.parse(jsonTransaction.getString("created"));
            String description = jsonTransaction.getString("narrative");

            transactions.add(new Transaction(
                    amount,
                    balance,
                    created,
                    currency,
                    description,
                    id
            ));
        }

        this.transactions = new CachedValue<>(transactions);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Long getAvailableToSpend() throws RemoteException {
        try {
            // cache for 60 seconds
            if (ChronoUnit.SECONDS.between(ZonedDateTime.now(), availableToSpend.updated) < 60) {
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
            if (ChronoUnit.SECONDS.between(ZonedDateTime.now(), balance.updated) < 60) {
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

    public Transaction[] getTransactions() throws RemoteException {
        try {
            // cache for 60 seconds
            if (ChronoUnit.SECONDS.between(ZonedDateTime.now(), transactions.updated) < 60) {
                return transactions.value.toArray(new Transaction[transactions.value.size()]);
            } else {
                callTransactionEndpoint();
                return transactions.value.toArray(new Transaction[transactions.value.size()]);
            }
        } catch (NullPointerException e) {
            callTransactionEndpoint();
            return transactions.value.toArray(new Transaction[transactions.value.size()]);
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public void overwriteSensitiveData() {
        accessToken = yamm.generateSecureRandom(accessToken.length);
    }
}
