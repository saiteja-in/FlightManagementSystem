package com.saiteja.flightservice.dto;

import com.saiteja.flightservice.model.enums.Airport;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FlightSearchRequest {

    @NotNull(message = "Origin airport is required")
    private Airport originAirport;

    @NotNull(message = "Destination airport is required")
    private Airport destinationAirport;

    @NotNull(message = "Flight date is required")
    private LocalDate flightDate;
}

