package com.saiteja.flightservice.dto;

import com.saiteja.flightservice.model.enums.Airline;
import com.saiteja.flightservice.model.enums.Airport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightResponse {
    private String id;
    private String flightNumber;
    private Airline airline;
    private Airport originAirport;
    private Airport destinationAirport;
    private Integer seatCapacity;
}

