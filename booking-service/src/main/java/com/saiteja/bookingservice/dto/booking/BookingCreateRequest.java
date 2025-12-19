package com.saiteja.bookingservice.dto.booking;

import com.saiteja.bookingservice.dto.passenger.PassengerRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BookingCreateRequest {
    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;
    // scheduleId is provided via path variable, not request body, so no validation annotation here
    private String scheduleId;
    @NotEmpty(message = "At least one passenger is required")
    private List<@Valid PassengerRequest> passengers;
}
