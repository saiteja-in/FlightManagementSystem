package com.saiteja.flightservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FlightSearchRequest {

    @NotBlank(message = "Origin airport is required")
    private String originAirport;

    @NotBlank(message = "Destination airport is required")
    private String destinationAirport;

    @NotNull(message = "Flight date is required")
    private LocalDate flightDate;
}

