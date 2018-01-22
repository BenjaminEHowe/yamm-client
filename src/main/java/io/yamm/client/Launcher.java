package io.yamm.client;

import com.jdotsoft.jarloader.JarClassLoader;

public class Launcher {
    public static void main(String[] args) {
        JarClassLoader jcl = new JarClassLoader();
        try {
            jcl.invokeMain("io.yamm.client.GUI", args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
