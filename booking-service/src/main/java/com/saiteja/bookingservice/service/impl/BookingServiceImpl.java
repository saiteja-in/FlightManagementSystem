package com.saiteja.bookingservice.service.impl;

import com.saiteja.bookingservice.client.FlightServiceClient;
import com.saiteja.bookingservice.dto.ApiResponse;
import com.saiteja.bookingservice.dto.booking.BookingCreateRequest;
import com.saiteja.bookingservice.dto.booking.BookingResponse;
import com.saiteja.bookingservice.dto.passenger.PassengerResponse;
import com.saiteja.bookingservice.dto.ticket.TicketResponse;
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
    public TicketResponse createBooking(BookingCreateRequest request) {
        if (request.getScheduleIds() == null || request.getScheduleIds().isEmpty()) {
            throw new BadRequestException("At least one schedule id is required");
        }

        if (request.getPassengers() == null || request.getPassengers().isEmpty()) {
            throw new BadRequestException("At least one passenger is required");
        }

        // Lock seats in flight-service for ALL scheduleIds
        List<String> seatNumbers = request.getPassengers().stream()
                .map(p -> p.getSeatNumber())
                .collect(Collectors.toList());

        for (String scheduleId : request.getScheduleIds()) {
            try {
                flightServiceClient.lockSeats(scheduleId, seatNumbers);
            } catch (Exception e) {
                // If locking fails for any schedule, release already locked seats
                for (String lockedScheduleId : request.getScheduleIds()) {
                    if (!lockedScheduleId.equals(scheduleId)) {
                        try {
                            flightServiceClient.releaseSeats(lockedScheduleId, seatNumbers);
                        } catch (Exception releaseEx) {
                        }
                    }
                }
                throw new BadRequestException("Failed to lock seats for schedule " + scheduleId + ": " + e.getMessage());
            }
        }

        // Generate unique PNR
        String pnr = generateUniquePNR();

        // Create booking
        Booking booking = Booking.builder()
                .pnr(pnr)
                .contactEmail(request.getContactEmail().trim().toLowerCase())
                .scheduleIds(request.getScheduleIds())
                .passengers(mapPassengers(request))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Generate ticket (returns first ticket, but generates tickets for all schedules)
        return ticketService.generateTicket(savedBooking.getId());
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
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Release seats for ALL scheduleIds
        List<String> seatNumbers = booking.getPassengers().stream()
                .map(Passenger::getSeatNumber)
                .collect(Collectors.toList());

        for (String scheduleId : booking.getScheduleIds()) {
            try {
                flightServiceClient.releaseSeats(scheduleId, seatNumbers);
            } catch (Exception e) {
                // Log error but continue releasing seats for other schedules
                // We don't want to fail the cancellation if one schedule fails
            }
        }

        // Cancel all tickets for this booking
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        tickets.forEach(ticket -> {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        });

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
