package io.yamm.utils;

import fi.iki.elonen.NanoHTTPD;
import io.yamm.backend.YAMM;

import java.io.IOException;

public class Webserver extends NanoHTTPD {

    private YAMM yamm;

    public Webserver(YAMM yamm) throws IOException {
        super(0);
        this.yamm = yamm;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        java.awt.Desktop.getDesktop().browse(java.net.URI.create("http://localhost:" + getListeningPort()));
    }

    @Override
    public Response serve(IHTTPSession session) {
        String message = "<!DOCTYPE html>\n<html>\n<title>YAMM Test Page</title>\n";
        message += "<p>URI: " + session.getUri() + "</p>\n";
        message += "<p>Parameters: " + session.getParameters().toString() + "</p>\n";
        message += "<p>Method: " + session.getMethod().toString() + "</p>\n";
        message += "</html>";
        return newFixedLengthResponse(message);
    }
}