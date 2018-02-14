package io.yamm.backend.providers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.yamm.backend.*;
import org.apache.http.HttpException;
import org.apache.http.auth.InvalidCredentialsException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private CachedValue<TransactionStore, ZonedDateTime> transactions;
    private char[] username;
    private final UUID uuid;
    private final YAMM yamm;

    @SuppressWarnings("unused") // accessed via reflection
    public static final String[] requiredCredentials = new String[] {"username", "password", "passcode"};

    public NewDay(char[][] credentials, YAMM yamm) throws HttpException {
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
                  Statement[] statements,
                  Transaction[] transactions,
                  UUID uuid,
                  YAMM yamm) throws HttpException {
        this.username = Arrays.copyOf(credentials[0], credentials[0].length);
        this.password = Arrays.copyOf(credentials[1], credentials[1].length);
        this.passcode = Arrays.copyOf(credentials[2], credentials[2].length);

        this.nickname = nickname;

        LinkedHashMap<UUID, Statement> statementsLinkedHashMap = new LinkedHashMap<>();
        for (int i = statements.length -1; i >= 0; i--) {
            statementsLinkedHashMap.put(statements[i].id, statements[i]);
        }
        this.statements = new CachedValue<>(statementsLinkedHashMap);

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

    private void authenticate() throws HttpException {
        // check username looks sensible (minimum length 6)
        if (username.length < 6) {
            throw new InvalidCredentialsException("Invalid username (6 characters required)");
        }

        // check password looks sensible (minimum length 6)
        if (password.length < 6) {
            throw new InvalidCredentialsException("Invalid password (6 characters required)");
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
                throw new InvalidCredentialsException("Invalid passcode (6 digits required)");
            }
        }

        try {
            // perform preauth w/ username and password
            JSONObject preauth = Unirest
                    .post("https://portal.newdaycards.com/accounts/services/rest/login/preauth?portalName=" + getSlug())
                    .body("{\"userId\":\"" + new String(username) + "\",\"passwd\":\"" + new String(password) + "\"}")
                    .asJson()
                    .getBody()
                    .getObject();
            if (preauth.has("errors")) {
                String errorMessage = preauth.getJSONArray("errors").getJSONObject(0).getString("message");
                if (errorMessage.equals("Please enter a valid username and password.")) {
                    throw new InvalidCredentialsException("Invalid username and / or password");
                } else if (errorMessage.startsWith("Account is locked.")) {
                    throw new InvalidCredentialsException("Account is locked; please visit online servicing to unlock the account");
                } else {
                    throw new HttpException(errorMessage);
                }
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
                String errorMessage = fullAuthBody.getJSONArray("errors").getJSONObject(0).getString("message");
                if (errorMessage.equals("Please enter a valid passcode.")) {
                    throw new InvalidCredentialsException("Invalid passcode");
                } else if (errorMessage.startsWith("Account is locked.")) {
                    throw new InvalidCredentialsException("Account is locked; please visit online servicing to unlock the account");
                } else {
                    throw new HttpException(errorMessage);
                }
            }
        } catch (UnirestException e) {
            // TODO: handle this better (connection timeouts etc.)
            throw new HttpException("UnirestException", e);
        }
    }

    private JSONObject callEndpoint(String endpoint) throws HttpException {
        return callEndpoint(endpoint, null, true);
    }

    private JSONObject callEndpoint(String endpoint, String body) throws HttpException {
        return callEndpoint(endpoint, body, true);
    }

    private JSONObject callEndpoint(String endpoint, JSONObject body) throws HttpException {
        return callEndpoint(endpoint, body.toString(), true);
    }

    private JSONObject callEndpoint(String endpoint, String body, boolean attemptReauthentication) throws HttpException {
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

            // check portal
            if (request.getHeaders().getFirst("cookie") != null) {
                Matcher matcher = Pattern.compile(".*?redirectPortal=(.*?);").
                        matcher(request.getHeaders().getFirst("cookie") + ";");
                if (matcher.find()) {
                    if (!matcher.group(1).equals(getSlug())) {
                        // if we're somehow on the wrong portal, reauthenticate and try again
                        authenticate();
                        return callEndpoint(endpoint, body, false);
                    }
                }
            }

            // handle errors
            if (request.getStatus() != 200) {
                throw new HttpException("NewDay API failure: status code " + request.getStatus() + " for endpoint " +
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
                    throw new HttpException(
                            json.getJSONArray("errors").getJSONObject(0).getString("message"));
                }
            } else {
                // return the error free JSON
                return json.getJSONObject("response");
            }
        } catch (UnirestException e) {
            // TODO: handle this better (connection timeouts etc.)
            throw new HttpException("UnirestException", e);
        }
    }

    private void callAccountSummaryEndpoint() throws HttpException {
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
            throw new HttpException("Error parsing next statement date provided by account summary endpoint!", e);
        }

        // set nickname
        nickname = json.getJSONObject("accountSummary").getString("productName");
    }

    private void callStatementsEndpoint() throws HttpException {
        // there isn't actually a single endpoint which gets statements, but for simplicity we'll pretend there is
        // get the dates of the available statements
        JSONArray statementDatesJSON;
        try {
            statementDatesJSON = callEndpoint("/v1/getStatementDates", "{}")
                    .getJSONArray("statementDates");
        } catch (HttpException e) {
            if (e.getMessage().equals("No statements are found for the input account. ")) {
                this.statements = new CachedValue<>(new LinkedHashMap<>());
                return;
            } else {
                throw e;
            }
        }

        // convert the JSON to an ArrayList, backwards (so the first element of the ArrayList is the oldest date
        ArrayList<Date> getStatementDates = new ArrayList<>();
        try {
            for (int i = statementDatesJSON.length() - 1; i >= 0; i--) {
                getStatementDates.add(new SimpleDateFormat("dd/MM/yyyy").parse(
                        statementDatesJSON.getJSONObject(i).getString("stmtDate")));
            }
        } catch (ParseException e) {
            throw new HttpException("Error parsing statement date provided by statement date endpoint!", e);
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
                throw new HttpException("Error parsing statement issued date provided by statement summary endpoint!", e);
            }

            // set due date
            Date due;
            try {
                due = new SimpleDateFormat("dd/MM/yyyy").parse(statementJSON.getString("paymentDueDate"));
            } catch (ParseException e) {
                throw new HttpException("Error parsing statement due date provided by statement summary endpoint!", e);
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

    public Long getAvailableToSpend() throws HttpException {
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

    public Long getBalance() throws HttpException {
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

    public Long getCreditLimit() throws HttpException {
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

    public Date getNextStatementDate() throws HttpException {
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

    public String getNickname() throws HttpException {
        if (nickname == null) {
            callAccountSummaryEndpoint();
        }
        return nickname;
    }

    public Statement[] getStatements() throws HttpException {
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

    public Transaction[] getTransactions() throws HttpException {
        try {
            // cache for 5 minutes
            if (ChronoUnit.SECONDS.between(transactions.updated, ZonedDateTime.now()) < 300) {
                return transactions.value.toArray();
            } else {
                getTransactionsByStatements();
                getTransactionsCurrent();
                return transactions.value.toArray();
            }
        } catch (NullPointerException e) {
            getTransactionsByStatements();
            getTransactionsCurrent();
            return transactions.value.toArray();
        }
    }

    private void getTransactionsByStatement(int statementIndex, UUID statementId) throws HttpException {
        TransactionStore transactions;
        try {
            transactions = this.transactions.value;
        } catch (NullPointerException e) {
            // if we don't have any transactions yet, just create an empty LinkedHashMap
            transactions = new TransactionStore();
        }

        JSONObject body = new JSONObject();
        body.put("detailFlag", "M");
        body.put("noOfTransaction", 50);
        body.put("tranStartNum", 0);
        body.put("tranNbrMonths", statementIndex);
        JSONObject json = callEndpoint("v1/getTransactions", body);

        JSONArray transactionDetails = json.getJSONArray("transactionDetails");
        for (int i = 0; i < transactionDetails.length(); i++) {
            JSONObject transactionJson = transactionDetails.getJSONObject(i);

            // get a transaction object to add a statementId to
            Transaction transaction = transactions.get(transactionJson.getString("tranRefNo"));
            if (transaction == null) {
                transaction = jsonToTransaction(transactionJson);
            }

            // create a new transaction object with the correct statementId and add it to the transaction store
            transactions.add(new Transaction(
                    transaction.amount,
                    transaction.balance,
                    transaction.category,
                    transaction.counterparty,
                    transaction.created,
                    transaction.declineReason,
                    transaction.description,
                    transaction.id,
                    transaction.localAmount,
                    transaction.localCurrency,
                    transaction.mcc,
                    transaction.providerId,
                    transaction.settled,
                    statementId,
                    transaction.type
            ));
        }

        // update the object's cached transactions
        this.transactions = new CachedValue<>(transactions);
    }

    private void getTransactionsByStatements() throws HttpException {
        callStatementsEndpoint(); // make sure our statements are up-to-date (override cache)
        Object[] statementIds = statements.value.keySet().toArray();

        // try to find a statement which we already have
        UUID newestStatementDownloaded = null;
        try {
            for (int i = transactions.value.size() - 1; i >= 0; i--) {
                newestStatementDownloaded = transactions.value.get(i).statementId;
                if (newestStatementDownloaded != null) {
                    break;
                }
            }
        } catch (NullPointerException e) { // if the transaction store hasn't been initialised, get all statements
        }

        int getStatementIndex;
        if (newestStatementDownloaded == null) { // if we failed
            getStatementIndex = statementIds.length - 1; // get all the statements available
        } else { // get all the statements newer than this one
            getStatementIndex = Arrays.asList(statementIds).indexOf(newestStatementDownloaded) - 1;
        }

        if (getStatementIndex > 0) { // if there are statements we don't (yet) have
            for (; getStatementIndex >= 0; getStatementIndex--) {
                UUID statementId = (UUID) statementIds[statementIds.length - getStatementIndex - 1];
                getTransactionsByStatement(getStatementIndex, statementId);
            }
        }
    }

    private void getTransactionsCurrent() throws HttpException {
        TransactionStore transactions;
        try {
            transactions = this.transactions.value;
        } catch (NullPointerException e) {
            // if we don't have any transactions yet, just create an empty LinkedHashMap
            transactions = new TransactionStore();
        }

        JSONObject body = new JSONObject();
        body.put("detailFlag", "C");
        body.put("noOfTransaction", 50);
        body.put("pendingFlag", true); // you would've thought you could set this to "false"... you'd be wrong!
        body.put("tranStartNum", 0);
        JSONObject json = callEndpoint("v1/getAllTransactions", body);

        JSONArray transactionDetails = json.getJSONArray("transactionDetails");
        for (int i = 0; i < transactionDetails.length(); i++) {
            JSONObject transactionJson = transactionDetails.getJSONObject(i);

            try {
                Transaction transaction = transactions.get(transactionJson.getString("tranRefNo"));
                if (transaction == null) { // if the transaction isn't yet in the transaction store
                    transactions.add(jsonToTransaction(transactionJson)); // add it!
                }
            } catch (JSONException e) {
                // transaction has not yet cleared, so will be ignored
            }
        }

        // update the object's cached transactions
        this.transactions = new CachedValue<>(transactions);
    }

    public UUID getUUID() {
        return uuid;
    }

    private Transaction jsonToTransaction(JSONObject json) {
        // ID stuff
        UUID id = UUID.randomUUID();
        String providerId = json.getString("tranRefNo");

        // amount: NewDay reports debits as positive and credits as negative
        Long amount = -1 * new Long(new DecimalFormat("0.00").format(
                json.getBigDecimal("amount")).replace(".", ""));

        // created time
        String[] createdDateParts = json.getString("effectiveDate").split("/");
        ZonedDateTime created = ZonedDateTime.of(
                Integer.parseInt(createdDateParts[2]),
                Integer.parseInt(createdDateParts[1]),
                Integer.parseInt(createdDateParts[0]),
                0,
                0,
                0,
                0,
                ZoneOffset.ofHours(0)
        );

        // description
        String description = json.getString("description");

        // mcc
        String mcc = Integer.toString(json.getInt("mcc"));

        // settled date
        String[] settledDateParts = json.getString("postedDate").split("/");
        ZonedDateTime settled = ZonedDateTime.of(
                Integer.parseInt(settledDateParts[2]),
                Integer.parseInt(settledDateParts[1]),
                Integer.parseInt(settledDateParts[0]),
                0,
                0,
                0,
                0,
                ZoneOffset.ofHours(0)
        );

        // for foreign transactions
        Long localAmount = null;
        Currency localCurrency = null;
        String foreignCurrencyStr = json.getString("foreignTxnCurrency");
        if (!foreignCurrencyStr.equals("")) {
            localCurrency = Currency.getInstance(foreignCurrencyStr);
            int decimalPlaces = localCurrency.getDefaultFractionDigits();
            if (decimalPlaces != 0) {
                StringBuilder patternBuilder = new StringBuilder("0.");
                for (int j = 0; j < decimalPlaces; j++) {
                    patternBuilder.append("0");
                }
                localAmount = Math.abs(new Long(new DecimalFormat(patternBuilder.toString()).format(
                        json.getBigDecimal("foreignTxnAmnt")).replace(".", "")));
            } else {
                localAmount = Math.abs(json.getLong("foreignTxnAmnt"));
            }
        }

        // constants (for now!)
        Counterparty counterparty = null;
        DeclineReason declineReason = null;
        TransactionType type = TransactionType.UNKNOWN;

        return new Transaction(
                amount,
                0L, // TODO: consider setting this (or deprecating balance)
                TransactionCategory.GENERAL,
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
                null, // we have no way of knowing the statementId here
                type
        );
    }

    public void overwriteSensitiveData() {
        passcode = yamm.generateSecureRandom(passcode.length);
        password = yamm.generateSecureRandom(password.length);
        username = yamm.generateSecureRandom(username.length);
    }

    public void setNickname(String newNickname) {
        nickname = newNickname;
    }
}
