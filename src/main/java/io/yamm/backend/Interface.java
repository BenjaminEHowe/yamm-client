package io.yamm.backend;

public interface Interface {
    void quit();
    String requestFolder();
    void showError(String message);
    void showError(String title, String message);
    void showMessage(String message);
    void showMessage(String title, String message);
    void showWarning(String message);
    void showWarning(String title, String message);
}
