package com.airportrouteplanner.routeservice.summary;

import com.airportrouteplanner.routeservice.integration.UpstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RouteGraphSummaryController.class)
public class RouteGraphExceptionHandler {

    @ExceptionHandler({UpstreamServiceException.class, RouteGraphUnavailableException.class})
    public ProblemDetail handleUnavailableGraph(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
        problem.setTitle("Route graph unavailable");
        return problem;
    }
}
