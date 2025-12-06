package com.saiteja.bookingservice.service.impl;

import com.saiteja.bookingservice.dto.ApiResponse;
import com.saiteja.bookingservice.dto.booking.BookingCreateRequest;
import com.saiteja.bookingservice.dto.booking.BookingResponse;
import com.saiteja.bookingservice.dto.passenger.PassengerResponse;
import com.saiteja.bookingservice.dto.ticket.TicketResponse;
import com.saiteja.bookingservice.exception.BadRequestException;
import com.saiteja.bookingservice.exception.ResourceNotFoundException;
import com.saiteja.bookingservice.model.Booking;
import com.saiteja.bookingservice.model.Passenger;
import com.saiteja.bookingservice.repository.BookingRepository;
import com.saiteja.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    @Override
    public TicketResponse createBooking(BookingCreateRequest request) {
      return null;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));

        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByEmail(String email) {
        List<Booking> bookings = bookingRepository.findByContactEmail(email.trim().toLowerCase());
        return bookings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse cancelBooking(String pnr) {
       return null;
    }

    private List<Passenger> mapPassengers(BookingCreateRequest request) {
        return request.getPassengers().stream()
                .map(p -> Passenger.builder()
                        .fullName(p.getFullName())
                        .gender(p.getGender())
                        .age(p.getAge())
                        .mealOption(p.getMealOption())
                        .seatNumber(p.getSeatNumber())
                        .build())
                .collect(Collectors.toList());
    }

    private BookingResponse toResponse(Booking booking) {
        List<PassengerResponse> passengers = booking.getPassengers().stream()
                .map(p -> PassengerResponse.builder()
                        .fullName(p.getFullName())
                        .gender(p.getGender())
                        .age(p.getAge())
                        .seatNumber(p.getSeatNumber())
                        .mealOption(p.getMealOption())
                        .build())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .pnr(booking.getPnr())
                .contactEmail(booking.getContactEmail())
                .scheduleIds(booking.getScheduleIds())
                .passengers(passengers)
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    //genereate unique pnr number
    private String generateUniquePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String pnr;
        int maxAttempts = 10;
        int attempts = 0;

        do {
            StringBuilder pnrBuilder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                int index = (int) (Math.random() * chars.length());
                pnrBuilder.append(chars.charAt(index));
            }
            pnr = pnrBuilder.toString();
            attempts++;
        } while (bookingRepository.existsByPnr(pnr) && attempts < maxAttempts);

        if (bookingRepository.existsByPnr(pnr)) {
            throw new BadRequestException("Failed to generate unique PNR. Please try again.");
        }

        return pnr;
    }
}
