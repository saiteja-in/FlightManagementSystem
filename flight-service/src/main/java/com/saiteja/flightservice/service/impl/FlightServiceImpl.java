package com.saiteja.flightservice.service.impl;

import com.saiteja.flightservice.dto.ApiResponse;
import com.saiteja.flightservice.dto.FlightCreateRequest;
import com.saiteja.flightservice.dto.FlightResponse;
import com.saiteja.flightservice.dto.FlightResponseWrapper;
import com.saiteja.flightservice.exception.DuplicateResourceException;
import com.saiteja.flightservice.model.Flight;
import com.saiteja.flightservice.repository.FlightRepository;
import com.saiteja.flightservice.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;

    @Override
    public ApiResponse createFlight(FlightCreateRequest request) {
        String flightNumber = request.getFlightNumber().trim().toUpperCase();

        if (flightRepository.existsByFlightNumber(flightNumber)) {
            throw new DuplicateResourceException("Flight already exists with number: " + flightNumber);
        }

        Flight flight = Flight.builder()
                .flightNumber(flightNumber)
                .airline(request.getAirline())
                .originAirport(request.getOriginAirport().trim().toUpperCase())
                .destinationAirport(request.getDestinationAirport().trim().toUpperCase())
                .seatCapacity(request.getSeatCapacity())
                .build();

        Flight savedFlight = flightRepository.save(flight);

        return ApiResponse.builder()
                .message("Flight created successfully with flight number: " + flightNumber)
                .status("CREATED")
                .id(savedFlight.getId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlightResponse> getAllFlights() {
        return flightRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FlightResponseWrapper getFlightByFlightNumber(String flightNumber) {
        return flightRepository.findByFlightNumber(flightNumber.trim().toUpperCase())
                .map(flight -> FlightResponseWrapper.builder()
                        .flight(toResponse(flight))
                        .message("Flight retrieved successfully")
                        .status("FOUND")
                        .build())
                .orElse(FlightResponseWrapper.builder()
                        .flight(null)
                        .message("Flight with number " + flightNumber + " does not exist")
                        .status("NOT_FOUND")
                        .build());
    }

    @Override
    public ApiResponse deleteFlight(String id) {
        return flightRepository.findById(id)
                .map(flight -> {
                    flightRepository.delete(flight);
                    return ApiResponse.builder()
                            .message("Flight deleted successfully")
                            .status("DELETED")
                            .build();
                })
                .orElse(ApiResponse.builder()
                        .message("Flight with id " + id + " does not exist")
                        .status("NOT_FOUND")
                        .build());
    }

    private FlightResponse toResponse(Flight flight) {
        return FlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .airline(flight.getAirline())
                .originAirport(flight.getOriginAirport())
                .destinationAirport(flight.getDestinationAirport())
                .seatCapacity(flight.getSeatCapacity())
                .build();
    }
}

