package com.saiteja.flightservice.controller;

import com.saiteja.flightservice.dto.ApiResponse;
import com.saiteja.flightservice.dto.FlightCreateRequest;
import com.saiteja.flightservice.dto.FlightResponse;
import com.saiteja.flightservice.dto.FlightResponseWrapper;
import com.saiteja.flightservice.exception.BadRequestException;
import com.saiteja.flightservice.security.JwtUtils;
import com.saiteja.flightservice.service.FlightService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0/flight/admin/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;
    private final JwtUtils jwtUtils;

    @GetMapping("/health")
    public String healthCheck(){
        return "healthy";
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createFlight(
            @Valid @RequestBody FlightCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        List<String> roles = getRolesFromRequest(httpRequest);
        
        // Verify user has ADMIN role
        if (!roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Only administrators can create flights");
        }
        
        ApiResponse response = flightService.createFlight(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FlightResponse>> getAllFlights() {
        List<FlightResponse> flights = flightService.getAllFlights();
        return ResponseEntity.ok(flights);
    }

    @GetMapping("/{flightNumber}")
    public ResponseEntity<FlightResponseWrapper> getFlight(@PathVariable String flightNumber) {
        FlightResponseWrapper response = flightService.getFlightByFlightNumber(flightNumber);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteFlight(@PathVariable String id) {
        ApiResponse response = flightService.deleteFlight(id);
        return ResponseEntity.ok(response);
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
