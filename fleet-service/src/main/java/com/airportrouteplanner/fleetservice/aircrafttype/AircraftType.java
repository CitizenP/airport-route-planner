package com.airportrouteplanner.fleetservice.aircrafttype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Entity
@Table(
        name = "aircraft_types",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_aircraft_types_manufacturer_model",
                columnNames = {"manufacturer", "model"}))
public class AircraftType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank
    @Column(name = "manufacturer", nullable = false)
    private String manufacturer;

    @NotBlank
    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "cruise_speed_km_h", nullable = false)
    private double cruiseSpeedKmH;

    @Column(name = "maximum_range_km", nullable = false)
    private double maximumRangeKm;

    @Column(name = "fuel_capacity_litres", nullable = false)
    private double fuelCapacityLitres;

    @Column(name = "fuel_consumption_litres_per_km", nullable = false)
    private double fuelConsumptionLitresPerKm;

    @Positive
    @Column(name = "passenger_capacity", nullable = false)
    private int passengerCapacity;

    @Column(name = "fuel_consumption_per_passenger", nullable = false)
    private double fuelConsumptionPerPassenger;

    protected AircraftType() {
    }

    public AircraftType(
            String manufacturer,
            String model,
            double cruiseSpeedKmH,
            double maximumRangeKm,
            double fuelCapacityLitres,
            double fuelConsumptionLitresPerKm,
            int passengerCapacity,
            double fuelConsumptionPerPassenger) {
        requirePositiveFinite(cruiseSpeedKmH, "cruiseSpeedKmH");
        requirePositiveFinite(maximumRangeKm, "maximumRangeKm");
        requirePositiveFinite(fuelCapacityLitres, "fuelCapacityLitres");
        requirePositiveFinite(fuelConsumptionLitresPerKm, "fuelConsumptionLitresPerKm");
        if (passengerCapacity <= 0) {
            throw new IllegalArgumentException("passengerCapacity must be greater than zero");
        }
        requirePositiveFinite(fuelConsumptionPerPassenger, "fuelConsumptionPerPassenger");
        this.manufacturer = manufacturer;
        this.model = model;
        this.cruiseSpeedKmH = cruiseSpeedKmH;
        this.maximumRangeKm = maximumRangeKm;
        this.fuelCapacityLitres = fuelCapacityLitres;
        this.fuelConsumptionLitresPerKm = fuelConsumptionLitresPerKm;
        this.passengerCapacity = passengerCapacity;
        this.fuelConsumptionPerPassenger = fuelConsumptionPerPassenger;
    }

    private static void requirePositiveFinite(double value, String fieldName) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and greater than zero");
        }
    }

    public Long getId() {
        return id;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public double getCruiseSpeedKmH() {
        return cruiseSpeedKmH;
    }

    public double getMaximumRangeKm() {
        return maximumRangeKm;
    }

    public double getFuelCapacityLitres() {
        return fuelCapacityLitres;
    }

    public double getFuelConsumptionLitresPerKm() {
        return fuelConsumptionLitresPerKm;
    }

    public int getPassengerCapacity() {
        return passengerCapacity;
    }

    public double getFuelConsumptionPerPassenger() {
        return fuelConsumptionPerPassenger;
    }
}
