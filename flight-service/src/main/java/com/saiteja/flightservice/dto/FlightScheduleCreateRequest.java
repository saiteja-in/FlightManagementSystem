package com.saiteja.flightservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class FlightScheduleCreateRequest {

    @NotBlank(message = "Flight number is required")
    private String flightNumber;

    @NotNull(message = "Flight date is required")
    @FutureOrPresent(message = "Flight date must be today or future")
    private LocalDate flightDate;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;

    @NotNull(message = "Fare is required")
    @DecimalMin(value = "1.00", message = "Fare must be at least 1.00")
    private BigDecimal fare;
}

