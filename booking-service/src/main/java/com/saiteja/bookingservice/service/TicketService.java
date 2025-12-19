package com.saiteja.bookingservice.service;

import com.saiteja.bookingservice.dto.ticket.TicketResponse;

public interface TicketService {
    TicketResponse generateTicket(String bookingId);
    TicketResponse getTicketByPnr(String pnr);
    TicketResponse getTicketById(String ticketId, Long userId);
}


