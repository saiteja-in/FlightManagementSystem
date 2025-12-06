package com.saiteja.bookingservice.dto.passenger;

import com.saiteja.bookingservice.model.enums.Gender;
import com.saiteja.bookingservice.model.enums.MealOption;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PassengerRequest {
    @NotBlank(message = "Passenger name is required")
    private String fullName;
    @NotNull(message = "Gender is required")
    private Gender gender;
    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Passenger age must be at least 1")
    @Max(value = 120, message = "Passenger age cannot exceed 120")
    private Integer age;
    @NotBlank(message = "Seat number is required")
    private String seatNumber;
    @NotNull(message = "Meal option is required")
    private MealOption mealOption;
}

