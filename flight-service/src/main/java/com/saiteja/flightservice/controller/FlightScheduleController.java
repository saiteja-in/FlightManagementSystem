package com.saiteja.flightservice.controller;

import com.saiteja.flightservice.dto.ApiResponse;
import com.saiteja.flightservice.dto.FlightScheduleCreateRequest;
import com.saiteja.flightservice.dto.FlightScheduleResponse;
import com.saiteja.flightservice.dto.FlightSearchRequest;
import com.saiteja.flightservice.exception.BadRequestException;
import com.saiteja.flightservice.security.JwtUtils;
import com.saiteja.flightservice.service.FlightScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0/flight/admin")
@RequiredArgsConstructor
@Validated
public class FlightScheduleController {

    private final FlightScheduleService flightScheduleService;
    private final JwtUtils jwtUtils;

    @PostMapping("/inventory")
    public ResponseEntity<ApiResponse> addInventory(
            @Valid @RequestBody FlightScheduleCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromRequest(httpRequest);
        List<String> roles = getRolesFromRequest(httpRequest);
        
        // Verify user has ADMIN role
        if (!roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Only administrators can create flight schedules");
        }
        
        ApiResponse response = flightScheduleService.createSchedule(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/search")
    public ResponseEntity<List<FlightScheduleResponse>> searchFlights(@Valid @RequestBody FlightSearchRequest request) {
        List<FlightScheduleResponse> schedules = flightScheduleService.searchFlights(
                request.getOriginAirport(),
                request.getDestinationAirport(),
                request.getFlightDate()
        );
        return ResponseEntity.ok(schedules);
    }

    //internal endpoint for booking-service to fetch schedule by id
    @GetMapping("/internal/schedules/{id}")
    public ResponseEntity<FlightScheduleResponse> getScheduleById(@PathVariable String id) {
        FlightScheduleResponse schedule = flightScheduleService.getScheduleById(id);
        return ResponseEntity.ok(schedule);
    }

    //internal endpoint for booking-service to lock seats
    @PostMapping("/internal/schedules/{id}/lock-seats")
    public ResponseEntity<ApiResponse> lockSeats(
            @PathVariable String id,
            @RequestBody List<String> seatNumbers
    ) {
        flightScheduleService.lockSeats(id, seatNumbers);
        ApiResponse response = ApiResponse.builder()
                .message("Seats locked successfully")
                .status("SUCCESS")
                .build();
        return ResponseEntity.ok(response);
    }

    //internal endpoint for booking-service to release seats
    @PostMapping("/internal/schedules/{id}/release-seats")
    public ResponseEntity<ApiResponse> releaseSeats(
            @PathVariable String id,
            @RequestBody List<String> seatNumbers
    ) {
        flightScheduleService.releaseSeats(id, seatNumbers);
        ApiResponse response = ApiResponse.builder()
                .message("Seats released successfully")
                .status("SUCCESS")
                .build();
        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
            throw new BadRequestException("Invalid or missing authentication token");
        }
        Long userId = jwtUtils.getUserIdFromJwtToken(jwt);
        if (userId == null) {
            throw new BadRequestException("User ID not found in token");
        }
        return userId;
    }

    private List<String> getRolesFromRequest(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
            throw new BadRequestException("Invalid or missing authentication token");
        }
        return jwtUtils.getRolesFromJwtToken(jwt);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}

