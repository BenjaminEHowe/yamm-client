package io.yamm.backend.providers;

import io.yamm.backend.YAMM;

import java.rmi.RemoteException;

@SuppressWarnings("unused") // instantiated by YAMM by reflection
public class NewDay_Topman extends NewDay {
    public static final String name = "Topman Card";

    public NewDay_Topman(char[][] credentials, YAMM yamm) throws RemoteException {
        super(credentials, yamm);
    }

    protected String getSlug() {
        return "topman";
    }
}
