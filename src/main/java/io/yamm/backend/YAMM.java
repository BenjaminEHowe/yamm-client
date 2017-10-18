package io.yamm.backend;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import java.io.*;
import java.security.Security;
import java.util.Properties;

public class YAMM {
    private String dataFolder = null;
    private StandardPBEStringEncryptor encryptor;
    private Properties prop;
    private final Interface ui;

    public YAMM(Interface ui) {
        Security.setProperty("crypto.policy", "unlimited");
        this.ui = ui;

        // see http://www.mkyong.com/java/java-properties-file-examples
        prop = new Properties();
        if (new File("config.properties").exists()) {
            // load existing config
            InputStream input = null;
            try {
                input = new FileInputStream("config.properties");
                prop.load(input);
                dataFolder = prop.getProperty("dataFolder");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (dataFolder == null) {
            // if no directory is specified, ask the user for one
            ui.showWarning(
                    "Data not found!",
                    "No data folder was found. Please select a folder to store YAMM data in.");
            try {
                dataFolder = ui.requestFolder();
            } catch (NullPointerException e) {
                ui.showError("No folder selected! YAMM will now quit.");
                ui.quit();
            }
        }
        ui.showMessage("Data folder", "Data folder is " + dataFolder);

        // check if data exists
        boolean dataExists = false;
        if (new File(dataFolder + File.separator + "cryptoTest.txt").exists()) {
            dataExists = true;
        }

        // request the password
        // TODO: CHANGE THIS TO AN ARRAY OF CHARS - STRINGS ARE IMMUTABLE SO CANNOT BE ZEROED!
        String password = "";
        while (password.length() < 12) {
            try {
                if (dataExists) {
                    password = ui.requestString("Please enter your YAMM encryption password:");
                } else {
                    password = ui.requestString("Please enter a password (minimum length 12 characters) to use to encrypt your YAMM data:");
                }
                if (password.length() < 12) {
                    ui.showError("Password must be greater than 12 characters!");
                } else {
                    break;
                }
            } catch (NullPointerException e) {
                if (dataExists) {
                    ui.showError("A password is required to decrypt YAMM data. YAMM will now quit.");
                } else {
                    ui.showError("A password is required to encrypt YAMM data. YAMM will now quit.");
                }
                quit();
            }
        }
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setProvider(new BouncyCastleProvider());
        encryptor.setAlgorithm("PBEWITHSHA256AND256BITAES-CBC-BC");
        encryptor.setPassword(password);

        // check that we can do crypto
        try {
            encryptor.encrypt("");
        } catch (EncryptionOperationNotPossibleException e) {
            ui.showError("Unable to initialise YAMM cryptography! This is probably because the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files have not been installed. YAMM will now quit.");
            quit();
        }

        // write or read
        if (dataExists) {
            String encryptedString = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(dataFolder + File.separator + "cryptoTest.txt"));
                encryptedString = reader.readLine();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                String decryptedString = encryptor.decrypt(encryptedString);
                ui.showMessage("cryptoTest file read: " + decryptedString);
            } catch (EncryptionOperationNotPossibleException e) {
                ui.showError("Incorrect password specified! YAMM will now quit.");
                // TODO: make sure this doesn't re-save data using the incorrect password
                quit();
            }
        } else {
            String encryptedString = encryptor.encrypt("test secret message");
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(dataFolder + File.separator + "cryptoTest.txt"));
                writer.write(encryptedString);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ui.showMessage("cryptoTest file saved.");
        }
        saveProperties();
    }

    private void saveProperties() {
        OutputStream output = null;
        try {
            output = new FileOutputStream("config.properties");
            prop.setProperty("dataFolder", dataFolder);
            prop.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void quit() {
        saveProperties();
        ui.quit();
    }
}
