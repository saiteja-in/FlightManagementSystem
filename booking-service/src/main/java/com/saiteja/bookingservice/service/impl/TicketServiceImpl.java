package com.saiteja.bookingservice.service.impl;

import com.saiteja.bookingservice.dto.passenger.PassengerResponse;
import com.saiteja.bookingservice.dto.ticket.TicketResponse;
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

        if (booking.getScheduleIds().isEmpty()) {
            throw new ResourceNotFoundException("No schedule found for booking");
        }

        // Generate tickets for ALL scheduleIds in the booking
        Ticket firstTicket = null;
        for (String scheduleId : booking.getScheduleIds()) {
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
                    .scheduleId(scheduleId)
                    .passengers(new java.util.ArrayList<>(ticketPassengers))
                    .status(TicketStatus.ACTIVE)
                    .issuedAt(LocalDateTime.now())
                    .build();

            Ticket saved = ticketRepository.save(ticket);
            if (firstTicket == null) {
                firstTicket = saved;
            }
        }

        // Return the first ticket (for backward compatibility)
        return toResponse(firstTicket);
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
