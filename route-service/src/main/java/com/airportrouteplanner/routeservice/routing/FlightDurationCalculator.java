package com.airportrouteplanner.routeservice.routing;

import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class FlightDurationCalculator {

    private static final double NANOS_PER_HOUR = 3_600_000_000_000.0;

    public Duration calculate(double distanceKm, double cruiseSpeedKmH) {
        if (!Double.isFinite(distanceKm) || distanceKm < 0) {
            throw new IllegalArgumentException("distance must be finite and non-negative");
        }
        if (!Double.isFinite(cruiseSpeedKmH) || cruiseSpeedKmH <= 0) {
            throw new IllegalArgumentException("cruise speed must be finite and greater than zero");
        }
        double durationNanos = distanceKm / cruiseSpeedKmH * NANOS_PER_HOUR;
        if (!Double.isFinite(durationNanos) || durationNanos > Long.MAX_VALUE) {
            throw new IllegalArgumentException("calculated flight duration is not representable");
        }
        return Duration.ofNanos(Math.round(durationNanos));
    }

    public static double toMinutes(Duration duration) {
        return duration.toNanos() / 60_000_000_000.0;
    }
}
