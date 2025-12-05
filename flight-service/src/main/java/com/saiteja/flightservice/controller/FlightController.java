package com.saiteja.flightservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1.0/flight/admin/flights")
public class FlightController {

    @GetMapping("/health")
    public String healthCheck(){
        return "healthy";
    }

}
