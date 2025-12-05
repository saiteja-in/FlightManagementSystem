package com.saiteja.flightservice.controller;

import com.saiteja.flightservice.dto.ApiResponse;
import com.saiteja.flightservice.dto.FlightCreateRequest;
import com.saiteja.flightservice.dto.FlightResponse;
import com.saiteja.flightservice.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0/flight/admin/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/health")
    public String healthCheck(){
        return "healthy";
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createFlight(@Valid @RequestBody FlightCreateRequest request) {
        ApiResponse response = flightService.createFlight(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FlightResponse>> getAllFlights() {
        List<FlightResponse> flights = flightService.getAllFlights();
        return ResponseEntity.ok(flights);
    }

    @GetMapping("/{flightNumber}")
    public ResponseEntity<FlightResponse> getFlight(@PathVariable String flightNumber) {
        FlightResponse flight = flightService.getFlightByFlightNumber(flightNumber);
        return ResponseEntity.ok(flight);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteFlight(@PathVariable String id) {
        ApiResponse response = flightService.deleteFlight(id);
        return ResponseEntity.ok(response);
    }

}
