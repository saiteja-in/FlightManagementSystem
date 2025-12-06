package com.saiteja.bookingservice.controller;

import com.saiteja.bookingservice.dto.ticket.TicketResponse;
import com.saiteja.bookingservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1.0/flight")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/ticket/{pnr}")
    public ResponseEntity<TicketResponse> getTicketByPnr(@PathVariable String pnr) {
        TicketResponse response = ticketService.getTicketByPnr(pnr);
        return ResponseEntity.ok(response);
    }
}

