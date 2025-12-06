package com.saiteja.bookingservice.dto.passenger;

import com.saiteja.bookingservice.model.enums.Gender;
import com.saiteja.bookingservice.model.enums.MealOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerResponse {
    private String fullName;
    private Gender gender;
    private Integer age;
    private String seatNumber;
    private MealOption mealOption;
}
