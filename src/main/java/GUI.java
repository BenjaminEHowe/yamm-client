import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class GUI {

    // sample from the Oracle tutorials:
    // https://docs.oracle.com/javase/tutorial/uiswing/misc/systemtray.html

    public static void main(String[] args) {
        // try to set an appropriate look and feel for the system
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException|
                    IllegalAccessException|
                    InstantiationException|
                    ClassNotFoundException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(GUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        // check for SystemTray support
        if (!SystemTray.isSupported()) {
            System.err.println("SystemTray is not supported.");
            return;
        }

        // create system tray icon
        // see https://stackoverflow.com/a/12287388 for resize code
        // transparency doesn't work on linux: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6453521
        BufferedImage trayIconImage;
        try {
            trayIconImage = ImageIO.read(GUI.class.getResource("images/icon.png"));
        } catch (IOException e) {
            System.err.println("Could not read icon graphic.");
            e.printStackTrace();
            return;
        }
        int trayIconWidth = new TrayIcon(trayIconImage).getSize().width;
        final TrayIcon trayIcon = new TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH));
        trayIcon.setToolTip("YAMM: Yet Another Money Manager");
        final SystemTray tray = SystemTray.getSystemTray();

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

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("TrayIcon could not be added.");
            return;
        }

        trayIcon.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "This dialog box is run from System Tray"));

        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "This dialog box is run from the About menu item"));

        ActionListener listener = e -> {
            MenuItem item = (MenuItem)e.getSource();
            //TrayIcon.MessageType type = null;
            System.out.println(item.getLabel());
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
            tray.remove(trayIcon);
            System.exit(0);
        });
    }
}
