package com.airportrouteplanner.flightservice.schedule;

import com.airportrouteplanner.flightservice.airport.AirportRunwayCapacityException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = FlightScheduleController.class)
public class FlightScheduleExceptionHandler {

    @ExceptionHandler(AirportRunwayCapacityException.class)
    public ProblemDetail handleAirportRunwayCapacityException(AirportRunwayCapacityException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
        problem.setTitle("Airport runway capacity unavailable");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
