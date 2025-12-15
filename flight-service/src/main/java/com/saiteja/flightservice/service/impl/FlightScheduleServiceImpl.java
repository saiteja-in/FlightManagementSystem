package com.saiteja.flightservice.service.impl;

import com.saiteja.flightservice.dto.ApiResponse;
import com.saiteja.flightservice.dto.FlightScheduleCreateRequest;
import com.saiteja.flightservice.dto.FlightScheduleResponse;
import com.saiteja.flightservice.exception.BadRequestException;
import com.saiteja.flightservice.exception.ResourceNotFoundException;
import com.saiteja.flightservice.model.Flight;
import com.saiteja.flightservice.model.FlightSchedule;
import com.saiteja.flightservice.model.enums.Airport;
import com.saiteja.flightservice.model.enums.FlightStatus;
import com.saiteja.flightservice.repository.FlightRepository;
import com.saiteja.flightservice.repository.FlightScheduleRepository;
import com.saiteja.flightservice.service.FlightScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FlightScheduleServiceImpl implements FlightScheduleService {

    private final FlightScheduleRepository flightScheduleRepository;
    private final FlightRepository flightRepository;

    @Override
    public ApiResponse createSchedule(FlightScheduleCreateRequest request) {
        String flightNumber = request.getFlightNumber().trim().toUpperCase();

        Flight flight = flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Flight not found with number: " + flightNumber
                ));

        FlightSchedule schedule = FlightSchedule.builder()
                .flight(flight)
                .flightDate(request.getFlightDate())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .fare(request.getFare())
                .totalSeats(flight.getSeatCapacity())
                .availableSeats(flight.getSeatCapacity())
                .status(FlightStatus.SCHEDULED)
                .build();

        FlightSchedule savedSchedule = flightScheduleRepository.save(schedule);

        return ApiResponse.builder()
                .message("Flight schedule created successfully for flight: " + flightNumber)
                .status("CREATED")
                .id(savedSchedule.getId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlightScheduleResponse> searchFlights(Airport origin, Airport destination, LocalDate date) {
        List<FlightSchedule> schedules = flightScheduleRepository
                .findByFlightOriginAirportAndFlightDestinationAirportAndFlightDate(
                        origin, destination, date
                );

        // Return empty list instead of throwing exception - more RESTful
        return schedules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FlightScheduleResponse getScheduleById(String id) {
        FlightSchedule schedule = flightScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight schedule not found: " + id));

        return toResponse(schedule);
    }

    @Override
    public void lockSeats(String scheduleId, List<String> seatNumbers) {
        FlightSchedule schedule = flightScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight schedule not found: " + scheduleId));

        int seatsToBook = seatNumbers.size();

        if (schedule.getAvailableSeats() < seatsToBook) {
            throw new BadRequestException("Not enough seats available. Available: " +
                    schedule.getAvailableSeats() + ", Requested: " + seatsToBook);
        }

        // Use Set for O(1) lookup performance
        List<String> bookedSeats = schedule.getBookedSeats();
        Set<String> bookedSeatsSet = new HashSet<>(bookedSeats);
        
        // Check for duplicate seat numbers within request
        Set<String> requestedSeatsSet = new HashSet<>(seatNumbers);
        if (requestedSeatsSet.size() < seatNumbers.size()) {
            throw new BadRequestException("Duplicate seat numbers in the request");
        }
        
        for (String seatNumber : seatNumbers) {
            if (bookedSeatsSet.contains(seatNumber)) {
                throw new BadRequestException("Seat " + seatNumber + " is already booked");
            }
        }

        bookedSeats.addAll(seatNumbers);
        schedule.setAvailableSeats(schedule.getAvailableSeats() - seatsToBook);

        flightScheduleRepository.save(schedule);
    }

    @Override
    public void releaseSeats(String scheduleId, List<String> seatNumbers) {
        FlightSchedule schedule = flightScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight schedule not found: " + scheduleId));

        int seatsToRelease = seatNumbers.size();
        List<String> bookedSeats = schedule.getBookedSeats();

        // bookedSeats is always initialized (@Builder.Default), so null check is unnecessary
        bookedSeats.removeIf(seatNumbers::contains);
        schedule.setAvailableSeats(schedule.getAvailableSeats() + seatsToRelease);

        flightScheduleRepository.save(schedule);
    }

    private FlightScheduleResponse toResponse(FlightSchedule schedule) {
        Flight flight = schedule.getFlight();

        return FlightScheduleResponse.builder()
                .scheduleId(schedule.getId())
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .airline(flight.getAirline().name())
                .originAirport(flight.getOriginAirport())
                .destinationAirport(flight.getDestinationAirport())
                .flightDate(schedule.getFlightDate())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .fare(schedule.getFare())
                .totalSeats(schedule.getTotalSeats())
                .availableSeats(schedule.getAvailableSeats())
                .status(schedule.getStatus().name())
                .build();
    }
}

