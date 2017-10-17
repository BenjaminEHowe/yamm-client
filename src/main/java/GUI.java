import io.yamm.backend.Interface;
import io.yamm.backend.YAMM;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class GUI implements Interface,Runnable {

    // sample from the Oracle tutorials:
    // https://docs.oracle.com/javase/tutorial/uiswing/misc/systemtray.html

    private SystemTray tray;
    private TrayIcon trayIcon;

    public static void main(String[] args) {
        GUI gui = new GUI();
        // try to set an appropriate look and feel for the system
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException|
                    IllegalAccessException|
                    InstantiationException|
                    ClassNotFoundException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(gui);
        new YAMM(gui);
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
        Menu displayMenu = new Menu("Display");
        MenuItem errorItem = new MenuItem("Error");
        MenuItem warningItem = new MenuItem("Warning");
        MenuItem infoItem = new MenuItem("Info");
        MenuItem noneItem = new MenuItem("None");
        MenuItem exitItem = new MenuItem("Exit");

        // add components to popup menu
        final PopupMenu popup = new PopupMenu();
        popup.add(aboutItem);
        popup.add(displayMenu);
        displayMenu.add(errorItem);
        displayMenu.add(warningItem);
        displayMenu.add(infoItem);
        displayMenu.add(noneItem);
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);

        // try to add the icon to the tray
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            showError("TrayIcon could not be added!");
            return;
        }

        trayIcon.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "This dialog box is run from System Tray"));

        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "This dialog box is run from the About menu item"));

        ActionListener listener = e -> {
            MenuItem item = (MenuItem)e.getSource();
            if ("Error".equals(item.getLabel())) {
                //type = TrayIcon.MessageType.ERROR;
                trayIcon.displayMessage("Sun TrayIcon Demo",
                        "This is an error message", TrayIcon.MessageType.ERROR);

            } else if ("Warning".equals(item.getLabel())) {
                //type = TrayIcon.MessageType.WARNING;
                trayIcon.displayMessage("Sun TrayIcon Demo",
                        "This is a warning message", TrayIcon.MessageType.WARNING);

            } else if ("Info".equals(item.getLabel())) {
                //type = TrayIcon.MessageType.INFO;
                trayIcon.displayMessage("Sun TrayIcon Demo",
                        "This is an info message", TrayIcon.MessageType.INFO);

            } else if ("None".equals(item.getLabel())) {
                //type = TrayIcon.MessageType.NONE;
                trayIcon.displayMessage("Sun TrayIcon Demo",
                        "This is an ordinary message", TrayIcon.MessageType.NONE);
            }
        };

        errorItem.addActionListener(listener);
        warningItem.addActionListener(listener);
        infoItem.addActionListener(listener);
        noneItem.addActionListener(listener);

        exitItem.addActionListener(e -> {
            quit();
        });
    }

    public void quit() {
        tray.remove(trayIcon);
        System.exit(0);
    }

    public String requestFolder() throws NullPointerException {
        JFileChooser f = new JFileChooser();
        f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        f.showDialog(null, "Select Folder");
        return f.getSelectedFile().toString();
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
