package io.yamm.backend;

import com.mashape.unirest.http.Unirest;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;

import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CancellationException;

public class YAMM {
    private Map<UUID, Account> accounts  = new HashMap<>();
    private final BasicCookieStore cookieStore = new BasicCookieStore();
    private Random random = new SecureRandom();
    private final UserInterface ui;

    public YAMM(UserInterface ui) {
        this.ui = ui;
        Unirest.setDefaultHeader("User-Agent", "YAMMBot/" + getVersion() + "; +https://yamm.io/bot");
        Unirest.setHttpClient(HttpClients.custom().
                setDefaultCookieStore(cookieStore)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build());
    }

    public void addAccount(Account account) {
        accounts.put(account.getUUID(), account);
    }

    public UUID addAccount(String providerSlug) throws Exception {
        // get the provider class, or throw ClassNotFoundException
        Class<?> provider = Class.forName("io.yamm.backend.providers." + providerSlug);

        // get the "nice" name of the provider and the credentials it requires
        String name;
        String[] requiredCredentials;
        try {
            name = (String) provider.getDeclaredField("name").get(null);
            // try to get the required credentials
            try {
                requiredCredentials = (String[]) provider.getDeclaredField("requiredCredentials").get(null);
            } catch (NoSuchFieldException e) {
                // if not from the class, from the superclass
                requiredCredentials = (String[]) provider.getSuperclass().getDeclaredField("requiredCredentials").get(null);
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            ui.showException(e);
            return null;
        }

        // ask the user for each credential
        char[][] credentials = new char[requiredCredentials.length][];
        for (int i = 0; i < requiredCredentials.length; i++) {
            credentials[i] = ui.requestCharArray(
                    "Please enter your " + name + " " + requiredCredentials[i] + ":");
        }

        // try to instantiate the object & add it to the accounts list
        try {
            Account account = (Account) provider.getConstructor(char[][].class, YAMM.class)
                    .newInstance(credentials, this);
            UUID accountID = account.getUUID();
            accounts.put(accountID, account);
            return accountID;
        } catch (InvocationTargetException e) {
            // if invalid credentials were supplied, tell the web server
            if (e.getCause() instanceof InvalidCredentialsException) {
                throw (InvalidCredentialsException) e.getCause();
            } else {
                throw new Exception("Error instantiating account!", e);
            }
        } catch (InstantiationException|
                IllegalAccessException|
                NoSuchMethodException|
                NullPointerException e) {
            throw new Exception("Error instantiating account!", e);
        }
    }

    public char[] generateSecureRandom(int length) {
        return YAMM.generateRandom(random, length);
    }

    public static char[] generateRandom(Random random, int length) {
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

    private List<Cookie> getCookie(String domain) {
        List<Cookie> cookies = new ArrayList<>();
        for (Cookie cookie: cookieStore.getCookies()) {
            if (cookie.getDomain().equals(domain)) {
                cookies.add(cookie);
            }
        }
        return cookies;
    }

    public String getCookie(String name, String domain) {
        return getCookie(name, domain, "/");
    }

    public String getCookie(String name, String domain, String path) {
        // clear any expired cookies
        cookieStore.clearExpired(new Date());

        // iterate over the cookieStore, looking for a matching cookie
        for (Cookie cookie: cookieStore.getCookies()) {
            if (cookie.getDomain().equals(domain) &&
                    cookie.getPath().equals(path) &&
                    cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }

        // if we get this far, no matching cookie was found :-(
        return null;
    }

    public String getVersion() {
        return "0.1";
    }

    public void overwriteSensitiveData() {
        for (Map.Entry<UUID, Account> account : getAccounts().entrySet()) {
            account.getValue().overwriteSensitiveData();
        }
    }

    public void raiseException(Exception e) {
        ui.showException(e);
    }
}
