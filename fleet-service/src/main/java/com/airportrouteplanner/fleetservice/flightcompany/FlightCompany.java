package com.airportrouteplanner.fleetservice.flightcompany;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "flight_companies")
public class FlightCompany {

    @Id
    @NotBlank
    @Column(name = "code", length = 10, nullable = false, updatable = false)
    private String code;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "country", nullable = false)
    private String country;

    @Pattern(regexp = "[A-Z]{3}")
    @Column(name = "base_airport_code", length = 3, nullable = false)
    private String baseAirportCode;

    protected FlightCompany() {
    }

    public FlightCompany(String code, String name, String country, String baseAirportCode) {
        if (baseAirportCode == null || !baseAirportCode.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("baseAirportCode must contain exactly three uppercase letters");
        }
        this.code = code;
        this.name = name;
        this.country = country;
        this.baseAirportCode = baseAirportCode;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getBaseAirportCode() {
        return baseAirportCode;
    }
}
