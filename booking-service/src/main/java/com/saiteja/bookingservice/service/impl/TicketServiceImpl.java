package com.saiteja.bookingservice.service.impl;

import com.saiteja.bookingservice.dto.passenger.PassengerResponse;
import com.saiteja.bookingservice.dto.ticket.TicketResponse;
import com.saiteja.bookingservice.exception.BadRequestException;
import com.saiteja.bookingservice.exception.ResourceNotFoundException;
import com.saiteja.bookingservice.model.Booking;
import com.saiteja.bookingservice.model.Ticket;
import com.saiteja.bookingservice.model.enums.TicketStatus;
import com.saiteja.bookingservice.repository.BookingRepository;
import com.saiteja.bookingservice.repository.TicketRepository;
import com.saiteja.bookingservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;

    @Override
    public TicketResponse generateTicket(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getScheduleId() == null || booking.getScheduleId().trim().isEmpty()) {
            throw new ResourceNotFoundException("No schedule found for booking");
        }

        // Generate ticket for the booking (one booking = one scheduleId = one ticket)
        LocalDateTime issuedAt = LocalDateTime.now();
        
        // Create a new list with copies of passengers to avoid shared collection reference
        List<com.saiteja.bookingservice.model.Passenger> ticketPassengers = booking.getPassengers().stream()
                .map(p -> com.saiteja.bookingservice.model.Passenger.builder()
                        .fullName(p.getFullName())
                        .gender(p.getGender())
                        .age(p.getAge())
                        .seatNumber(p.getSeatNumber())
                        .mealOption(p.getMealOption())
                        .build())
                .collect(Collectors.toList());
        
        Ticket ticket = Ticket.builder()
                .pnr(booking.getPnr())
                .bookingId(booking.getId())
                .scheduleId(booking.getScheduleId())
                .passengers(new java.util.ArrayList<>(ticketPassengers))
                .status(TicketStatus.ACTIVE)
                .issuedAt(issuedAt)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        
        return toResponse(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketByPnr(String pnr) {
        Ticket ticket = ticketRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ResourceNotFoundException("Ticket has been cancelled");
        }

        return toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(String ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BadRequestException("Ticket has been cancelled");
        }

        // Validate user owns the booking
        validateUserOwnsTicket(ticket, userId);

        return toResponse(ticket);
    }

    private void validateUserOwnsTicket(Ticket ticket, Long userId) {
        Booking booking = bookingRepository.findById(ticket.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for ticket"));

        if (!booking.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Ticket not found");
        }
    }

    private TicketResponse toResponse(Ticket ticket) {
        List<PassengerResponse> passengers = ticket.getPassengers().stream()
                .map(p -> PassengerResponse.builder()
                        .fullName(p.getFullName())
                        .gender(p.getGender())
                        .age(p.getAge())
                        .seatNumber(p.getSeatNumber())
                        .mealOption(p.getMealOption())
                        .build())
                .collect(Collectors.toList());

        return TicketResponse.builder()
                .ticketId(ticket.getId())
                .pnr(ticket.getPnr())
                .scheduleId(ticket.getScheduleId())
                .passengers(passengers)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .issuedAt(ticket.getIssuedAt())
                .build();
    }
}
