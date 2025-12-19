package com.saiteja.bookingservice.service.impl;

import com.saiteja.bookingservice.client.FlightServiceClient;
import com.saiteja.bookingservice.dto.ApiResponse;
import com.saiteja.bookingservice.dto.booking.BookingCreateRequest;
import com.saiteja.bookingservice.dto.booking.BookingResponse;
import com.saiteja.bookingservice.dto.passenger.PassengerResponse;
import com.saiteja.bookingservice.exception.BadRequestException;
import com.saiteja.bookingservice.exception.ResourceNotFoundException;
import com.saiteja.bookingservice.model.Booking;
import com.saiteja.bookingservice.model.Passenger;
import com.saiteja.bookingservice.model.Ticket;
import com.saiteja.bookingservice.model.enums.BookingStatus;
import com.saiteja.bookingservice.model.enums.TicketStatus;
import com.saiteja.bookingservice.repository.BookingRepository;
import com.saiteja.bookingservice.repository.TicketRepository;
import com.saiteja.bookingservice.service.BookingService;
import com.saiteja.bookingservice.service.TicketService;
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
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final FlightServiceClient flightServiceClient;

    @Override
    public String createBooking(BookingCreateRequest request, Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID is required");
        }

        if (request.getScheduleId() == null || request.getScheduleId().trim().isEmpty()) {
            throw new BadRequestException("Schedule ID is required");
        }

        if (request.getPassengers() == null || request.getPassengers().isEmpty()) {
            throw new BadRequestException("At least one passenger is required");
        }

        // Lock seats in flight-service for the schedule
        List<String> seatNumbers = request.getPassengers().stream()
                .map(com.saiteja.bookingservice.dto.passenger.PassengerRequest::getSeatNumber)
                .collect(Collectors.toList());

        try {
            flightServiceClient.lockSeats(request.getScheduleId(), seatNumbers);
        } catch (Exception e) {
            throw new BadRequestException("Failed to lock seats for schedule " + request.getScheduleId() + ": " + e.getMessage());
        }

        // Generate unique PNR
        String pnr = generateUniquePNR();

        // Create booking
        Booking booking = Booking.builder()
                .pnr(pnr)
                .contactEmail(request.getContactEmail().trim().toLowerCase())
                .userId(userId)
                .scheduleId(request.getScheduleId())
                .passengers(mapPassengers(request))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        ticketService.generateTicket(savedBooking.getId());

        return pnr;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByPnr(String pnr, Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID is required");
        }

        Booking booking = bookingRepository.findByPnrAndUserId(pnr, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr + " for the current user"));

        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID is required");
        }

        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse cancelBooking(String pnr, Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID is required");
        }

        Booking booking = bookingRepository.findByPnrAndUserId(pnr, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr + " for the current user"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Release seats for the schedule
        List<String> seatNumbers = booking.getPassengers().stream()
                .map(Passenger::getSeatNumber)
                .collect(Collectors.toList());

        try {
            flightServiceClient.releaseSeats(booking.getScheduleId(), seatNumbers);
        } catch (Exception e) {
            // Log error but don't fail cancellation if seat release fails
            // The booking is already marked as cancelled
        }

        // Cancel all tickets for this booking - use batch save for better performance
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        if (!tickets.isEmpty()) {
            tickets.forEach(ticket -> ticket.setStatus(TicketStatus.CANCELLED));
            ticketRepository.saveAll(tickets);
        }

        bookingRepository.save(booking);

        return ApiResponse.builder()
                .message("Booking and tickets cancelled successfully")
                .status("CANCELLED")
                .build();
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

        // Get ticketId for this booking (since one booking = one scheduleId = one ticket)
        String ticketId = null;
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        if (!tickets.isEmpty()) {
            // Get the first ticket (should be only one)
            ticketId = tickets.get(0).getId();
        }

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .pnr(booking.getPnr())
                .contactEmail(booking.getContactEmail())
                .userId(booking.getUserId())
                .ticketId(ticketId)
                .scheduleId(booking.getScheduleId())
                .passengers(passengers)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    // Generate unique PNR number
    private String generateUniquePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int maxAttempts = 10;
        int attempts = 0;
        String pnr;

        do {
            StringBuilder pnrBuilder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                int index = (int) (Math.random() * chars.length());
                pnrBuilder.append(chars.charAt(index));
            }
            pnr = pnrBuilder.toString();
            attempts++;
            
            // Check if PNR exists only once per attempt
            if (!bookingRepository.existsByPnr(pnr)) {
                return pnr;
            }
        } while (attempts < maxAttempts);

        throw new BadRequestException("Failed to generate unique PNR. Please try again.");
    }
}
