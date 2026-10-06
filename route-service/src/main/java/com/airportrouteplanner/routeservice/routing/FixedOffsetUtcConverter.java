package com.airportrouteplanner.routeservice.routing;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class FixedOffsetUtcConverter {

    public Instant toUtcInstant(LocalDateTime localDateTime, int utcOffsetMinutes) {
        if (localDateTime == null) {
            throw new IllegalArgumentException("local date and time is required");
        }
        try {
            ZoneOffset offset = ZoneOffset.ofTotalSeconds(Math.multiplyExact(utcOffsetMinutes, 60));
            return localDateTime.toInstant(offset);
        } catch (ArithmeticException | DateTimeException exception) {
            throw new IllegalArgumentException(
                    "UTC offset cannot be represented by Java ZoneOffset: " + utcOffsetMinutes + " minutes",
                    exception);
        }
    }
}
