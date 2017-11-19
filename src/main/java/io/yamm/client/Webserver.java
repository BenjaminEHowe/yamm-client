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
        char[] auth = YAMM.generateRandom(random, 24);

        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        java.awt.Desktop.getDesktop().browse(java.net.URI.create(
                "https://alpha.yamm.io/app/#port=" + getListeningPort() + ",secret=" + new String(auth)));

        // replace auth code with a hash
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            authHash = digest.digest(new String(auth).getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            ui.showException(e);
        }
        //noinspection UnusedAssignment securely overwrite auth code
        auth = YAMM.generateRandom(random, 24);
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
                return CORSify(session, newFixedLengthResponse(Response.Status.FORBIDDEN,
                        "application/json",
                        json.toString()));
            }
        } catch (NoSuchAlgorithmException e) {
            ui.showException(e);
            return CORSify(session, newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                    "application/json",
                    json.toString()));
        } catch (NullPointerException e) {
            json.put("message", "Key not supplied.");
            return CORSify(session, newFixedLengthResponse(Response.Status.UNAUTHORIZED,
                    "application/json",
                    json.toString()));
        }

        // for now, only support API v1
        if (!URIParts[0].equals("v1")) {
            json.put("message", "Only API v1 is supported.");
            return CORSify(session, newFixedLengthResponse(Response.Status.NOT_FOUND,
                    "application/json",
                    json.toString()));
        }

        // check the URI contains an endpoint
        if (URIParts.length < 2) {
            json.put("message", "Endpoint not specified.");
            return CORSify(session, newFixedLengthResponse(Response.Status.NOT_FOUND,
                    "application/json",
                    json.toString()));
        }

        // do something!
        switch (URIParts[1]) {
            case "accounts":
                // if an account has been specified
                if (URIParts.length > 2) {
                    // if an operation has been specified
                    if (URIParts.length > 3) {
                        switch(URIParts[3]) {
                            case "transactions":
                                try {
                                    JSONArray transactions = DataHandler.transactionsToJSON(yamm.getAccounts().get(UUID.fromString(URIParts[2])).getTransactions());
                                    return CORSify(session, newFixedLengthResponse(Response.Status.OK,
                                            "application/json",
                                            transactions.toString()));
                                } catch (NullPointerException e) {
                                    yamm.raiseException(e);
                                    json.put("message", "The requested account was not found.");
                                    return CORSify(session, newFixedLengthResponse(Response.Status.NOT_FOUND,
                                            "application/json",
                                            json.toString()));
                                } catch (RemoteException e) {
                                    yamm.raiseException(e);
                                    return CORSify(session, newFixedLengthResponse(Response.Status.NOT_FOUND,
                                            "application/json",
                                            json.toString()));
                                }

                            default:
                                return CORSify(session, NotFound());
                        }
                    } else {
                        switch (session.getMethod()) {
                            case GET:
                                try {
                                    JSONObject account = DataHandler.accountToJSON(yamm.getAccounts().get(UUID.fromString(URIParts[2])));
                                    return CORSify(session, newFixedLengthResponse(Response.Status.OK,
                                            "application/json",
                                            account.toString()));
                                } catch (NullPointerException e) {
                                    json.put("message", "The requested account was not found.");
                                    return CORSify(session, newFixedLengthResponse(Response.Status.NOT_FOUND,
                                            "application/json",
                                            json.toString()));
                                } catch (RemoteException e) {
                                    yamm.raiseException(e);
                                    return CORSify(session, newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                            "application/json",
                                            json.toString()));
                                }

                            default:
                                return CORSify(session, MethodNotAllowed());
                        }
                    }
                } else {
                    switch (session.getMethod()) {
                        case GET:
                            try {
                                JSONArray accounts = DataHandler.accountsToJSON(yamm.getAccounts());
                                return CORSify(session, newFixedLengthResponse(Response.Status.OK,
                                        "application/json",
                                        accounts.toString()));
                            } catch (RemoteException e) {
                                yamm.raiseException(e);
                                return CORSify(session, newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                        "application/json",
                                        json.toString()));
                            }

                        default:
                            return CORSify(session, MethodNotAllowed());
                    }
                }

            case "account-requests":
                switch (session.getMethod()) {
                    case GET:
                        return CORSify(session, NotImplemented());

                    case POST:
                        final HashMap<String, String> map = new HashMap<>();
                        try {
                            session.parseBody(map);
                        } catch (IOException|ResponseException e) {
                            ui.showException(e);
                        }
                        try {
                            String id = yamm.addAccount(
                                    new JSONObject(map.get("postData")).getString("provider")).toString();
                            json.put("message", "Account " + id + " created.");
                            return CORSify(session, newFixedLengthResponse(Response.Status.CREATED,
                                    "application/json",
                                    json.toString()));
                        } catch (CancellationException e) {
                            json.put("message", "User aborted account request.");
                            return CORSify(session, newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                    "application/json",
                                    json.toString()));
                        } catch (ClassNotFoundException e) {
                            json.put("message", "The requested provider was not found.");
                            return CORSify(session, newFixedLengthResponse(Response.Status.NOT_FOUND,
                                    "application/json",
                                    json.toString()));
                        } catch (Exception e) {
                            new Thread(() -> ui.showException(e)).start();
                            json.put("message", "Unknown exception.");
                            return CORSify(session, newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                    "application/json",
                                    json.toString()));
                        }

                    default:
                        return CORSify(session, MethodNotAllowed());
                }

            case "about":
                switch (session.getMethod()) {
                    case GET:
                        json.put("version", yamm.getVersion());
                        return CORSify(session, newFixedLengthResponse(Response.Status.OK,
                                "application/json",
                                json.toString()));

                    default:
                        return CORSify(session, MethodNotAllowed());
                }

            default:
                return CORSify(session, NotFound());
        }
    }

    private Response CORSify(IHTTPSession session, Response response) {
        String origin = session.getHeaders().get("origin");
        if (origin.endsWith("yamm.io")) {
            response.addHeader("Access-Control-Allow-Origin", origin);
        }
        response.addHeader("Vary", "Origin");
        return response;
    }

    private Response MethodNotAllowed() {
        JSONObject json = new JSONObject();
        json.put("message",
                "Endpoint exists but does not support method.");
        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                "application/json",
                json.toString());
    }

    private Response NotFound() {
        JSONObject json = new JSONObject();
        json.put("message", "Endpoint not found.");
        return newFixedLengthResponse(Response.Status.NOT_FOUND,
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