package com.saiteja.flightservice.model;

import com.saiteja.flightservice.model.enums.Airline;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flights")
@EntityListeners(AuditingEntityListener.class)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Flight {

    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    @Column(name="id",updatable=false,nullable=false)
    private String id;

    @NotBlank(message="flight number cannot be empty")
    @Column(name="flight_number",unique=true,nullable=false,length=10)
    private String flightNumber;

    @NotNull(message = "airline can't be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "airline", nullable = false, length = 20)
    private Airline airline;

    @NotBlank(message = "origin airport is required")
    @Column(name = "origin_airport", nullable = false, length = 20)
    private String originAirport;

    @NotBlank(message = "destination airport is required")
    @Column(name = "destination_airport", nullable = false, length = 20)
    private String destinationAirport;

    @NotNull(message = "seat capacity is required")
    @Min(value = 1, message = "Seat capacity must be at least 1")
    @Max(value = 1000, message = "Seat capacity cannot exceed 1000")
    @Column(name = "seat_capacity", nullable = false)
    private Integer seatCapacity;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<FlightSchedule> schedules = new ArrayList<>();
}
