package io.yamm.backend.providers;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.neovisionaries.i18n.CountryCode;
import io.yamm.backend.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public abstract class NewDay implements CreditCard {
    private CachedValue<Long> availableToSpend;
    private CachedValue<Long> balance;
    private String bearerToken;
    private CachedValue<Long> creditLimit;
    private String decimalFormatPattern;
    private CachedValue<Date> nextStatementDate;
    private String nickname;
    private char[] passcode;
    private char[] password;
    private CachedValue<TransactionStore> transactions = new CachedValue<>(new TransactionStore(), true);
    private char[] username;
    private final UUID uuid;
    private final YAMM yamm;

    @SuppressWarnings("unused") // accessed via reflection
    public static final String[] requiredCredentials = new String[] {"username", "password", "passcode"};

    public NewDay(char[][] credentials, YAMM yamm) throws YAMMRuntimeException {
        this.username = Arrays.copyOf(credentials[0], credentials[0].length);
        this.password = Arrays.copyOf(credentials[1], credentials[1].length);
        this.passcode = Arrays.copyOf(credentials[2], credentials[2].length);
        this.uuid = UUID.randomUUID();
        this.yamm = yamm;
        authenticate();
    }

    public NewDay(char[][] credentials,
                  @SuppressWarnings("unused") Currency currency, // TODO: for now, ignore currency (but this should change in the future!)
                  String nickname,
                  Transaction[] transactions,
                  UUID uuid,
                  YAMM yamm) throws YAMMRuntimeException {
        this.username = Arrays.copyOf(credentials[0], credentials[0].length);
        this.password = Arrays.copyOf(credentials[1], credentials[1].length);
        this.passcode = Arrays.copyOf(credentials[2], credentials[2].length);

        this.nickname = nickname;

        TransactionStore transactionStore = new TransactionStore();
        for (int i = transactions.length - 1; i >= 0; i--) {
            transactionStore.add(transactions[i]);
        }
        this.transactions = new CachedValue<>(transactionStore);

        this.uuid = uuid;
        this.yamm = yamm;

        authenticate();
    }

    protected abstract String getSlug();

    protected abstract String getName();

    private void authenticate() throws YAMMRuntimeException {
        // check username looks sensible (minimum length 6)
        if (username.length < 6) {
            throw new YAMMRuntimeException("Invalid username (6 characters or more required)");
        }

        // check password looks sensible (minimum length 6)
        if (password.length < 6) {
            throw new YAMMRuntimeException("Invalid password (6 characters or more required)");
        }

        // check passcode looks sensible (length 6, all digits)
        {
            boolean failed = false;

            // check length
            if (passcode.length != 6) {
                failed = true;
            }

            // check each character is a digit
            if (!failed) {
                for (int i = 0; i < 6; i++) {
                    if (!Character.isDigit(passcode[i])) {
                        failed = true;
                        break;
                    }
                }
            }

            // show error if required
            if (failed) {
                throw new YAMMRuntimeException("Invalid passcode (6 digits required)");
            }
        }

        // get anti forgery cookie
        try {
            Unirest.get("https://portal.newdaycards.com/" + getSlug() + "/login").asString();
        } catch (UnirestException e) {
            // TODO: handle this better (connection timeouts etc.)
            throw new YAMMRuntimeException("UnirestException", e);
        }

        // build the body for getting the passcode challenge
        JSONObject getPasscodeChallengeBody = new JSONObject();
        getPasscodeChallengeBody.put("username", new String(username));
        getPasscodeChallengeBody.put("password", new String(password));

        // get the passcode challenge
        JSONObject passcodeChallenge;
        try {
            passcodeChallenge = Unirest.post("https://portal.newdaycards.com/authentication/getPasscodeChallenge")
                    .header("Content-Type", "application/json")
                    .header("Referer", "https://portal.newdaycards.com/" + getSlug() + "/login")
                    .header("x-xsrf-token", yamm.getCookie("XSRF-TOKEN", "portal.newdaycards.com"))
                    .body(getPasscodeChallengeBody)
                    .asJson()
                    .getBody()
                    .getObject();
        } catch (UnirestException e) {
            // TODO: handle this better (connection timeouts etc.)
            throw new YAMMRuntimeException("UnirestException", e);
        }

        // decode the identity cookie and obtain the bearer token
        String identityJson;
        try {
            identityJson = URLDecoder.decode(yamm.getCookie("Identity.Cookie", "portal.newdaycards.com"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new YAMMRuntimeException("utf-8 not supported!");
        }
        String partAuthToken = new JSONObject(identityJson).getJSONObject("PartAuthToken").getString("Token");

        // build the passcodeDigits JSON
        JSONArray passcodeDigits = passcodeChallenge.getJSONObject("challenge").getJSONArray("passcodeDigits");
        for (int i = 0; i < passcodeDigits.length(); i++) {
            JSONObject passcodeDigit = passcodeDigits.getJSONObject(i);
            passcodeDigit.put("digit", Character.getNumericValue(passcode[passcodeDigit.getInt("position")]));
            passcodeDigits.put(i, passcodeDigit);
        }

        // build the body for the passcode challenge
        JSONObject submitPasscodeChallengeBody = new JSONObject();
        submitPasscodeChallengeBody.put("passcodeDigits", passcodeDigits);
        submitPasscodeChallengeBody.put("challengeVerificationToken", passcodeChallenge.getString("challengeVerificationToken"));
        submitPasscodeChallengeBody.put("username", new String(username));
        submitPasscodeChallengeBody.put("password", new String(password));
        submitPasscodeChallengeBody.put("rememberMe", false);

        // submit the passcode challenge
        JSONObject passcodeChallengeResponse;
        try {
            passcodeChallengeResponse = Unirest.post("https://portal.newdaycards.com/authentication/submitPasscodeChallenge")
                    .header("Authorization", "Bearer " + partAuthToken)
                    .header("Content-Type", "application/json")
                    .header("Referer", "https://portal.newdaycards.com/" + getSlug() + "/login")
                    .header("x-xsrf-token", yamm.getCookie("XSRF-TOKEN", "portal.newdaycards.com"))
                    .body(submitPasscodeChallengeBody)
                    .asJson()
                    .getBody()
                    .getObject();
        } catch (UnirestException e) {
            // TODO: handle this better (connection timeouts etc.)
            throw new YAMMRuntimeException("UnirestException", e);
        }

        // check for success, store identity token
        if (passcodeChallengeResponse.has("success") && passcodeChallengeResponse.getBoolean("success")) {
            try {
                identityJson = URLDecoder.decode(yamm.getCookie("Identity.Cookie", "portal.newdaycards.com"), "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new YAMMRuntimeException("utf-8 not supported!");
            }
            bearerToken = new JSONObject(identityJson).getJSONObject("IdentityToken").getString("Token");
        } else {
            throw new YAMMRuntimeException("NewDay authentication failed (stage 2)");
        }
    }

    private void callAccountSummaryEndpoint() throws YAMMRuntimeException {
        reauthenticateIfRequired();

        // fetch data
        JSONObject json;
        try {
            json = Unirest.get("https://portal.newdaycards.com/api/AccountSummary/AccountSummary")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Referer", "https://portal.newdaycards.com/" + getSlug() + "/login")
                    .asJson()
                    .getBody()
                    .getObject();
        } catch (UnirestException e) {
            throw new YAMMRuntimeException("UnirestException", e);
        }

        // set available to spend
        availableToSpend = new CachedValue<>(new Long(new DecimalFormat(getDecimalFormatPattern()).format(
                json.getBigDecimal("availableToSpend")).replace(".", "")));

        // set balance
        balance = new CachedValue<>(-1 * new Long(new DecimalFormat(getDecimalFormatPattern()).format(
                json.getBigDecimal("currentBalance")).replace(".", "")));

        // set credit limit
        Long creditLimitMajorUnits = json.getLong("creditLimit");
        StringBuilder creditLimitStr = new StringBuilder(creditLimitMajorUnits.toString());
        for (int i = 0; i < getCurrency().getDefaultFractionDigits(); i++) {
            creditLimitStr.append("0");
        }
        creditLimit = new CachedValue<>(new Long(creditLimitStr.toString()));

        // set next statement date
        try {
            nextStatementDate = new CachedValue<>(
                    new SimpleDateFormat("yyyy-MM-dd").parse(
                            json.getString("nextStatementDate").substring(0, 9)));
        } catch (ParseException e) {
            throw new YAMMRuntimeException("Error parsing next statement date provided by account summary endpoint!", e);
        }

        // set nickname
        nickname = json.getJSONObject("productDetails").getString("description");
    }

    private void callTransactionsEndpoint() throws YAMMRuntimeException {
        reauthenticateIfRequired();

        // declare a few variables
        TransactionStore transactionStore = transactions.value;
        JSONArray json;

        // get data from NewDay
        try {
            json = Unirest.get("https://portal.newdaycards.com/api/statements/01-01-1970/31-12-2099/B/transactions")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Referer", "https://portal.newdaycards.com/" + getSlug() + "/account-summary")
                    .asJson()
                    .getBody()
                    .getArray();
        } catch (UnirestException e) {
            throw new YAMMRuntimeException("UnirestException", e);
        }

        // instantiate each transaction from JSON, add it to the transaction store
        for (Object transactionObj : json) {
            JSONObject transactionJson = (JSONObject) transactionObj;

            // debug
            System.out.println(transactionJson.getString("description"));

            // skip pending transactions (they have no reference so cannot be tracked)
            if (transactionJson.getString("category").equals("pending")) {
                continue;
            }

            // skip transactions which are already stored
            if (transactionStore.get(transactionJson.getString("referenceNbr")) != null) {
                continue;
            }

            // convert amount, name, country, and created / posted dates
            long amount = -1 * new Long(new DecimalFormat(getDecimalFormatPattern()).format(
                    transactionJson.getBigDecimal("amount")).replace(".", ""));
            String name;
            CountryCode country = null;
            if (transactionJson.getString("description").length() == 40) { // if the description is a 40-character Mastercard description
                name = transactionJson.getString("description").substring(0, 23).trim();
                country = CountryCode.getByCode(transactionJson.getString("description").substring(37, 40));
            } else if (transactionJson.getString("description").length() == 39) { // if the description is a 39-character (i.e. US) Mastercard description
                name = transactionJson.getString("description").substring(0, 23).trim();
                country = CountryCode.getByCode("USA");
            } else {
                name = getName();
            }
            ZonedDateTime created = ZonedDateTime.parse(transactionJson.getString("effectiveDate"));
            ZonedDateTime settled = ZonedDateTime.parse(transactionJson.getString("postDate"));

            // instantiate the transaction
            Transaction transaction = new Transaction(
                    amount,
                    0L,
                    TransactionCategory.UNKNOWN,
                    new Counterparty(
                            null,
                            new Address(
                                    null,
                                    null,
                                    country,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            ),
                            null,
                            name,
                            null,
                            null
                    ),
                    created,
                    null,
                    transactionJson.getString("description"),
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    transactionJson.getString("referenceNbr"),
                    settled,
                    null,
                    TransactionType.UNKNOWN
            );

            // if the transaction doesn't already exist in the transaction store, add it
            transactionStore.add(transaction);
        }

        // set the transaction store
        transactions = new CachedValue<>(transactionStore);

        // debug
        System.out.println("First: " + transactions.value.first().description);
        System.out.println("Last: " + transactions.value.last().description);
    }

    public Long getAvailableToSpend() throws YAMMRuntimeException {
        try {
            // cache for 5 minutes
            if (ChronoUnit.SECONDS.between(availableToSpend.updated, ZonedDateTime.now()) < 300) {
                return availableToSpend.value;
            } else {
                callAccountSummaryEndpoint();
                return availableToSpend.value;
            }
        } catch (NullPointerException e) {
            callAccountSummaryEndpoint();
            return availableToSpend.value;
        }
    }

    public Long getBalance() throws YAMMRuntimeException {
        try {
            // cache for 5 minutes
            if (ChronoUnit.SECONDS.between(balance.updated, ZonedDateTime.now()) < 300) {
                return balance.value;
            } else {
                callAccountSummaryEndpoint();
                return balance.value;
            }
        } catch (NullPointerException e) {
            callAccountSummaryEndpoint();
            return balance.value;
        }
    }

    @SuppressWarnings("unused") // accessed via reflection
    private char[][] getCredentials() {
        return new char[][] {username, password, passcode};
    }

    public Long getCreditLimit() throws YAMMRuntimeException {
        try {
            // cache for 1 hour
            if (ChronoUnit.SECONDS.between(creditLimit.updated, ZonedDateTime.now()) < 3600) {
                return creditLimit.value;
            } else {
                callAccountSummaryEndpoint();
                return creditLimit.value;
            }
        } catch (NullPointerException e) {
            callAccountSummaryEndpoint();
            return creditLimit.value;
        }
    }

    public Currency getCurrency() {
        return Currency.getInstance("GBP"); // it appears that all NewDay cards are billed in pounds sterling
    }

    private String getDecimalFormatPattern() {
        try {
            //noinspection ResultOfMethodCallIgnored
            decimalFormatPattern.getClass(); // will trigger NPE if applicable
        } catch (NullPointerException e) {
            // if the decimal format pattern hasn't yet been determined, determine it!
            StringBuilder decimalFormatPatternBuilder = new StringBuilder("0");
            for (int i = 0; i < getCurrency().getDefaultFractionDigits(); i++) {
                if (i == 0) {
                    decimalFormatPatternBuilder.append(".");
                }
                decimalFormatPatternBuilder.append("0");
            }
            decimalFormatPattern = decimalFormatPatternBuilder.toString();
        }
        return decimalFormatPattern;
    }

    public Date getNextStatementDate() throws YAMMRuntimeException {
        try {
            // cache for 12 hours
            if (ChronoUnit.SECONDS.between(nextStatementDate.updated, ZonedDateTime.now()) < 43200) {
                return nextStatementDate.value;
            } else {
                callAccountSummaryEndpoint();
                return nextStatementDate.value;
            }
        } catch (NullPointerException e) {
            callAccountSummaryEndpoint();
            return nextStatementDate.value;
        }
    }

    public String getNickname() throws YAMMRuntimeException {
        if (nickname == null) {
            callAccountSummaryEndpoint();
        }
        return nickname;
    }

    public Transaction getTransaction(UUID id) throws YAMMRuntimeException {
        try {
            // cache for 5 minutes
            if (ChronoUnit.SECONDS.between(transactions.updated, ZonedDateTime.now()) < 300) {
                return transactions.value.get(id);
            } else {
                callTransactionsEndpoint();
                return transactions.value.get(id);
            }
        } catch (NullPointerException e) {
            callTransactionsEndpoint();
            return transactions.value.get(id);
        }
    }

    public Transaction[] getTransactions() throws YAMMRuntimeException {
        try {
            // cache for 5 minutes
            if (ChronoUnit.SECONDS.between(transactions.updated, ZonedDateTime.now()) < 300) {
                return transactions.value.toArray();
            } else {
                callTransactionsEndpoint();
                return transactions.value.toArray();
            }
        } catch (NullPointerException e) {
            callTransactionsEndpoint();
            return transactions.value.toArray();
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public void overwriteSensitiveData() {
        passcode = yamm.generateSecureRandom(passcode.length);
        password = yamm.generateSecureRandom(password.length);
        username = yamm.generateSecureRandom(username.length);
    }

    private void reauthenticateIfRequired() throws YAMMRuntimeException {
        boolean authenticated;
        try {
            authenticated = Unirest.get("https://portal.newdaycards.com/authentication/IsAuthenticated")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Referer", "https://portal.newdaycards.com/" + getSlug() + "/login")
                    .asJson()
                    .getBody()
                    .getObject()
                    .getBoolean("isAuthenticated");
        } catch (UnirestException e) {
            // TODO: handle this better (connection timeouts etc.)
            throw new YAMMRuntimeException("UnirestException", e);
        }

        if (!authenticated) {
            authenticate();
        }
    }

    public void setNickname(String newNickname) {
        nickname = newNickname;
    }
}
