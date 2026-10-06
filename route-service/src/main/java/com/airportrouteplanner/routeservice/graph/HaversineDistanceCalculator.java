package com.airportrouteplanner.routeservice.graph;

import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator {

    public static final double EARTH_MEAN_RADIUS_KM = 6371.0088;

    public double calculateKilometres(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude) {
        validateCoordinate("origin latitude", originLatitude, -90.0, 90.0);
        validateCoordinate("origin longitude", originLongitude, -180.0, 180.0);
        validateCoordinate("destination latitude", destinationLatitude, -90.0, 90.0);
        validateCoordinate("destination longitude", destinationLongitude, -180.0, 180.0);

        double originLatitudeRadians = Math.toRadians(originLatitude);
        double destinationLatitudeRadians = Math.toRadians(destinationLatitude);
        double latitudeDifference = Math.toRadians(destinationLatitude - originLatitude);
        double longitudeDifference = Math.toRadians(destinationLongitude - originLongitude);

        double haversine = Math.pow(Math.sin(latitudeDifference / 2.0), 2)
                + Math.cos(originLatitudeRadians)
                * Math.cos(destinationLatitudeRadians)
                * Math.pow(Math.sin(longitudeDifference / 2.0), 2);
        double clampedHaversine = Math.max(0.0, Math.min(1.0, haversine));
        double centralAngle = 2.0 * Math.atan2(
                Math.sqrt(clampedHaversine), Math.sqrt(1.0 - clampedHaversine));
        return EARTH_MEAN_RADIUS_KM * centralAngle;
    }

    private static void validateCoordinate(String name, double value, double minimum, double maximum) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be finite and between " + minimum + " and " + maximum + " inclusive");
        }
    }
}
