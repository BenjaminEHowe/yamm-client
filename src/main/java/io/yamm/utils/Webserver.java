package io.yamm.utils;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fi.iki.elonen.NanoHTTPD;
import io.yamm.backend.Interface;
import io.yamm.backend.YAMM;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Webserver extends NanoHTTPD {

    private Interface ui;
    private YAMM yamm;

    public Webserver(Interface ui, YAMM yamm) throws IOException {
        super(0);
        this.ui = ui;
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
        // get BBC News headline to demonstrate Unirest
        String headline;
        try {
            String response = Unirest.get("https://feeds.bbci.co.uk/news/rss.xml").asString().getBody();
            String pattern = ".*?<item>.*?<title><!\\[CDATA\\[(.*?)]]></title>.*";
            Pattern r = Pattern.compile(pattern, Pattern.DOTALL);
            Matcher m = r.matcher(response);
            if (m.find()) {
                headline = m.group(1);
            } else {
                headline = "Not found";
            }
        } catch (UnirestException e) {
            ui.showException(e);
            headline = "Unavailable";
        }
        message += "<p>Top BBC News Headline: " + headline + "</p>\n";
        message += "</html>";
        return newFixedLengthResponse(message);
    }
}