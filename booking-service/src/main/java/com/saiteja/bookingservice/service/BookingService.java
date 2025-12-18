package com.saiteja.bookingservice.service;

import com.saiteja.bookingservice.dto.ApiResponse;
import com.saiteja.bookingservice.dto.booking.BookingCreateRequest;
import com.saiteja.bookingservice.dto.booking.BookingResponse;

import java.util.List;

public interface BookingService {
    String createBooking(BookingCreateRequest request, Long userId);
    BookingResponse getBookingByPnr(String pnr, Long userId);
    List<BookingResponse> getBookingsByUserId(Long userId);
    ApiResponse cancelBooking(String pnr, Long userId);
}

