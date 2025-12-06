package com.saiteja.bookingservice.repository;

import com.saiteja.bookingservice.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {

    Optional<Ticket> findByPnr(String pnr);

    List<Ticket> findByBookingId(String bookingId);
}


