package io.yamm.backend.providers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.yamm.backend.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public abstract class NewDay implements CreditCard {
    private CachedValue<Long, ZonedDateTime> availableToSpend;
    private CachedValue<Long, ZonedDateTime> balance;
    private CachedValue<Long, ZonedDateTime> creditLimit;
    private String decimalFormatPattern;
    private CachedValue<Date, ZonedDateTime> nextStatementDate;
    private String nickname;
    private char[] passcode;
    private char[] password;
    private CachedValue<LinkedHashMap<UUID, Statement>, ZonedDateTime> statements;
    private Map<String, UUID> transactionRefs = new HashMap<>();
    private CachedValue<LinkedHashMap<UUID, Transaction>, ZonedDateTime> transactions;
    private char[] username;
    private final UUID uuid;
    private final YAMM yamm;

    public static final String[] requiredCredentials = new String[] {"username", "password", "passcode"};

    public NewDay(char[][] credentials, YAMM yamm) {
        this.username = Arrays.copyOf(credentials[0], credentials[0].length);
        this.password = Arrays.copyOf(credentials[1], credentials[1].length);
        this.passcode = Arrays.copyOf(credentials[2], credentials[2].length);
        this.uuid = UUID.randomUUID();
        this.yamm = yamm;
    }

    protected abstract String getSlug();

    private void authenticate() throws RemoteException {
        try {
            // perform preauth w/ username and password
            JSONObject preauth = Unirest
                    .post("https://portal.newdaycards.com/accounts/services/rest/login/preauth?portalName=" + getSlug())
                    .body("{\"userId\":\"" + new String(username) + "\",\"passwd\":\"" + new String(password) + "\"}")
                    .asJson()
                    .getBody()
                    .getObject();
            if (preauth.has("errors")) {
                throw new RemoteException(
                        preauth.getJSONArray("errors").getJSONObject(0).getString("message"));
            }

            // determine requested passcode digits
            JSONArray requestedPasscodeIndexes = preauth.getJSONObject("response").getJSONArray("passcdDigits");
            char[] requestedPasscodeDigits = new char[requestedPasscodeIndexes.length() * 2 - 1];
            for (int i = 0; i < requestedPasscodeIndexes.length(); i++) {
                requestedPasscodeDigits[i * 2] = passcode[(int) requestedPasscodeIndexes.get(i) - 1];
                // place a bar between each digit
                if (i != requestedPasscodeIndexes.length() - 1) {
                    requestedPasscodeDigits[i * 2 + 1] = '|';
                }
            }

            // perform auth w/ requested passcode digits
            HttpResponse<JsonNode> fullAuth = Unirest
                    .post("https://portal.newdaycards.com/accounts/j_spring_security_check")
                    .header("accept", "application/json")
                    .field("j_password", new String(requestedPasscodeDigits))
                    .field("portalName", getSlug())
                    .asJson();
            JSONObject fullAuthBody = fullAuth.getBody().getObject();
            if (fullAuthBody.has("errors")) {
                throw new RemoteException(
                        fullAuthBody.getJSONArray("errors").getJSONObject(0).getString("message"));
            }
        } catch (UnirestException e) {
            // TODO: handle this better (connection timeouts etc.)
            throw new RemoteException("UnirestException", e);
        }
    }

    private JSONObject callEndpoint(String endpoint) throws RemoteException {
        return callEndpoint(endpoint, null, true);
    }

    private JSONObject callEndpoint(String endpoint, String body) throws RemoteException {
        return callEndpoint(endpoint, body, true);
    }

    private JSONObject callEndpoint(String endpoint, JSONObject body) throws RemoteException {
        return callEndpoint(endpoint, body.toString(), true);
    }

    private JSONObject callEndpoint(String endpoint, String body, boolean attemptReauthentication) throws RemoteException {
        try {
            // perform an appropriate HTTP request
            HttpResponse<JsonNode> request;
            if (body == null) {
                request = Unirest.post("https://portal.newdaycards.com/accounts/services/rest/" + endpoint)
                        .asJson();
            } else {
                request = Unirest.post("https://portal.newdaycards.com/accounts/services/rest/" + endpoint)
                        .body(body)
                        .asJson();
            }

            // handle errors
            if (request.getStatus() != 200) {
                throw new RemoteException("NewDay API failure: status code " + request.getStatus() + " for endpoint " +
                        "https://portal.newdaycards.com/accounts/services/rest/" + endpoint + ".");
            }

            JSONObject json = request.getBody().getObject();

            // handle errors in the JSON
            if (json.getString("status").equals("error")) {
                // if it looks like we need to reauthenticate and we're allowed to, do it!
                if (json.getJSONArray("errors").getJSONObject(0).getString("message").equals("") &&
                        attemptReauthentication) {
                    authenticate();
                    return callEndpoint(endpoint, body, false);
                } else {
                    throw new RemoteException(
                            json.getJSONArray("errors").getJSONObject(0).getString("message"));
                }
            } else {
                // return the error free JSON
                return json.getJSONObject("response");
            }
        } catch (UnirestException e) {
            // TODO: handle this better (connection timeouts etc.)
            throw new RemoteException("UnirestException", e);
        }
    }

    private void callAccountSummaryEndpoint() throws RemoteException {
        JSONObject json = callEndpoint("getAccountSummaryData");

        // set available to spend
        availableToSpend = new CachedValue<>(new Long(new DecimalFormat(getDecimalFormatPattern()).format(
                json.getJSONObject("accountSummary").getBigDecimal("otb")).replace(".", "")));

        // set balance
        balance = new CachedValue<>(-1 * new Long(new DecimalFormat(getDecimalFormatPattern()).format(
                json.getJSONObject("accountSummary").getBigDecimal("currentBalance")).replace(".", "")));

        // set credit limit
        Long creditLimitMajorUnits = json.getJSONObject("accountSummary").getLong("creditLimit");
        StringBuilder creditLimitStr = new StringBuilder(creditLimitMajorUnits.toString());
        for (int i = 0; i < getCurrency().getDefaultFractionDigits(); i++) {
            creditLimitStr.append("0");
        }
        creditLimit = new CachedValue<>(new Long(creditLimitStr.toString()));

        // set next statement date
        try {
            nextStatementDate = new CachedValue<>(
                    new SimpleDateFormat("dd/MM/yyyy").parse(
                            json.getJSONObject("accountSummary").getString("nextStatementDt")));
        } catch (ParseException e) {
            throw new RemoteException("Error parsing next statement date provided by account summary endpoint!", e);
        }

        // set nickname
        nickname = json.getJSONObject("accountSummary").getString("productName");
    }

    private void callStatementsEndpoint() throws RemoteException {
        // there isn't actually a single endpoint which gets statements, but for simplicity we'll pretend there is
        // get the dates of the available statements
        JSONArray statementDatesJSON = callEndpoint("/v1/getStatementDates", "{}")
                 .getJSONArray("statementDates");

        // convert the JSON to an ArrayList, backwards (so the first element of the ArrayList is the oldest date
        ArrayList<Date> getStatementDates = new ArrayList<>();
        try {
            for (int i = statementDatesJSON.length() - 1; i >= 0; i--) {
                getStatementDates.add(new SimpleDateFormat("dd/MM/yyyy").parse(
                        statementDatesJSON.getJSONObject(i).getString("stmtDate")));
            }
        } catch (ParseException e) {
            throw new RemoteException("Error parsing statement date provided by statement date endpoint!", e);
        }

        // get a working copy of the statements we already have
        LinkedHashMap<UUID, Statement> statements;
        try {
            statements = this.statements.value;
        } catch (NullPointerException e) {
            statements = new LinkedHashMap<>();
        }

        // find the last statement
        try {
            Statement lastStatement = null;
            for (Statement statement : statements.values()) {
                lastStatement = statement;
            }

            // don't get details of statements which aren't newer than the last one we have
            for (int i = 0; i < getStatementDates.size(); i++) {
                if (!getStatementDates.get(i).after(lastStatement.issued)) {
                    getStatementDates.remove(i);
                    i--;
                }
            }
        } catch (NullPointerException e) {
            // if we don't have any statements yet then get all the statements we can
        }

        // get details of the new statements
        for (Date statementDate : getStatementDates) {
            String statementDateStr = new SimpleDateFormat("dd MMM yyyy").format(statementDate);

            JSONObject statementJSON = callEndpoint(
                    "v1/getStatementSummaryNew",
                    "{\"relativeMonthNumber\":0, \"stmtDate\":\"" + statementDateStr + "\"}"
            ).getJSONObject("statementDetail");

            // set ID
            UUID id = UUID.randomUUID();

            // set balance
            Long balance = new Long(new DecimalFormat(getDecimalFormatPattern()).format(
                    statementJSON.getBigDecimal("stdEndBal")).replace(".", ""));

            // set previous balance
            Long previousBalance = new Long(new DecimalFormat(getDecimalFormatPattern()).format(
                    statementJSON.getBigDecimal("stdBegBal")).replace(".", ""));

            // set minimum payment
            Long minimumPayment = new Long(new DecimalFormat(getDecimalFormatPattern()).format(
                    statementJSON.getBigDecimal("minimumPayment")).replace(".", ""));

            // set issued date
            Date issued;
            try {
                issued = new SimpleDateFormat("dd/MM/yyyy").parse(statementJSON.getString("stmtDate"));
            } catch (ParseException e) {
                throw new RemoteException("Error parsing statement issued date provided by statement summary endpoint!", e);
            }

            // set due date
            Date due;
            try {
                due = new SimpleDateFormat("dd/MM/yyyy").parse(statementJSON.getString("paymentDueDate"));
            } catch (ParseException e) {
                throw new RemoteException("Error parsing statement due date provided by statement summary endpoint!", e);
            }

            // add statement to statements
            statements.put(id, new Statement(balance,
                    due,
                    id,
                    issued,
                    minimumPayment,
                    previousBalance));
        }

        // set global statements (with new updated date)
        this.statements = new CachedValue<>(statements);
    }

    private void callTransactionEndpoint() throws RemoteException {
        LinkedHashMap<UUID, Transaction> transactions;
        try {
            transactions = this.transactions.value;
        } catch (NullPointerException e) {
            // if we don't have any transactions yet, just create an empty LinkedHashMap
            transactions = new LinkedHashMap<>();
        }
        int getStatementIndex;

        callStatementsEndpoint(); // make sure our statements are up-to-date (override cache)
        Object[] statementIds = statements.value.keySet().toArray();

        try {
            UUID newestStatementDownloaded = null;

            // try to find a statement which we already have
            for (Map.Entry<UUID, Transaction> t : transactions.entrySet()) {
                newestStatementDownloaded = t.getValue().statementId;
                if (newestStatementDownloaded != null) {
                    break;
                }
            }

            // if we failed
            if (newestStatementDownloaded == null) {
                getStatementIndex = statementIds.length - 1; // get all the statements available
            } else {

                // get all te statements newer than this one
                getStatementIndex = Arrays.asList(statementIds).indexOf(newestStatementDownloaded) - 1;
            }
        } catch (NullPointerException e) {
            getStatementIndex = statementIds.length - 1; // if there aren't any transactions, get them all!
        }

        if (getStatementIndex > 0) {
            JSONObject body = new JSONObject();
            body.put("detailFlag", "M");
            body.put("noOfTransaction", 50);
            body.put("tranStartNum", 0);
            for (getStatementIndex = getStatementIndex; getStatementIndex >= 0; getStatementIndex--) {
                // get the transactions for the given index
                body.put("tranNbrMonths", getStatementIndex);
                JSONObject json = callEndpoint("v1/getTransactions", body);

                JSONArray transactionDetails = json.getJSONArray("transactionDetails");
                for (int i = 0; i < transactionDetails.length(); i++) {
                    JSONObject jsonTransaction = transactionDetails.getJSONObject(i);

                    // ID stuff
                    UUID id = UUID.randomUUID();
                    String providerId = jsonTransaction.getString("tranRefNo");
                    transactionRefs.put(providerId, id);

                    // amount: NewDay reports debits as positive and credits as negative
                    Long amount = -1 * new Long(new DecimalFormat("0.00").format(
                            jsonTransaction.getBigDecimal("amount")).replace(".", ""));

                    // created time
                    String[] createdDateParts = jsonTransaction.getString("effectiveDate").split("/");
                    ZonedDateTime created = ZonedDateTime.of(
                            Integer.parseInt(createdDateParts[2]),
                            Integer.parseInt(createdDateParts[1]),
                            Integer.parseInt(createdDateParts[0]),
                            0,
                            0,
                            0,
                            0,
                            ZoneId.of("UTC")
                    );

                    // description
                    String description = jsonTransaction.getString("description");

                    // mcc
                    String mcc = Integer.toString(jsonTransaction.getInt("mcc"));

                    // settled date
                    String[] settledDateParts = jsonTransaction.getString("postedDate").split("/");
                    ZonedDateTime settled = ZonedDateTime.of(
                            Integer.parseInt(settledDateParts[2]),
                            Integer.parseInt(settledDateParts[1]),
                            Integer.parseInt(settledDateParts[0]),
                            0,
                            0,
                            0,
                            0,
                            ZoneId.of("UTC")
                    );

                    // statement ID
                    UUID statementId = (UUID) statementIds[statementIds.length - getStatementIndex - 1];

                    // for foreign transactions
                    Long localAmount = null;
                    Currency localCurrency = null;

                    // constants (for now!)
                    Long balance = 0L;
                    Counterparty counterparty = null;
                    DeclineReason declineReason = null;
                    TransactionCategory category = TransactionCategory.GENERAL;
                    TransactionType type = TransactionType.UNKNOWN;

                    transactions.put(id, new Transaction(
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
                            statementId,
                            type
                    ));
                }
            }
        }

        this.transactions = new CachedValue<>(transactions);
    }

    public Long getAvailableToSpend() throws RemoteException {
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

    public Long getBalance() throws RemoteException {
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

    public Long getCreditLimit() throws RemoteException {
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

    public Date getNextStatementDate() throws RemoteException {
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

    public String getNickname() throws RemoteException {
        if (nickname == null) {
            callAccountSummaryEndpoint();
        }
        return nickname;
    }

    public Statement[] getStatements() throws RemoteException {
        try {
            // cache for 1 hour
            if (ChronoUnit.SECONDS.between(transactions.updated, ZonedDateTime.now()) < 3600) {
                return statements.value.values().toArray(new Statement[statements.value.size()]);
            } else {
                callStatementsEndpoint();
                return statements.value.values().toArray(new Statement[statements.value.size()]);
            }
        } catch (NullPointerException e) {
            callStatementsEndpoint();
            return statements.value.values().toArray(new Statement[statements.value.size()]);
        }
    }

    public Transaction[] getTransactions() throws RemoteException {
        try {
            // cache for 5 minutes
            if (ChronoUnit.SECONDS.between(transactions.updated, ZonedDateTime.now()) < 300) {
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
        passcode = yamm.generateSecureRandom(passcode.length);
        password = yamm.generateSecureRandom(password.length);
        username = yamm.generateSecureRandom(username.length);
    }

    public void setNickname(String newNickname) {
        nickname = newNickname;
    }

    /*public void XcallAuthenticationEndpoint() throws UnirestException, RemoteException {
        System.out.println("Test account summary endpoint:");
        System.out.println(
                Unirest
                        .post("https://portal.newdaycards.com/accounts/services/rest/getAccountSummaryData")
                        .asJson()
                        .getBody()
                        .getObject()
        );
        System.out.println();

        System.out.println("Test statement dates endpoint:");
        System.out.println(
                Unirest
                        .post("https://portal.newdaycards.com/accounts/services/rest/v1/getStatementDates")
                        .body("{}")
                        .asJson()
                        .getBody()
                        .getObject()
        );
        System.out.println();

        System.out.println("Test statement summary endpoint:");
        // note: in the body, relativeMonthNumber must be set to an integer between 0 and 5 inclusive, but the value
        // doesn't appear to impact the results (an illegal value results in "System currently unavailable").
        System.out.println(
                Unirest
                        .post("https://portal.newdaycards.com/accounts/services/rest/v1/getStatementSummaryNew")
                        .body("{\"relativeMonthNumber\":0, \"stmtDate\":\"05 Dec 2017\"}")
                        .asJson()
                        .getBody()
                        .getObject()
        );
        System.out.println();

        System.out.println("Test transactions endpoint (\"current\" - not on a statement yet):");
        System.out.println(
                Unirest
                        .post("https://portal.newdaycards.com/accounts/services/rest/v1/getAllTransactions")
                        .body("{\"noOfTransaction\":50,\"pendingFlag\":true,\"detailFlag\":\"C\",\"tranStartNum\":0}")
                        .asJson()
                        .getBody()
                        .getObject()
        );
        System.out.println();

        System.out.println("Test transactions endpoint (statement):");
        System.out.println(
                Unirest
                        .post("https://portal.newdaycards.com/accounts/services/rest/v1/getTransactions")
                        .body("{\"noOfTransaction\":50,\"tranNbrMonths\":0,\"detailFlag\":\"M\",\"tranStartNum\":0}")
                        .asJson()
                        .getBody()
                        .getObject()
        );
        System.out.println();

        // when the cookie expires the response is: {"errors":[{"code":"FDESU001","message":""}],"status":"error"}
    }*/
}
