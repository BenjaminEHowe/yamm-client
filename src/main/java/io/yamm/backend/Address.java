package io.yamm.backend;

import com.neovisionaries.i18n.CountryCode;

public class Address {
    public final Boolean approximate;
    public final String city;
    public final CountryCode country;
    public final String county;
    public final Double latitude;
    public final Double longitude;
    public final String postcode;
    public final String streetAddress;

    public Address(Boolean approximate,
                   String city,
                   CountryCode country,
                   String county,
                   Double latitude,
                   Double longitude,
                   String postcode,
                   String streetAddress) {
        this.approximate = approximate;
        this.city = city;
        this.country = country;
        this.county = county;
        this.latitude = latitude;
        this.longitude = longitude;
        this.postcode = postcode;
        this.streetAddress = streetAddress;
    }
}
