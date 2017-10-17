package io.yamm.backend;

import java.io.*;
import java.util.Properties;

public class YAMM {
    private final Interface ui;

    public YAMM(Interface ui) {
        this.ui = ui;

        // see http://www.mkyong.com/java/java-properties-file-examples
        Properties prop = new Properties();
        if (new File("config.properties").exists()) {
            // load existing config
            InputStream input = null;
            try {
                input = new FileInputStream("config.properties");
                prop.load(input);
                // get the property value and print it out
                System.out.println(prop.getProperty("configDirectory"));
            } catch (IOException ex) {
                ex.printStackTrace();
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
        OutputStream output = null;
        try {
            output = new FileOutputStream("config.properties");
            String directory = ui.requestDirectory();
            // set the properties value
            prop.setProperty("configDirectory", directory);
            // save properties to project root folder
            prop.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
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
