package com.saiteja.bookingservice.controller;

import com.saiteja.bookingservice.dto.ticket.TicketResponse;
import com.saiteja.bookingservice.exception.BadRequestException;
import com.saiteja.bookingservice.security.JwtUtils;
import com.saiteja.bookingservice.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1.0/flight")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final JwtUtils jwtUtils;

    @GetMapping("/ticket/{pnr}")
    public ResponseEntity<TicketResponse> getTicketByPnr(@PathVariable String pnr) {
        TicketResponse response = ticketService.getTicketByPnr(pnr);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/ticket/id/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable String ticketId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromRequest(httpRequest);
        TicketResponse response = ticketService.getTicketById(ticketId, userId);
        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
            throw new BadRequestException("Invalid or missing authentication token");
        }
        Long userId = jwtUtils.getUserIdFromJwtToken(jwt);
        if (userId == null) {
            throw new BadRequestException("User ID not found in token");
        }
        return userId;
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}

