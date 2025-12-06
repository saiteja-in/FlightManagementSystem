package com.saiteja.bookingservice.dto.booking;

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
public class BookingResponse {
    private String bookingId;
    private String pnr;
    private String contactEmail;
    private List<String> scheduleIds;
    private List<PassengerResponse> passengers;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
