package com.saiteja.bookingservice.client;

import com.saiteja.bookingservice.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlightServiceClient {

    private final RestTemplate restTemplate;
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
            throw new BadRequestException("Failed to lock seats: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Server error while locking seats for schedule {}: {}", scheduleId, e.getMessage());
            throw new BadRequestException("Flight service error: " + e.getResponseBodyAsString());
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
            throw new BadRequestException("Failed to release seats: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Server error while releasing seats for schedule {}: {}", scheduleId, e.getMessage());
            throw new BadRequestException("Flight service error: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Failed to connect to flight-service for schedule {}: {}", scheduleId, e.getMessage());
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("No instances available")) {
                throw new BadRequestException("Flight service is not available. Please ensure flight-service is running and registered with Eureka.");
            }
            throw new BadRequestException("Failed to connect to flight service: " + errorMsg + ". Please ensure flight-service is running and registered with Eureka.");
        }
    }
}

