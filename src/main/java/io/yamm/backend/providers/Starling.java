package io.yamm.backend.providers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.yamm.backend.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
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
    private Map<String, UUID> transactionRefs = new HashMap<>();
    private CachedValue<LinkedHashMap<UUID, Transaction>, ZonedDateTime> transactions;
    private final UUID uuid;
    private YAMM yamm;

    public static final String name = "Starling Bank";
    public static final String[] requiredCredentials = new String[] {"personal access token"};

    public Starling(char[][] credentials, YAMM yamm) throws RemoteException {
        accessToken = Arrays.copyOf(credentials[0], credentials[0].length);
        uuid = UUID.randomUUID();
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
        this.uuid = uuid;
        this.yamm = yamm;

        // add transactions
        LinkedHashMap<UUID, Transaction> transactionsLHM = new LinkedHashMap<>();
        for (int i = transactions.length - 1; i >= 0; i--) {
            transactionsLHM.put(transactions[i].id, transactions[i]);
            transactionRefs.put(transactions[i].providerId, transactions[i].id);
        }
        this.transactions = new CachedValue<>(transactionsLHM,
                Instant.ofEpochSecond(0).atZone(ZoneId.of("UTC")));
    }

    private void callAccountEndpoint() throws RemoteException {
        JSONObject json = callEndpoint("v1/accounts").getBody().getObject();
        nickname = json.getString("name");
        accountNumber = json.getString("accountNumber");
        sortCode = json.getString("sortCode");
        iban = json.getString("iban");
        bic = json.getString("bic");
    }

    private void callBalanceEndpoint() throws RemoteException {
        JSONObject json = callEndpoint("v1/accounts/balance").getBody().getObject();

        // set currency
        currency = Currency.getInstance(json.getString("currency"));

        // set available balance
        availableToSpend = new CachedValue<>(
                new Long(new DecimalFormat("0.00").format(
                        json.getBigDecimal("availableToSpend")).replace(".", "")));

        // set balance
        balance = new CachedValue<>(
                new Long(new DecimalFormat("0.00").format(
                        json.getBigDecimal("clearedBalance")).replace(".", "")));
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
            throw new RemoteException("Starling API failure: status code " + json.getStatus() + " for endpoint " +
                    "https://api.starlingbank.com/api/" + endpoint + ".");
        }
    }

    private void callTransactionEndpoint() throws RemoteException {
        try {
            // TODO: improve this to handle transactions which take a while to settle
            Transaction newestTransaction = transactions.value.entrySet().iterator().next().getValue();
            callTransactionEndpoint(newestTransaction.created.minusDays(21), ZonedDateTime.now());
        } catch (NoSuchElementException|NullPointerException e) {
            // by default, get transactions from the epoch to now
            callTransactionEndpoint(Instant.ofEpochSecond(0).atZone(ZoneId.of("UTC")), ZonedDateTime.now());
        }
    }

    private void callTransactionEndpoint(ZonedDateTime from, ZonedDateTime to) throws RemoteException {
        String fromStr = from.getYear() + "-" + String.format("%02d", from.getMonthValue()) + "-" + String.format("%02d", from.getDayOfMonth());
        String toStr = to.getYear() + "-" + String.format("%02d", to.getMonthValue()) + "-" + String.format("%02d", to.getDayOfMonth());
        JSONObject json = callEndpoint("v1/transactions?from=" + fromStr + "&to=" + toStr).getBody().getObject();

        LinkedHashMap<UUID, Transaction> transactions;
        try {
            transactions = this.transactions.value;
        } catch (NullPointerException e) {
            transactions = new LinkedHashMap<>();
        }

        JSONArray jsonTransactions = json.getJSONObject("_embedded").getJSONArray("transactions");
        // iterate through transactions backwards (i.e. oldest first)
        for (int i = jsonTransactions.length() - 1; i >= 0; --i) {
            JSONObject jsonTransaction = jsonTransactions.getJSONObject(i);

            // try to find an existing transaction
            String providerId = jsonTransaction.getString("id");
            UUID id = transactionRefs.get(providerId);
            if (id == null) {
                id = UUID.randomUUID();
            } else {
                // if the transaction already exists, skip it
                continue;
            }

            TransactionCategory category = TransactionCategory.GENERAL;
            Counterparty counterparty = null;
            DeclineReason declineReason = null;
            Long localAmount = null;
            Currency localCurrency = null;
            String mcc = null;
            ZonedDateTime settled = null;
            TransactionType type = TransactionType.UNKNOWN;

            Long amount = new Long(new DecimalFormat("0.00").format(
                    jsonTransaction.getBigDecimal("amount")).replace(".", ""));
            Long balance = new Long(new DecimalFormat("0.00").format(
                    jsonTransaction.getBigDecimal("balance")).replace(".", ""));
            ZonedDateTime created = ZonedDateTime.parse(jsonTransaction.getString("created"));
            String description = jsonTransaction.getString("narrative");

            switch (jsonTransaction.getString("source")) {
                case "DIRECT_CREDIT":
                    type = TransactionType.BACS;
                    break;

                case "DIRECT_DEBIT":
                    type = TransactionType.DIRECT_DEBIT;
                    break;

                case "DIRECT_DEBIT_DISPUTE":
                    type = TransactionType.DIRECT_DEBIT;
                    break;

                case "INTERNAL_TRANSFER":
                    type = TransactionType.TRANSFER;
                    break;

                case "MASTER_CARD":
                    type = TransactionType.CARD;

                    JSONObject cardJSON = callEndpoint("v1/transactions/mastercard/" + providerId).getBody().getObject();

                    // settled status
                    if (cardJSON.getString("status").equals("SETTLED")) {
                        settled = ZonedDateTime.parse("1970-01-01T00:00:00.000Z");
                    } else {
                        // skip pending transactions; see https://github.com/BenjaminEHowe/yamm-client/issues/6
                        continue;
                    }

                    switch(cardJSON.getString("mastercardTransactionMethod")) {
                        case "CONTACTLESS":
                            type = TransactionType.CARD_CONTACTLESS;
                            break;
                        case "MAGNETIC_STRIP":
                            type = TransactionType.CARD_MAGSTRIPE;
                            break;
                        case "MANUAL_KEY_ENTRY":
                            type = TransactionType.CARD_MANUAL;
                            break;
                        case "CHIP_AND_PIN":
                            type = TransactionType.CARD_PIN;
                            break;
                        case "ONLINE":
                            type = TransactionType.CARD_ONLINE;
                            break;
                        case "ATM":
                            type = TransactionType.CARD_CASH;
                            break;
                        case "APPLE_PAY":
                            type = TransactionType.MOBILE_APPLE;
                            break;
                        case "ANDROID_PAY":
                            type = TransactionType.MOBILE_ANDROID;
                            break;
                    }

                    // show foreign currency info, if the transaction was in a foreign currency
                    Currency sourceCurrency = Currency.getInstance(cardJSON.getString("sourceCurrency"));
                    if (sourceCurrency != currency) {
                        localCurrency = sourceCurrency;
                        int decimalPlaces = localCurrency.getDefaultFractionDigits();
                        if (decimalPlaces != 0) {
                            StringBuilder patternBuilder = new StringBuilder("0.");
                            for (int j = 0; j < decimalPlaces; j++) {
                                patternBuilder.append("0");
                            }
                            localAmount = new Long(new DecimalFormat(patternBuilder.toString()).format(
                                    cardJSON.getBigDecimal("sourceAmount")).replace(".", ""));
                        } else {
                            localAmount = cardJSON.getLong("sourceAmount");
                        }
                    }

                    // merchant (counterparty)
                    if (cardJSON.has("merchantId")) {
                        JSONObject merchantJSON = callEndpoint("v1/merchants/" +
                                cardJSON.getString("merchantId")).getBody().getObject();

                        Address address = null;
                        URL icon = null;
                        String merchantName = null;
                        URL website = null;

                        // merchant address
                        if (cardJSON.has("merchantLocationId")) {
                            JSONObject locationJSON = callEndpoint("v1/merchants/" +
                                    cardJSON.getString("merchantId") + "/locations/" +
                                    cardJSON.getString("merchantLocationId")).getBody().getObject();

                            if (locationJSON.has("mastercardMerchantCategoryCode")) {
                                mcc = String.valueOf(locationJSON.getInt("mastercardMerchantCategoryCode"));
                            }

                            // this is where we would fill the address in, but at the moment Starling only provices
                            // a Google Places ID. This is unhelpful!
                        }

                        if (merchantJSON.has("twitterUsername")) {
                            try {
                                String twitterUsername = merchantJSON.getString("twitterUsername").replace("@", "");
                                icon = new URL("https://twitter.com/" + twitterUsername + "/profile_image");
                            } catch (MalformedURLException e) {
                                // TODO: handle this better
                                icon = null;
                            }
                        }
                        if (merchantJSON.has("name")) {
                            merchantName = merchantJSON.getString("name");
                        }
                        if (merchantJSON.has("website")) {
                            try {
                                website = new URL(merchantJSON.getString("website"));
                            } catch (MalformedURLException e) {
                                // TODO: handle this better
                                website = null;
                            }
                        }

                        counterparty = new Counterparty(
                                null,
                                address,
                                icon,
                                merchantName,
                                null,
                                website
                        );
                    }

                    // category
                    if (cardJSON.has("spendingCategory")) {
                        switch(cardJSON.getString("spendingCategory")) {
                            case "BILLS_AND_SERVICES":
                                category = TransactionCategory.BILLS_AND_SERVICES;
                                break;
                            case "EATING_OUT":
                                category = TransactionCategory.EATING_OUT;
                                break;
                            case "ENTERTAINMENT":
                                category = TransactionCategory.ENTERTAINMENT;
                                break;
                            case "EXPENSES":
                                category = TransactionCategory.EXPENSES;
                                break;
                            case "GENERAL":
                                category = TransactionCategory.GENERAL;
                                break;
                            case "GROCERIES":
                                category = TransactionCategory.GROCERIES;
                                break;
                            case "SHOPPING":
                                category = TransactionCategory.SHOPPING;
                                break;
                            case "HOLIDAYS":
                                category = TransactionCategory.ENTERTAINMENT;
                                break;
                            case "PAYMENTS":
                                category = TransactionCategory.GENERAL;
                                break;
                            case "TRANSPORT":
                                category = TransactionCategory.TRANSPORT;
                                break;
                            case "LIFESTYLE":
                                category = TransactionCategory.ENTERTAINMENT;
                                break;
                            case "NONE":
                                category = TransactionCategory.GENERAL;
                                break;
                        }
                    }

                    break;

                case "FASTER_PAYMENTS_IN":
                    type = TransactionType.FASTER_PAYMENT;
                    settled = created; // settles instantly
                    break;

                case "FASTER_PAYMENTS_OUT":
                    type = TransactionType.FASTER_PAYMENT;
                    settled = created; // settles instantly
                    break;

                case "FASTER_PAYMENTS_REVERSAL":
                    type = TransactionType.FASTER_PAYMENT;
                    settled = created; // settles instantly
                    break;

                case "STRIPE_FUNDING":
                    type = TransactionType.PAYMENT;
                    settled = created; // settles instantly
                    break;

                case "INTEREST_PAYMENT":
                    type = TransactionType.INTEREST;
                    settled = created; // settles instantly
                    break;

                case "NOSTRO_DEPOSIT":
                    type = TransactionType.SWIFT;
                    // TODO: verify that the below is true
                    settled = created; // settles instantly
                    break;

                case "OVERDRAFT":
                    type = TransactionType.TRANSFER;
                    settled = created; // settles instantly
                    break;
            }

            Transaction transaction = new Transaction(
                    amount,
                    balance,
                    category,
                    counterparty,
                    created,
                    declineReason,
                    description,
                    id,
                    localAmount,
                    localCurrency,
                    mcc,
                    providerId,
                    settled,
                    null,
                    type
            );
            transactions.put(transaction.id, transaction);
            transactionRefs.put(transaction.providerId, transaction.id);
        }

        this.transactions = new CachedValue<>(transactions);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Long getAvailableToSpend() throws RemoteException {
        try {
            // cache for 60 seconds
            if (ChronoUnit.SECONDS.between(availableToSpend.updated, ZonedDateTime.now()) < 60) {
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
            if (ChronoUnit.SECONDS.between(balance.updated, ZonedDateTime.now()) < 60) {
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

    private char[][] getCredentials() {
        return new char[][] {accessToken};
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
            if (ChronoUnit.SECONDS.between(transactions.updated, ZonedDateTime.now()) < 60) {
                return transactions.value.values().toArray(new Transaction[transactions.value.size()]);
            } else {
                callTransactionEndpoint();
                return transactions.value.values().toArray(new Transaction[transactions.value.size()]);
            }
        } catch (NullPointerException e) {
            callTransactionEndpoint();
            return transactions.value.values().toArray(new Transaction[transactions.value.size()]);
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public void overwriteSensitiveData() {
        accessToken = yamm.generateSecureRandom(accessToken.length);
    }

    public void setNickname(String newNickname) {
        nickname = newNickname;
    }
}
