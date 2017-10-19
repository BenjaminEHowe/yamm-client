package io.yamm.backend;

public interface Interface {
    void quit();
    String requestFolder() throws NullPointerException;
    String requestString(String message);
    void showError(String message);
    void showError(String title, String message);
    void showException(Exception e);
    void showMessage(String message);
    void showMessage(String title, String message);
    void showWarning(String message);
    void showWarning(String title, String message);
}
