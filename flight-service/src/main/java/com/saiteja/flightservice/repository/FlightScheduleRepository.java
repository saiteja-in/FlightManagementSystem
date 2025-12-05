package com.saiteja.flightservice.repository;

import com.saiteja.flightservice.model.Flight;
import com.saiteja.flightservice.model.FlightSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, String> {

    List<FlightSchedule> findByFlightAndFlightDate(Flight flight, LocalDate date);

    Optional<FlightSchedule> findById(String id);

    List<FlightSchedule> findByFlightOriginAirportAndFlightDestinationAirportAndFlightDate(
            String originAirport,
            String destinationAirport,
            LocalDate flightDate
    );
}

