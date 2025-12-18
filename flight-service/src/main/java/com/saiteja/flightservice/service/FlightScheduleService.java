package com.saiteja.flightservice.service;

import com.saiteja.flightservice.dto.ApiResponse;
import com.saiteja.flightservice.dto.FlightScheduleCreateRequest;
import com.saiteja.flightservice.dto.FlightScheduleResponse;
import com.saiteja.flightservice.model.enums.Airport;

import java.util.List;

public interface FlightScheduleService {
    ApiResponse createSchedule(FlightScheduleCreateRequest request, Long createdByUserId);
    List<FlightScheduleResponse> searchFlights(Airport origin, Airport destination, java.time.LocalDate date);
    FlightScheduleResponse getScheduleById(String id);
    void lockSeats(String scheduleId, List<String> seatNumbers);
    void releaseSeats(String scheduleId, List<String> seatNumbers);
}
