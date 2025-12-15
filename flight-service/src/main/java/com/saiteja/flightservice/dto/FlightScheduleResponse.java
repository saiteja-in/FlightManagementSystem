package com.saiteja.flightservice.dto;

import com.saiteja.flightservice.model.enums.Airport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightScheduleResponse {
    private String scheduleId;
    private String flightId;
    private String flightNumber;
    private String airline;
    private Airport originAirport;
    private Airport destinationAirport;
    private LocalDate flightDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private BigDecimal fare;
    private Integer totalSeats;
    private Integer availableSeats;
    private String status;
}
