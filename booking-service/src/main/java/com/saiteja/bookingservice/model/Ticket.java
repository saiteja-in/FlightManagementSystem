package com.saiteja.bookingservice.model;

import com.saiteja.bookingservice.model.enums.TicketStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
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
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @NotBlank(message = "PNR cannot be empty")
    @Column(name = "pnr", nullable = false, length = 10)
    private String pnr;

    @NotBlank(message = "Booking ID is required")
    @Column(name = "booking_id", nullable = false, length = 36)
    private String bookingId;

    @NotBlank(message = "Schedule ID is required")
    @Column(name = "schedule_id", nullable = false, length = 36)
    private String scheduleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    @ElementCollection
    @CollectionTable(name = "ticket_passengers", joinColumns = @JoinColumn(name = "ticket_id"))
    @NotEmpty(message = "Passenger list cannot be empty")
    @Builder.Default
    private List<@Valid Passenger> passengers = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @NotNull(message = "Issued time is required")
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
}



