package com.saiteja.bookingservice.dto.ticket;

import com.saiteja.bookingservice.dto.passenger.PassengerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private String ticketId;
    private String pnr;
//    private String bookingId;
    private String scheduleId;
//    private String status;
    private List<PassengerResponse> passengers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime issuedAt;
}

