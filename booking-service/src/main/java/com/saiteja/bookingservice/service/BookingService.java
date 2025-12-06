package com.saiteja.bookingservice.service;

import com.saiteja.bookingservice.dto.ApiResponse;
import com.saiteja.bookingservice.dto.booking.BookingCreateRequest;
import com.saiteja.bookingservice.dto.booking.BookingResponse;
import com.saiteja.bookingservice.dto.ticket.TicketResponse;

import java.util.List;

public interface BookingService {
    TicketResponse createBooking(BookingCreateRequest request);
    BookingResponse getBookingByPnr(String pnr);
    List<BookingResponse> getBookingsByEmail(String email);
    ApiResponse cancelBooking(String pnr);
}

