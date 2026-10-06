package com.airportrouteplanner.webapp.route;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RouteUiController.class)
public class RouteUiExceptionHandler {

    @ExceptionHandler(RouteCalculationException.class)
    public ProblemDetail handleRouteCalculationFailure(RouteCalculationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                exception.status(), exception.getMessage());
        problem.setTitle(exception.title());
        return problem;
    }
}
