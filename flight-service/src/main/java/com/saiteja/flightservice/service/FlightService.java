package com.saiteja.flightservice.service;

import com.saiteja.flightservice.dto.ApiResponse;
import com.saiteja.flightservice.dto.FlightCreateRequest;
import com.saiteja.flightservice.dto.FlightResponse;
import com.saiteja.flightservice.dto.FlightResponseWrapper;

import java.util.List;

public interface FlightService {
    ApiResponse createFlight(FlightCreateRequest request);
    List<FlightResponse> getAllFlights();
    FlightResponseWrapper getFlightByFlightNumber(String flightNumber);
    ApiResponse deleteFlight(String id);
}
