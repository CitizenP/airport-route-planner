package com.airportrouteplanner.webapp.airport;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AirportUiController.class)
public class AirportUiExceptionHandler {

    @ExceptionHandler(AirportServiceUnavailableException.class)
    public ProblemDetail handleUnavailableAirportService(AirportServiceUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, exception.getMessage());
        problem.setTitle("Airport data unavailable");
        return problem;
    }
}
