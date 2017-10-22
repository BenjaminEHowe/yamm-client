package io.yamm.backend;

import com.mashape.unirest.http.Unirest;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CancellationException;

public class YAMM {
    private Map<UUID, Account> accounts  = new HashMap<>();
    private String dataFolder = null;
    private Random random;
    private final UserInterface ui;

    public YAMM(UserInterface ui) {
        this.ui = ui;
        Unirest.setDefaultHeader("User-Agent", "YAMMBot/" + getVersion() + "; +https://yamm.io/bot");
        random = new SecureRandom();
    }

    public void addAccount(String providerSlug) throws CancellationException, ClassNotFoundException, Exception {
        // get the provider class, or throw ClassNotFoundException
        Class<?> provider = Class.forName("io.yamm.backend.providers." + providerSlug);

        // get the "nice" name of the provider and the credentials it requires
        String name;
        String[] requiredCredentials;
        try {
            name = (String) provider.getDeclaredField("name").get(null);
            requiredCredentials = (String[]) provider.getDeclaredField("requiredCredentials").get(null);
        } catch (IllegalAccessException|NoSuchFieldException e) {
            ui.showException(e);
            return;
        }

        // ask the user for each credential
        char[][] credentials = new char[requiredCredentials.length][];
        try {
            for (int i = 0; i < requiredCredentials.length; i++) {
                credentials[i] = ui.requestCharArray(
                        "Please enter your " + name + " " + requiredCredentials[i] + ":");
            }
        } catch (NullPointerException e) {
            throw new CancellationException();
        }

        // try to instantiate the object & add it to the accounts list
        try {
            Account account = (Account) provider.getConstructor(char[][].class, YAMM.class)
                    .newInstance(credentials, this);
            accounts.put(account.getUUID(), account);
        } catch (InstantiationException|
                IllegalAccessException|
                InvocationTargetException|
                NoSuchMethodException|
                NullPointerException e) {
            throw new Exception("Error instantiating account!", e);
        }
    }

    public char[] generateSecureRandom(int length) {
        assert length > 0;
        final String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        char[] buffer = new char[length];
        for (int i = 0; i < buffer.length; ++i)
            buffer[i] = symbols.charAt(random.nextInt(symbols.length()));
        return buffer;
    }

    public Map<UUID, Account> getAccounts() {
        return accounts;
    }

    public String getVersion() {
        return "0.1";
    }

    public void raiseException(Exception e) {
        ui.showException(e);
    }

    public static long secondsBetween(Date d1, Date d2) {
        return Math.abs((d2.getTime()-d1.getTime())/1000);
    }
}
