package com.airportrouteplanner.flightservice.schedule;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flight-legs")
public class FlightScheduleController {

    private final FlightScheduleService flightScheduleService;

    public FlightScheduleController(FlightScheduleService flightScheduleService) {
        this.flightScheduleService = flightScheduleService;
    }

    @GetMapping
    public List<AdjustedFlightLegResponse> getAdjustedDailySchedule() {
        return flightScheduleService.getAdjustedDailySchedule();
    }
}
