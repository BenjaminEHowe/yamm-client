package io.yamm.backend;

import java.net.URL;

public class Counterparty {
    public final String accountNumber;
    public final Address address;
    public final URL icon;
    public final String name;
    public final String sortCode;
    public final URL website;

    public Counterparty(String accountNumber,
                        Address address,
                        URL icon,
                        String name,
                        String sortCode,
                        URL website) {
        this.accountNumber = accountNumber;
        this.address = address;
        this.icon = icon;
        this.name = name;
        this.sortCode = sortCode;
        this.website = website;
    }
}
