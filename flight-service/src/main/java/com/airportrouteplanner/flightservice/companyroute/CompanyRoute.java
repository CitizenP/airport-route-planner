package com.airportrouteplanner.flightservice.companyroute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalTime;
import java.util.Locale;

@Entity
@Table(
        name = "company_routes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_company_routes_company_route_number",
                columnNames = {"company_code", "route_number"}))
public class CompanyRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Min(1)
    @Max(8)
    @Column(name = "route_number", nullable = false)
    private int routeNumber;

    @NotBlank
    @Column(name = "company_code", length = 10, nullable = false)
    private String companyCode;

    @Pattern(regexp = "[A-Z]{3}")
    @Column(name = "base_airport_code", length = 3, nullable = false)
    private String baseAirportCode;

    @Pattern(regexp = "[A-Z]{3}")
    @Column(name = "destination_airport_code", length = 3, nullable = false)
    private String destinationAirportCode;

    @NotNull
    @Positive
    @Column(name = "aircraft_type_id", nullable = false)
    private Long aircraftTypeId;

    @NotNull
    @Column(name = "scheduled_outbound_departure_utc", nullable = false)
    private LocalTime scheduledOutboundDepartureUtc;

    protected CompanyRoute() {
    }

    public CompanyRoute(
            int routeNumber,
            String companyCode,
            String baseAirportCode,
            String destinationAirportCode,
            Long aircraftTypeId,
            LocalTime scheduledOutboundDepartureUtc) {
        if (routeNumber < 1 || routeNumber > 8) {
            throw new IllegalArgumentException("routeNumber must be between 1 and 8 inclusive");
        }
        if (companyCode == null || companyCode.isBlank()) {
            throw new IllegalArgumentException("companyCode is required");
        }

        String normalizedBaseAirportCode = normalizeAirportCode(baseAirportCode, "baseAirportCode");
        String normalizedDestinationAirportCode = normalizeAirportCode(
                destinationAirportCode, "destinationAirportCode");
        if (normalizedBaseAirportCode.equals(normalizedDestinationAirportCode)) {
            throw new IllegalArgumentException("baseAirportCode and destinationAirportCode must be different");
        }
        if (aircraftTypeId == null || aircraftTypeId <= 0) {
            throw new IllegalArgumentException("aircraftTypeId must be greater than zero");
        }
        if (scheduledOutboundDepartureUtc == null) {
            throw new IllegalArgumentException("scheduledOutboundDepartureUtc is required");
        }

        this.routeNumber = routeNumber;
        this.companyCode = companyCode.toUpperCase(Locale.ROOT);
        this.baseAirportCode = normalizedBaseAirportCode;
        this.destinationAirportCode = normalizedDestinationAirportCode;
        this.aircraftTypeId = aircraftTypeId;
        this.scheduledOutboundDepartureUtc = scheduledOutboundDepartureUtc;
    }

    private static String normalizeAirportCode(String airportCode, String fieldName) {
        if (airportCode == null) {
            throw new IllegalArgumentException(fieldName + " must contain exactly three letters");
        }
        String normalizedCode = airportCode.toUpperCase(Locale.ROOT);
        if (!normalizedCode.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(fieldName + " must contain exactly three letters");
        }
        return normalizedCode;
    }

    public Long getId() {
        return id;
    }

    public int getRouteNumber() {
        return routeNumber;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getBaseAirportCode() {
        return baseAirportCode;
    }

    public String getDestinationAirportCode() {
        return destinationAirportCode;
    }

    public Long getAircraftTypeId() {
        return aircraftTypeId;
    }

    public LocalTime getScheduledOutboundDepartureUtc() {
        return scheduledOutboundDepartureUtc;
    }
}
