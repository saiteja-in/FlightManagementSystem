package com.saiteja.bookingservice.model;


import com.saiteja.bookingservice.model.enums.BookingStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(columnNames = "pnr"))
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @NotBlank(message = "PNR cannot be empty")
    @Column(name = "pnr", unique = true, nullable = false, length = 10)
    private String pnr;

    @ElementCollection
    @CollectionTable(name = "booking_schedule_ids", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "schedule_id", length = 36)
    @NotEmpty(message = "At least one schedule must be selected")
    @Builder.Default
    private List<String> scheduleIds = new ArrayList<>();

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "booking_passengers", joinColumns = @JoinColumn(name = "booking_id"))
    @NotEmpty(message = "At least one passenger is required")
    @Builder.Default
    private List<@Valid Passenger> passengers = new ArrayList<>();

    @NotNull(message = "Booking status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

