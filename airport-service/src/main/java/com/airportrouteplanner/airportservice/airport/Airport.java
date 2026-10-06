package com.airportrouteplanner.airportservice.airport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "airports")
public class Airport {

    @Id
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}")
    @Column(name = "iata_code", length = 3, nullable = false, updatable = false)
    private String iataCode;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "city", nullable = false)
    private String city;

    @NotBlank
    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Min(1)
    @Column(name = "number_of_runways", nullable = false)
    private int numberOfRunways;

    @Min(-720)
    @Max(840)
    @Column(name = "utc_offset_minutes", nullable = false)
    private int utcOffsetMinutes;

    protected Airport() {
    }

    public Airport(
            String iataCode,
            String name,
            String city,
            String country,
            double latitude,
            double longitude,
            int numberOfRunways,
            int utcOffsetMinutes) {
        if (!(latitude >= -90.0 && latitude <= 90.0)) {
            throw new IllegalArgumentException("latitude must be between -90 and 90 inclusive");
        }
        if (!(longitude >= -180.0 && longitude <= 180.0)) {
            throw new IllegalArgumentException("longitude must be between -180 and 180 inclusive");
        }
        this.iataCode = iataCode;
        this.name = name;
        this.city = city;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.numberOfRunways = numberOfRunways;
        this.utcOffsetMinutes = utcOffsetMinutes;
    }

    public String getIataCode() {
        return iataCode;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getNumberOfRunways() {
        return numberOfRunways;
    }

    public int getUtcOffsetMinutes() {
        return utcOffsetMinutes;
    }
}
