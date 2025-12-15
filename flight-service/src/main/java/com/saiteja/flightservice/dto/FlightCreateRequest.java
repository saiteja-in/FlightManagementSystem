package com.saiteja.flightservice.dto;

import com.saiteja.flightservice.model.enums.Airline;
import com.saiteja.flightservice.model.enums.Airport;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class FlightCreateRequest {

    @NotBlank(message = "Flight number is required")
    private String flightNumber;

    @NotNull(message = "Airline is required")
    private Airline airline;

    @NotNull(message = "Origin airport is required")
    private Airport originAirport;

    @NotNull(message = "Destination airport is required")
    private Airport destinationAirport;

    @NotNull(message = "Seat capacity is required")
    @Min(value = 1, message = "Seat capacity must be at least 1")
    @Max(value = 1000, message = "Seat capacity cannot exceed 1000")
    private Integer seatCapacity;
}

