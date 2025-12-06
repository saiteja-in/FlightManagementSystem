package com.saiteja.bookingservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saiteja.bookingservice.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlightServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String FLIGHT_SERVICE_URL = "http://flight-service";

    public void lockSeats(String scheduleId, List<String> seatNumbers) {
        try {
            String url = FLIGHT_SERVICE_URL + "/api/v1.0/flight/admin/internal/schedules/{id}/lock-seats";
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(seatNumbers),
                    Void.class,
                    scheduleId
            );
            log.info("Successfully locked seats for schedule: {}", scheduleId);
        } catch (HttpClientErrorException e) {
            log.error("Client error while locking seats for schedule {}: {}", scheduleId, e.getMessage());
            String errorMessage = extractErrorMessage(e.getResponseBodyAsString());
            throw new BadRequestException(errorMessage);
        } catch (HttpServerErrorException e) {
            log.error("Server error while locking seats for schedule {}: {}", scheduleId, e.getMessage());
            String errorMessage = extractErrorMessage(e.getResponseBodyAsString());
            throw new BadRequestException(errorMessage);
        } catch (RestClientException e) {
            log.error("Failed to connect to flight-service for schedule {}: {}", scheduleId, e.getMessage());
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("No instances available")) {
                throw new BadRequestException("Flight service is not available. Please ensure flight-service is running and registered with Eureka.");
            }
            throw new BadRequestException("Failed to connect to flight service: " + errorMsg + ". Please ensure flight-service is running and registered with Eureka.");
        }
    }

    public void releaseSeats(String scheduleId, List<String> seatNumbers) {
        try {
            String url = FLIGHT_SERVICE_URL + "/api/v1.0/flight/admin/internal/schedules/{id}/release-seats";
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(seatNumbers),
                    Void.class,
                    scheduleId
            );
            log.info("Successfully released seats for schedule: {}", scheduleId);
        } catch (HttpClientErrorException e) {
            log.error("Client error while releasing seats for schedule {}: {}", scheduleId, e.getMessage());
            String errorMessage = extractErrorMessage(e.getResponseBodyAsString());
            throw new BadRequestException(errorMessage);
        } catch (HttpServerErrorException e) {
            log.error("Server error while releasing seats for schedule {}: {}", scheduleId, e.getMessage());
            String errorMessage = extractErrorMessage(e.getResponseBodyAsString());
            throw new BadRequestException(errorMessage);
        } catch (RestClientException e) {
            log.error("Failed to connect to flight-service for schedule {}: {}", scheduleId, e.getMessage());
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("No instances available")) {
                throw new BadRequestException("Flight service is not available. Please ensure flight-service is running and registered with Eureka.");
            }
            throw new BadRequestException("Failed to connect to flight service: " + errorMsg + ". Please ensure flight-service is running and registered with Eureka.");
        }
    }

    private String extractErrorMessage(String errorResponseBody) {
        if (errorResponseBody == null || errorResponseBody.trim().isEmpty()) {
            return "Unable to lock seats. Please try again later.";
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> errorMap = objectMapper.readValue(errorResponseBody, HashMap.class);
            String message = (String) errorMap.get("message");
            
            if (message != null && !message.trim().isEmpty()) {
                if (message.contains("already booked")) {
                    return "One or more selected seats are already booked. Please choose different seats.";
                } else if (message.contains("Not enough seats available")) {
                    return message;
                } else if (message.contains("Flight schedule not found")) {
                    return "The flight schedule is not available. Please check the schedule ID.";
                } else {
                    return message;
                }
            }
            
            String error = (String) errorMap.get("error");
            if (error != null && !error.trim().isEmpty()) {
                return "Unable to process booking: " + error;
            }
            
            return "Unable to lock seats. Please try again later.";
        } catch (Exception e) {
            if (errorResponseBody.contains("already booked")) {
                return "One or more selected seats are already booked. Please choose different seats.";
            } else if (errorResponseBody.contains("Not enough seats available")) {
                return errorResponseBody;
            }
            log.warn("Could not parse error response: {}", errorResponseBody);
            return "Unable to lock seats. Please try again later.";
        }
    }
}

