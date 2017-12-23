package io.yamm.client;

import io.yamm.backend.UserInterface;
import io.yamm.backend.YAMM;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

class GUI implements UserInterface,Runnable {

    // sample from the Oracle tutorials:
    // https://docs.oracle.com/javase/tutorial/uiswing/misc/systemtray.html

    private DataHandler dh;
    private static GUI gui;
    private SystemTray tray;
    private TrayIcon trayIcon;

    public static void main(String[] args) {
        gui = new GUI();
        // try to set an appropriate look and feel for the system
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException|
                    IllegalAccessException|
                    InstantiationException|
                    ClassNotFoundException e) {
            gui.showException(e);
        }
        SwingUtilities.invokeLater(gui);
    }

    public void run() {
        // check for SystemTray support
        if (!SystemTray.isSupported()) {
            showError("SystemTray is not supported!");
            return;
        }

        // create system tray icon
        // see https://stackoverflow.com/a/12287388 for resize code
        // transparency doesn't work on linux: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6453521
        BufferedImage trayIconImage;
        try {
            trayIconImage = ImageIO.read(GUI.class.getResource("/images/icon.png"));
        } catch (IOException e) {
            showError("Could not read icon graphic!");
            return;
        }
        int trayIconWidth = new TrayIcon(trayIconImage).getSize().width;
        trayIcon = new TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH));
        trayIcon.setToolTip("YAMM: Yet Another Money Manager");
        tray = SystemTray.getSystemTray();

        // create popup menu components
        MenuItem aboutItem = new MenuItem("About");
        MenuItem FAQItem = new MenuItem("FAQ");
        MenuItem exitItem = new MenuItem("Exit");

        // add components to popup menu
        final PopupMenu popup = new PopupMenu();
        popup.add(aboutItem);
        popup.add(FAQItem);
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);

        // try to add the icon to the tray
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            showError("TrayIcon could not be added!");
            return;
        }

        // launch the YAMM logic
        YAMM yamm = new YAMM(gui);
        try {
            dh = new DataHandler(gui, yamm);
        } catch (NullPointerException e) { // if crypto couldn't be initialised
            quitWithoutSaving();
        }
        try {
            new Webserver(gui, yamm);
        } catch (IOException e) {
            showException(e);
        }

        // listeners etc.
        trayIcon.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "This dialog box is run from System Tray"));

        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "This dialog box is run from the About menu item"));

        FAQItem.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(java.net.URI.create("https://yamm.io/faq"));
            } catch (IOException ex) {
                showException(ex);
            }
        });

        exitItem.addActionListener(e -> quit());
    }

    public void quit() {
        dh.save();
        quitWithoutSaving();
    }

    void quitWithoutSaving() {
        try {
            dh.overwriteSensitiveData();
        } catch (NullPointerException ignored) {
        }
        tray.remove(trayIcon);
        System.exit(0);
    }

    public String requestFolder() throws NullPointerException {
        JFileChooser f = new JFileChooser();
        f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        f.showDialog(null, "Select Folder");
        return f.getSelectedFile().toString();
    }

    public char[] requestCharArray(String message) {
        JLabel passwordLabel = new JLabel("<html><p style='width:240px'>" + message + "</p></html>");
        JPasswordField password = new JPasswordField();
        password.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                password.setEchoChar('Y'); // signal that we closed due to enter
                password.getRootPane().getParent().setVisible(false);
            }
        });

        int result = JOptionPane.showOptionDialog(
                null,
                new Object[]{passwordLabel, password},
                "Input",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new JButton[] {new JButton("OK"), new JButton("Cancel")},
                null);

        if (result == 0) { // OK
            return password.getPassword();
        } else if (result == 1) { // cancel
            return null;
        } else { // closed
            if (password.getEchoChar() == 'Y') { // if we closed due to enter, return the password
                return password.getPassword();
            } else {
                return null;
            }
        }
    }

    public String requestString(String message) {
        return JOptionPane.showInputDialog("<html><p style='width:240px'>" + message + "</p></html>");
    }

    public void showError(String message) {
        showError("Error", message);
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(
                null,
                "<html><p style='width:240px'>" + message + "</p></html>",
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    public void showException(Exception e) {
        // source: https://www.javalobby.org//java/forums/t19012.html
        // create and configure a text area - fill it with exception text.
        final JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        textArea.setText(writer.toString());

        // stuff it in a scrollpane with a controlled size.
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 240));

        // pass the scrollpane to the joptionpane.
        JOptionPane.showMessageDialog(null, scrollPane, "Exception!", JOptionPane.ERROR_MESSAGE);
    }

    public void showMessage(String message) {
        showMessage("Information", message);
    }

    public void showMessage(String title, String message) {
        JOptionPane.showMessageDialog(
                null,
                "<html><p style='width:240px'>" + message + "</p></html>",
                title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showWarning(String message) {
        showWarning("Warning", message);
    }

    public void showWarning(String title, String message) {
        JOptionPane.showMessageDialog(
                null,
                "<html><p style='width:240px'>" + message + "</p></html>",
                title,
                JOptionPane.WARNING_MESSAGE);
    }
}
