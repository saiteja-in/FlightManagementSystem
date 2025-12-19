package com.saiteja.bookingservice.controller;

import com.saiteja.bookingservice.dto.ApiResponse;
import com.saiteja.bookingservice.dto.booking.BookingCreateRequest;
import com.saiteja.bookingservice.dto.booking.BookingResponse;
import com.saiteja.bookingservice.exception.BadRequestException;
import com.saiteja.bookingservice.security.JwtUtils;
import com.saiteja.bookingservice.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0/flight")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final JwtUtils jwtUtils;

    @GetMapping("/health")
    public String healthCheck(){
        return "healthy";
    }

    @PostMapping("/booking/{scheduleId}")
    public ResponseEntity<String> bookFlight(
            @PathVariable String scheduleId,
            @Valid @RequestBody BookingCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromRequest(httpRequest);
        request.setScheduleId(scheduleId);
        String pnr = bookingService.createBooking(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(pnr);
    }

    @GetMapping("/booking/{pnr}")
    public ResponseEntity<BookingResponse> getBookingByPnr(
            @PathVariable String pnr,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromRequest(httpRequest);
        BookingResponse response = bookingService.getBookingByPnr(pnr, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        List<BookingResponse> bookings = bookingService.getBookingsByUserId(userId);
        return ResponseEntity.ok(bookings);
    }

    @DeleteMapping("/booking/cancel/{pnr}")
    public ResponseEntity<ApiResponse> cancelBooking(
            @PathVariable String pnr,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromRequest(httpRequest);
        ApiResponse response = bookingService.cancelBooking(pnr, userId);
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

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}