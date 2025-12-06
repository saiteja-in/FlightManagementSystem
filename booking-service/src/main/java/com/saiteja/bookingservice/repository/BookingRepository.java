package com.saiteja.bookingservice.repository;

import com.saiteja.bookingservice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    Optional<Booking> findByPnr(String pnr);
    boolean existsByPnr(String pnr);
    List<Booking> findByContactEmail(String email);
}

