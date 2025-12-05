package com.saiteja.flightservice.service;

import com.saiteja.flightservice.dto.ApiResponse;
import com.saiteja.flightservice.dto.FlightCreateRequest;
import com.saiteja.flightservice.dto.FlightResponse;

import java.util.List;

public interface FlightService {
    ApiResponse createFlight(FlightCreateRequest request);
    List<FlightResponse> getAllFlights();
    FlightResponse getFlightByFlightNumber(String flightNumber);
    ApiResponse deleteFlight(String id);
}
