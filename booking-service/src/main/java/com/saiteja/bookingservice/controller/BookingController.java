package com.saiteja.bookingservice.controller;

import com.saiteja.bookingservice.dto.ApiResponse;
import com.saiteja.bookingservice.dto.booking.BookingCreateRequest;
import com.saiteja.bookingservice.dto.booking.BookingResponse;
import com.saiteja.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0/flight")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/health")
    public String healthCheck(){
        return "healthy";
    }

    @PostMapping("/booking/{scheduleId}")
    public ResponseEntity<String> bookFlight(
            @PathVariable String scheduleId,
            @Valid @RequestBody BookingCreateRequest request
    ) {
        request.setScheduleIds(List.of(scheduleId));
        String pnr = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pnr);
    }

    @GetMapping("/booking/{pnr}")
    public ResponseEntity<BookingResponse> getBookingByPnr(@PathVariable String pnr) {
        BookingResponse response = bookingService.getBookingByPnr(pnr);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/email/{email}")
    public ResponseEntity<List<BookingResponse>> getBookingsByEmail(@PathVariable String email) {
        List<BookingResponse> bookings = bookingService.getBookingsByEmail(email);
        return ResponseEntity.ok(bookings);
    }

    @DeleteMapping("/booking/cancel/{pnr}")
    public ResponseEntity<ApiResponse> cancelBooking(@PathVariable String pnr) {
        ApiResponse response = bookingService.cancelBooking(pnr);
        return ResponseEntity.ok(response);
    }
}