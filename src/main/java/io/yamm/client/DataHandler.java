package io.yamm.client;

import com.neovisionaries.i18n.CountryCode;
import io.yamm.backend.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.security.Security;
import java.time.ZonedDateTime;
import java.util.*;

class DataHandler {
    private String dataFolder = null;
    private StandardPBEStringEncryptor encryptor;
    private final GUI gui;
    private Properties properties = new Properties();
    private final YAMM yamm;

    DataHandler(GUI gui, YAMM yamm) {
        this.gui = gui;
        this.yamm = yamm;
        Security.setProperty("crypto.policy", "unlimited");

        // load properties
        if (new File("config.properties").exists()) {
            // load existing config
            InputStream input = null;
            try {
                input = new FileInputStream("config.properties");
                properties.load(input);
                dataFolder = properties.getProperty("dataFolder");
            } catch (IOException e) {
                gui.showException(e);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        gui.showException(e);
                    }
                }
            }
        }

        // if no directory is specified, ask the user for one
        if (dataFolder == null) {
            gui.showWarning(
                    "Data not found!",
                    "No data folder was found. Please select a folder to store YAMM data in.");
            try {
                dataFolder = gui.requestFolder();
            } catch (NullPointerException e) {
                gui.showError("No folder selected! YAMM will now quit.");
                gui.quit();
            }
        }

        // check if data exists
        boolean dataExists = false;
        if (new File(dataFolder + File.separator + "accounts.yamm").exists()) {
            dataExists = true;
        }

        // request the password
        char[] password = {};
        while (password.length < 12) {
            try {
                if (dataExists) {
                    password = gui.requestCharArray("Please enter your YAMM encryption password:");
                } else {
                    password = gui.requestCharArray("Please enter a password (minimum length 12 characters) to use to encrypt your YAMM data:");
                }
                if (password.length < 12) {
                    gui.showError("Password must be greater than 12 characters!");
                } else {
                    break;
                }
            } catch (NullPointerException e) {
                if (dataExists) {
                    gui.showError("A password is required to decrypt YAMM data. YAMM will now quit.");
                } else {
                    gui.showError("A password is required to encrypt YAMM data. YAMM will now quit.");
                }
                saveProperties();
                gui.quitWithoutSaving();
            }
        }
        encryptor = new StandardPBEStringEncryptor();
        encryptor.setProvider(new BouncyCastleProvider());
        encryptor.setAlgorithm("PBEWITHSHA256AND256BITAES-CBC-BC");
        encryptor.setPasswordCharArray(password);
        //noinspection UnusedAssignment securely overwrite password
        password = YAMM.generateRandom(new SecureRandom(), password.length);

        // check that we can do crypto
        try {
            encryptor.encrypt("");
        } catch (EncryptionOperationNotPossibleException e) {
            gui.showError("Unable to initialise YAMM cryptography! This is probably because the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files have not been installed. YAMM will now quit.");
            saveProperties();
            gui.quitWithoutSaving();
        }

        // if data exists, load it
        if (dataExists) {
            try {
                // load account data
                JSONArray accounts = new JSONArray(encryptedRead("accounts.yamm"));

                // load transaction data and create account
                for (int i = 0; i < accounts.length(); i++) {
                    JSONObject accountJSON = accounts.getJSONObject(i);

                    // get the account ID
                    UUID uuid = UUID.fromString(accountJSON.getString("id"));

                    // build the array of transactions
                    Transaction[] transactions;
                    try {
                        JSONArray transactionsJSON = new JSONArray(encryptedRead(uuid + ".yamm"));
                        transactions = new Transaction[transactionsJSON.length()];
                        // iterate over transactions backwards
                        for (int j = 0; j < transactions.length; j++) {
                            transactions[j] = JSONToTransaction(transactionsJSON.getJSONObject(j));
                        }
                    } catch (IOException e) {
                        transactions = new Transaction[0];
                    }

                    // get the provider class
                    String providerString = accountJSON.getString("provider");
                    Class<?> provider;
                    try {
                        provider = Class.forName("io.yamm.backend.providers." + providerString);
                    } catch (ClassNotFoundException e) {
                        gui.showError("Unknown provider \"" + providerString + "\" referenced in save data!");
                        continue;
                    }

                    // load account data from JSON
                    // credentials
                    char[][] credentials = new char[accountJSON.getJSONArray("credentials").length()][];
                    for (int j = 0; j < accountJSON.getJSONArray("credentials").length(); j++) {
                        credentials[j] = accountJSON.getJSONArray("credentials").getString(j).toCharArray();
                    }
                    // account data
                    Currency currency = Currency.getInstance(accountJSON.getString("currency"));
                    String nickname = accountJSON.getString("nickname");
                    if (BankAccount.class.isAssignableFrom(provider)) {
                        // bank account data
                        String accountNumber = accountJSON.getString("accountNumber");
                        String bic = accountJSON.getString("bic");
                        String iban = accountJSON.getString("iban");
                        String sortCode = accountJSON.getString("sortCode");

                        // instantiate!
                        yamm.addAccount((Account) provider.getConstructor(
                                char[][].class,
                                String.class,
                                String.class,
                                Currency.class,
                                String.class,
                                String.class,
                                String.class,
                                Transaction[].class,
                                UUID.class,
                                YAMM.class)
                                .newInstance(
                                        credentials,
                                        accountNumber,
                                        bic,
                                        currency,
                                        iban,
                                        nickname,
                                        sortCode,
                                        transactions,
                                        uuid,
                                        yamm));
                    } else {
                        // instantiate Accounts which don't implement BankAccount
                        // except... we don't have any of them! (yet...)
                    }

                    // create account from account and transaction data
                }
            } catch (IllegalAccessException|
                    IOException|
                    InstantiationException|
                    InvocationTargetException|
                    NoSuchMethodException e) {
                gui.showException(e);
            }
        }
    }

    static JSONObject accountToJSON(Account account) throws RemoteException {
        JSONObject json = new JSONObject();

        json.put("id", account.getUUID());
        json.put("availableToSpend", account.getAvailableToSpend());
        json.put("balance", account.getBalance());
        json.put("currency", account.getCurrency());
        json.put("nickname", account.getNickname());
        json.put("provider", account.getClass().getSimpleName());

        if (account instanceof BankAccount) {
            json.put("accountNumber", ((BankAccount) account).getAccountNumber());
            json.put("bic", ((BankAccount) account).getBIC());
            json.put("iban", ((BankAccount) account).getIBAN());
            json.put("sortCode", ((BankAccount) account).getSortCode());
        }

        return json;
    }

    static JSONArray accountsToJSON(Map<UUID, Account> accounts) throws RemoteException {
        JSONArray json = new JSONArray();
        for (Map.Entry<UUID, Account> account : accounts.entrySet()) {
            json.put(accountToJSON(account.getValue()));
        }
        return json;
    }

    static JSONObject addressToJSON(Address address) {
        JSONObject json = new JSONObject();

        if (address.approximate != null) {
            json.put("approximate", address.approximate);
        }
        if (address.city != null) {
            json.put("city", address.city);
        }
        if (address.country != null) {
            json.put("country", address.country);
        }
        if (address.county != null) {
            json.put("county", address.county);
        }
        if (address.latitude != null) {
            json.put("latitude", address.latitude);
        }
        if (address.longitude != null) {
            json.put("longitude", address.longitude);
        }
        if (address.postcode != null) {
            json.put("postcode", address.postcode);
        }
        if (address.streetAddress != null) {
            json.put("streetAddress", address.streetAddress);
        }

        return json;
    }

    static JSONObject counterpartyToJSON(Counterparty counterparty) {
        JSONObject json = new JSONObject();

        if (counterparty.accountNumber != null) {
            json.put("accountNumber", counterparty.accountNumber);
        }
        if (counterparty.address != null) {
            json.put("address", addressToJSON(counterparty.address));
        }
        if (counterparty.icon != null) {
            json.put("icon", counterparty.icon);
        }
        if (counterparty.name != null) {
            json.put("name", counterparty.name);
        }
        if (counterparty.sortCode != null) {
            json.put("sortCode", counterparty.sortCode);
        }
        if (counterparty.website != null) {
            json.put("website", counterparty.website);
        }

        return json;
    }

    private String encryptedRead(String filename) throws IOException {
        InputStream input = new FileInputStream(dataFolder + File.separator + filename);
        String encrypted = new Scanner(input, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        input.close();
        try {
            return encryptor.decrypt(encrypted);
        } catch (EncryptionOperationNotPossibleException e) {
            gui.showError("Incorrect password entered! YAMM will now quit.");
            gui.quitWithoutSaving();
            return null; // we'll never actually get here due to the previous statement
        }
    }

    private void encryptedWrite(String filename, String data) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(dataFolder + File.separator + filename));
            writer.write(encryptor.encrypt(data));
        } catch (IOException e) {
            gui.showException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    gui.showException(e);
                }
            }
        }
    }

    private Address JSONToAddress(JSONObject json) {
        Boolean approximate;
        String city;
        CountryCode country;
        String county;
        Double latitude;
        Double longitude;
        String postcode;
        String streetAddress;

        try {
            approximate = json.getBoolean("approximate");
        } catch (JSONException e) {
            approximate = true;
        }

        try {
            city = json.getString("city");
        } catch (JSONException e) {
            city = null;
        }

        try {
            country = CountryCode.getByCode(json.getString("country"));
        } catch (JSONException e) {
            country = null;
        }

        try {
            county = json.getString("county");
        } catch (JSONException e) {
            county = null;
        }

        try {
            latitude = json.getDouble("latitude");
        } catch (JSONException e) {
            latitude = null;
        }

        try {
            longitude = json.getDouble("longitude");
        } catch (JSONException e) {
            longitude = null;
        }

        try {
            postcode = json.getString("postcode");
        } catch (JSONException e) {
            postcode = null;
        }

        try {
            streetAddress = json.getString("streetAddress");
        } catch (JSONException e) {
            streetAddress = null;
        }

        return new Address(
                approximate,
                city,
                country,
                county,
                latitude,
                longitude,
                postcode,
                streetAddress
        );
    }

    private Counterparty JSONToCounterparty(JSONObject json) {
        String accountNumber;
        Address address;
        URL icon;
        String name;
        String sortCode;
        URL website;

        try {
            accountNumber = json.getString("accountNumber");
        } catch (JSONException e) {
            accountNumber = null;
        }

        try {
            address = JSONToAddress(json.getJSONObject("address"));
        } catch (JSONException e ) {
            address = null;
        }

        try {
            icon = new URL(json.getString("icon"));
        } catch (JSONException | MalformedURLException e) {
            // TODO: log this failure (and handle it better)
            icon = null;
        }

        try {
            name = json.getString("name");
        } catch (JSONException e) {
            // TODO: log this failure (and handle it better)
            name = "undefined";
        }

        try {
            sortCode = json.getString("sortCode");
        } catch (JSONException e) {
            sortCode = null;
        }

        try {
            website = new URL(json.getString("website"));
        } catch (JSONException | MalformedURLException e) {
            // TODO: log this failure (and handle it better)
            website = null;
        }

        return new Counterparty(
            accountNumber,
            address,
            icon,
            name,
            sortCode,
            website
        );
    }

    private Transaction JSONToTransaction(JSONObject json) {
        Long amount;
        Long balance;
        TransactionCategory category;
        Counterparty counterparty;
        ZonedDateTime created;
        DeclineReason declineReason;
        String description;
        UUID id;
        Long localAmount;
        Currency localCurrency;
        String mcc; // ISO 18245 merchant category code
        String providerId;
        ZonedDateTime settled;
        TransactionType type;

        try {
            amount = json.getLong("amount");
        } catch (JSONException e) {
            // TODO: log this failure (and handle it better)
            amount = 0L;
        }

        try {
            balance = json.getLong("balance");
        } catch (JSONException e) {
            // TODO: log this failure (and handle it better)
            balance = 0L;
        }

        try {
            category = TransactionCategory.valueOf(json.getString("category"));
        } catch (JSONException | IllegalArgumentException e ) {
            category = TransactionCategory.GENERAL;
        }

        try {
            counterparty = JSONToCounterparty(json.getJSONObject("counterparty"));
        } catch (JSONException e ) {
            // TODO: log this failure (and handle it better)
            counterparty = null;
        }

        try {
            created = ZonedDateTime.parse(json.getString("created"));
        } catch (JSONException e ) {
            // TODO: log this failure (and handle it better)
            created = ZonedDateTime.now();
        }

        try {
            declineReason = DeclineReason.valueOf(json.getString("declineReason"));
        } catch (JSONException | IllegalArgumentException e ) {
            declineReason = null;
        }

        try {
            description = json.getString("description");
        } catch (JSONException e) {
            description = "";
        }

        try {
            id = UUID.fromString(json.getString("id"));
        } catch (JSONException e) {
            // TODO: log this failure (and handle it better)
            id = UUID.randomUUID();
        }

        try {
            localAmount = json.getLong("localAmount");
        } catch (JSONException e) {
            // TODO: log this failure (and handle it better)
            localAmount = amount;
        }

        try {
            localCurrency = Currency.getInstance(json.getString("localCurrency"));
        } catch (IllegalArgumentException | JSONException e) {
            // TODO: log this failure (and handle it better)
            localCurrency = Currency.getInstance("GBP");
        }

        try {
            mcc = json.getString("mcc");
        } catch (JSONException e ) {
            mcc = null;
        }

        try {
            providerId = json.getString("providerId");
        } catch (JSONException e ) {
            providerId = null;
        }

        try {
            settled = ZonedDateTime.parse(json.getString("settled"));
        } catch (JSONException e ) {
            // TODO: log this failure (and handle it better)
            settled = null;
        }

        try {
            type = TransactionType.valueOf(json.getString("type"));
        } catch (JSONException | IllegalArgumentException e ) {
            type = TransactionType.UNKNOWN;
        }

        return new Transaction(
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
                type
        );

    }

    void overwriteSensitiveData() {
        yamm.overwriteSensitiveData();
    }

    void save() {
        saveData();
        saveProperties();
    }

    private void saveData() { // TODO: implement this
        JSONArray accounts = new JSONArray();
        for(Map.Entry<UUID, Account> account : yamm.getAccounts().entrySet()) {
            try {
                // get the account JSON
                JSONObject accountJson = accountToJSON(account.getValue());

                // get the account credentials using reflection
                @SuppressWarnings("JavaReflectionMemberAccess") // intellij gets it wrong! (ish...)
                Method getCredentials = account.getValue().getClass().getDeclaredMethod("getCredentials");
                getCredentials.setAccessible(true);

                // add the credentials to the JSON
                char[][] credentials = (char[][]) getCredentials.invoke(account.getValue());
                JSONArray credentialsJSON = new JSONArray();
                for (char[] credential : credentials) {
                    credentialsJSON.put(new String(credential));
                }
                accountJson.put("credentials", credentialsJSON);

                // add account data to the "accounts" file
                accounts.put(accountJson);

                // add data on the account transactions to an account-specific file
                JSONArray transactions = transactionsToJSON(account.getValue().getTransactions());

                // save the account-specific file
                encryptedWrite(
                        account.getValue().getUUID().toString() + ".yamm",
                        transactions.toString());
            } catch (IllegalAccessException|
                    InvocationTargetException|
                    NoSuchMethodException|
                    RemoteException e) {
                yamm.raiseException(e);
            }
        }

        // save the "accounts" file
        encryptedWrite("accounts.yamm", accounts.toString());
    }

    private void saveProperties() {
        OutputStream output = null;
        try {
            output = new FileOutputStream("config.properties");
            properties.setProperty("dataFolder", dataFolder);
            properties.store(output, null);
        } catch (IOException e) {
            gui.showException(e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    gui.showException(e);
                }
            }
        }
    }

    static JSONObject transactionToJSON(Transaction transaction) throws RemoteException {
        JSONObject json = new JSONObject();

        if (transaction.amount != null) {
            json.put("amount", transaction.amount);
        }
        if (transaction.balance != null) {
            json.put("balance", transaction.balance);
        }
        if (transaction.category != null) {
            json.put("category", transaction.category);
        }
        if (transaction.counterparty != null) {
            json.put("counterparty", counterpartyToJSON(transaction.counterparty));
        }
        if (transaction.created != null) {
            json.put("created", transaction.created);
        }
        if (transaction.declineReason != null) {
            json.put("declineReason", transaction.declineReason);
        }
        if (transaction.description != null) {
            json.put("description", transaction.description);
        }
        if (transaction.id != null) {
            json.put("id", transaction.id);
        }
        if (transaction.localAmount != null) {
            json.put("localAmount", transaction.localAmount);
        }
        if (transaction.localCurrency != null) {
            json.put("localCurrency", transaction.localCurrency);
        }
        if (transaction.mcc != null) {
            json.put("mcc", transaction.mcc);
        }
        if (transaction.providerId != null) {
            json.put("providerId", transaction.providerId);
        }
        if (transaction.settled != null) {
            json.put("settled", transaction.settled);
        }
        if (transaction.type != null) {
            json.put("type", transaction.type);
        }

        return json;
    }

    static JSONArray transactionsToJSON(Transaction[] transactions) throws RemoteException {
        Collections.reverse(Arrays.asList(transactions)); // so the newest transaction is first
        JSONArray json = new JSONArray();
        for (Transaction transaction : transactions) {
            json.put(transactionToJSON(transaction));
        }
        return json;
    }
}
