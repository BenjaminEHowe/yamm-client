package io.yamm.client;

import io.yamm.backend.Account;
import io.yamm.backend.BankAccount;
import io.yamm.backend.Transaction;
import io.yamm.backend.YAMM;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
                        for (int j = transactionsJSON.length() - 1; j >= 0; j--) {
                            // the oldest transaction should be the first item in the array
                            transactions[transactionsJSON.length() - (j + 1)] = new Transaction(
                                    transactionsJSON.getJSONObject(j).getLong("amount"),
                                    transactionsJSON.getJSONObject(j).getLong("balance"),
                                    ZonedDateTime.parse(transactionsJSON.getJSONObject(j).getString("created")),
                                    Currency.getInstance(transactionsJSON.getJSONObject(j).getString("currency")),
                                    transactionsJSON.getJSONObject(j).getString("description"),
                                    UUID.fromString(transactionsJSON.getJSONObject(j).getString("id")),
                                    transactionsJSON.getJSONObject(i).getString("providerID")
                            );
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

    private String encryptedRead(String filename) throws IOException {
        InputStream input = new FileInputStream(dataFolder + File.separator + filename);
        String encrypted = new Scanner(input, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        input.close();
        try {
            return encryptor.decrypt(encrypted);
        } catch (EncryptionOperationNotPossibleException e) {
            gui.showError("Incorrect password entered! YAMM will now quit.");
            gui.quitWithoutSaving();
            return ""; // we'll never actually get here due to the previous statement
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

        json.put("amount", transaction.amount);
        json.put("balance", transaction.balance);
        json.put("created", transaction.created.toString());
        json.put("currency", transaction.currency.getCurrencyCode());
        json.put("description", transaction.description);
        json.put("id", transaction.id.toString());
        json.put("providerID", transaction.providerID);

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
