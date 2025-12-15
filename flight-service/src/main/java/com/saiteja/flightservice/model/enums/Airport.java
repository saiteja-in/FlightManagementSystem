package com.saiteja.flightservice.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Airport {
    DEL, // Delhi
    HYD, // Hyderabad
    BLR, // Bengaluru
    BOM, // Mumbai
    MAA, // Chennai
    CCU, // Kolkata
    GOI, // Goa (Dabolim/Mopa)
    AMD, // Ahmedabad
    PNQ, // Pune
    COK; // Kochi

    @JsonCreator
    public static Airport from(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Airport code is required");
        }
        try {
            return Airport.valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid airport code: " + code);
        }
    }

    @JsonValue
    public String getCode() {
        return name();
    }
}
