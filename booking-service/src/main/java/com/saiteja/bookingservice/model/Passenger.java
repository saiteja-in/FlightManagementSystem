package com.saiteja.bookingservice.model;

import com.saiteja.bookingservice.model.enums.Gender;
import com.saiteja.bookingservice.model.enums.MealOption;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @NotBlank(message = "passenger name is required")
    private String fullName;

    @NotNull(message = "gender is required")
    private Gender gender;

    @NotNull(message = "age is required")
    @Min(value = 1, message = "passenger age must be at least 1")
    @Max(value = 120, message = "passenger age cannot exceed 120")
    private Integer age;

    @NotBlank(message = "seat number is required")
    private String seatNumber;

    @NotNull(message = "meal option is required")
    private MealOption mealOption;
}


