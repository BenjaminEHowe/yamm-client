package io.yamm.backend;

public class YAMMRuntimeException extends Exception {
    public YAMMRuntimeException(String message) {
        super(message);
    }

    public YAMMRuntimeException(String message, Exception e) {
        super(message, e);
    }
}
