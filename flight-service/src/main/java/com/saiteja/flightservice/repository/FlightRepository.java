package com.saiteja.flightservice.repository;

import com.saiteja.flightservice.model.Flight;
import com.saiteja.flightservice.model.enums.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight,String> {
    Optional<Flight> findByFlightNumber(String flightNumber);
    boolean existsByFlightNumber(String flightNumber);
    List<Flight> findByOriginAirportAndDestinationAirport(Airport origin, Airport destination);
}
