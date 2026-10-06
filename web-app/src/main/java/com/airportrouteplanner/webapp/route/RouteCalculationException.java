package com.airportrouteplanner.webapp.route;

import org.springframework.http.HttpStatus;

public class RouteCalculationException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    public RouteCalculationException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    public RouteCalculationException(HttpStatus status, String title, String detail, Throwable cause) {
        super(detail, cause);
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}
