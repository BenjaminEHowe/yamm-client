package io.yamm.client;

import fi.iki.elonen.NanoHTTPD;
import io.yamm.backend.Account;
import io.yamm.backend.UserInterface;
import io.yamm.backend.YAMM;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CancellationException;

class Webserver extends NanoHTTPD {
    private byte[] authHash;
    private UserInterface ui;
    private YAMM yamm;

    Webserver(UserInterface ui, YAMM yamm) throws IOException {
        super(0);
        this.ui = ui;
        this.yamm = yamm;
        Random random = new SecureRandom();

        // generate auth code
        char[] auth = DataHandler.generateRandom(random, 24);

        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        java.awt.Desktop.getDesktop().browse(java.net.URI.create("http://localhost:" + getListeningPort() + "/v1/about?auth=" + new String(auth)));

        // replace auth code with a hash
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            authHash = digest.digest(new String(auth).getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            ui.showException(e);
        }
        auth = DataHandler.generateRandom(random, 24); // securely overwrite auth code
    }

    @Override
    public Response serve(IHTTPSession session) {
        JSONObject json = new JSONObject();
        String[] URIParts = session.getUri().substring(1).split("/");

        // check session ID
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] candidateAuthHash = digest.digest(session.getParameters().get("auth").get(0).getBytes(StandardCharsets.US_ASCII));
            if (!Arrays.equals(candidateAuthHash, authHash)) {
                json.put("message", "Key not authorised.");
                return newFixedLengthResponse(Response.Status.FORBIDDEN,
                        "application/json",
                        json.toString());
            }
        } catch (NoSuchAlgorithmException e) {
            ui.showException(e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                    "application/json",
                    json.toString());
        } catch (NullPointerException e) {
            json.put("message", "Key not supplied.");
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED,
                    "application/json",
                    json.toString());
        }

        // for now, only support API v1
        if (!URIParts[0].equals("v1")) {
            json.put("message", "Only API v1 is supported.");
            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                    "application/json",
                    json.toString());
        }

        // check the URI contains an endpoint
        if (URIParts.length < 2) {
            json.put("message", "Endpoint not specified.");
            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                    "application/json",
                    json.toString());
        }

        // do something!
        switch (URIParts[1]) {
            case "accounts":
                switch (session.getMethod()) {
                    case GET:
                        JSONArray accounts = new JSONArray();
                        for (Map.Entry<UUID, Account> account : yamm.getAccounts().entrySet()) {
                            try {
                                accounts.put(DataHandler.accountToJSON(account.getValue()));
                            } catch (RemoteException e) {
                                yamm.raiseException(e);
                            }
                        }
                        return newFixedLengthResponse(Response.Status.OK,
                                "application/json",
                                accounts.toString());

                    default:
                        return MethodNotAllowed();
                }

            case "account-requests":
                switch (session.getMethod()) {
                    case GET:
                        return NotImplemented();

                    case POST:
                        final HashMap<String, String> map = new HashMap<>();
                        try {
                            session.parseBody(map);
                        } catch (IOException|ResponseException e) {
                            ui.showException(e);
                        }
                        try {
                            yamm.addAccount(new JSONObject(map.get("postData")).getString("provider"));
                            json.put("message", "Account created.");
                            return newFixedLengthResponse(Response.Status.CREATED,
                                    "application/json",
                                    json.toString());
                        } catch (CancellationException e) {
                            json.put("message", "User aborted account request.");
                            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                    "application/json",
                                    json.toString());
                        } catch (ClassNotFoundException e) {
                            json.put("message", "The requested provider was not found.");
                            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                                    "application/json",
                                    json.toString());
                        } catch (Exception e) {
                            new Thread(() -> { ui.showException(e); }).start();
                            json.put("message", "Unknown exception.");
                            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                    "application/json",
                                    json.toString());
                        }

                    default:
                        return MethodNotAllowed();
                }

            case "about":
                switch (session.getMethod()) {
                    case GET:
                        json.put("version", yamm.getVersion());
                        return newFixedLengthResponse(Response.Status.OK,
                                "application/json",
                                json.toString());

                    default:
                        return MethodNotAllowed();
                }

            default:
                json.put("message", "Endpoint not found.");
                return newFixedLengthResponse(Response.Status.NOT_FOUND,
                        "application/json",
                        json.toString());
        }
    }

    private Response MethodNotAllowed() {
        JSONObject json = new JSONObject();
        json.put("message",
                "Endpoint exists but does not support method.");
        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                "application/json",
                json.toString());
    }

    private Response NotImplemented() {
        JSONObject json = new JSONObject();
        json.put("message",
                "Endpoint exists and supports method but has not yet been implemented.");
        return newFixedLengthResponse(Response.Status.NOT_IMPLEMENTED,
                "application/json",
                json.toString());
    }
}