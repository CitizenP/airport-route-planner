package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.integration.UpstreamServiceException;
import com.airportrouteplanner.routeservice.summary.RouteGraphUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RouteCalculationController.class)
public class RouteCalculationExceptionHandler {

    @ExceptionHandler(InvalidRouteRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRouteRequestException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid route request", exception);
    }

    @ExceptionHandler(AirportNotFoundException.class)
    public ProblemDetail handleUnknownAirport(AirportNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Airport not found", exception);
    }

    @ExceptionHandler(RouteNotFoundException.class)
    public ProblemDetail handleRouteNotFound(RouteNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Route not found", exception);
    }

    @ExceptionHandler({UpstreamServiceException.class, RouteGraphUnavailableException.class})
    public ProblemDetail handleUnavailableGraph(RuntimeException exception) {
        return problem(HttpStatus.BAD_GATEWAY, "Route graph unavailable", exception);
    }

    private static ProblemDetail problem(HttpStatus status, String title, RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        return problem;
    }
}
