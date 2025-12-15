package com.saiteja.flightservice.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AirportTests {

    @Test
    void from_acceptsLowercaseAndTrims() {
        assertEquals(Airport.DEL, Airport.from("  del "));
        assertEquals(Airport.GOI, Airport.from("goi"));
    }

    @Test
    void from_rejectsInvalidCode() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Airport.from("XYZ"));
        assertEquals("Invalid airport code: XYZ", ex.getMessage());
    }

    @Test
    void from_rejectsNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> Airport.from(null));
        assertThrows(IllegalArgumentException.class, () -> Airport.from("  "));
    }
}
