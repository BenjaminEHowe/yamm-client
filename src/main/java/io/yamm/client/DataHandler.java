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
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.*;

class DataHandler {
    private String dataFolder = null;
    private StandardPBEStringEncryptor encryptor;
    private final GUI gui;
    private Properties prop = new Properties();
    private final YAMM yamm;

    DataHandler(GUI gui, YAMM yamm) {
        this.gui = gui;
        this.yamm = yamm;
        Security.setProperty("crypto.policy", "unlimited");

        // see http://www.mkyong.com/java/java-properties-file-examples
        if (new File("config.properties").exists()) {
            // load existing config
            InputStream input = null;
            try {
                input = new FileInputStream("config.properties");
                prop.load(input);
                dataFolder = prop.getProperty("dataFolder");
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

        if (dataFolder == null) {
            // if no directory is specified, ask the user for one
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
        if (new File(dataFolder + File.separator + "cryptoTest.txt").exists()) {
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
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
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

        // write or read
        if (dataExists) {
            String encryptedString = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(dataFolder + File.separator + "cryptoTest.txt"));
                encryptedString = reader.readLine();
                reader.close();
            } catch (IOException e) {
                gui.showException(e);
            }
            try {
                String decryptedString = encryptor.decrypt(encryptedString);
                gui.showMessage("cryptoTest file read: " + decryptedString);
            } catch (EncryptionOperationNotPossibleException e) {
                gui.showError("Incorrect password specified! YAMM will now quit.");
                saveProperties();
                gui.quitWithoutSaving();
            }
        } else {
            String encryptedString = encryptor.encrypt("test secret message");
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(dataFolder + File.separator + "cryptoTest.txt"));
                writer.write(encryptedString);
                writer.close();
            } catch (IOException e) {
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
            json.put("BIC", ((BankAccount) account).getBIC());
            json.put("IBAN", ((BankAccount) account).getIBAN());
            json.put("sortCode", ((BankAccount) account).getSortCode());
        }

        return json;
    }

    void overwriteSensitiveData() {
        yamm.overwriteSensitiveData();
    }

    void save() {
        saveData();
        saveProperties();
    }

    private void saveData() { // TODO: implement this
    }

    private void saveProperties() {
        OutputStream output = null;
        try {
            output = new FileOutputStream("config.properties");
            prop.setProperty("dataFolder", dataFolder);
            prop.store(output, null);
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
