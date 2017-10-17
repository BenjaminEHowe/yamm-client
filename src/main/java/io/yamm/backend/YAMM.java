package io.yamm.backend;

import java.io.*;
import java.util.Properties;

public class YAMM {
    private final Interface ui;

    public YAMM(Interface ui) {
        this.ui = ui;

        // see http://www.mkyong.com/java/java-properties-file-examples
        Properties prop = new Properties();
        String dataFolder = null;
        if (new File("config.properties").exists()) {
            // load existing config
            InputStream input = null;
            try {
                input = new FileInputStream("config.properties");
                prop.load(input);
                dataFolder = prop.getProperty("dataFolder");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (dataFolder == null) {
            // if no directory is specified, ask the user for one
            ui.showWarning(
                    "Data not found!",
                    "No data folder was found. Please select a folder to store YAMM data in.");
            try {
                dataFolder = ui.requestFolder();
            } catch (NullPointerException e) {
                ui.showError("No folder selected! YAMM will now quit.");
                ui.quit();
            }
        }
        ui.showMessage("Data folder", "Data folder is " + dataFolder);
        // write new config
        OutputStream output = null;
        try {
            output = new FileOutputStream("config.properties");
            prop.setProperty("dataFolder", dataFolder);
            prop.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
