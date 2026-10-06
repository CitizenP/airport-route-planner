# Airport Route Planner

Airport Route Planner is an educational and portfolio project for exploring a microservice-based route-planning system with Java and Spring Boot.

## Current status

The repository currently contains three implemented microservices. `airport-service` provides the Airport domain, 161 built-in airports, and a read-only REST API. `fleet-service` provides the AircraftType and FlightCompany domains, 23 built-in aircraft types, 77 built-in flight companies, and read-only REST APIs. `flight-service` contains 616 persisted CompanyRoutes and deterministically derives one outbound and one return FlightLeg for each route through a read-only API. This produces 1,232 derived daily scheduled legs before runway adjustment. Each service owns an independent H2 database for development and testing, with schemas and reference data managed by Flyway. Runway conflict adjustment, routing, and the user interface are not implemented.

## Planned capabilities

The project is intended to eventually demonstrate:

- Java and Spring Boot microservices
- Weighted graph algorithms
- Shortest-route calculation
- Fastest schedule-aware route calculation
- Lowest-total-fuel route calculation
- Ecological fuel-per-passenger route calculation
- A Thymeleaf-based interactive map interface

All capabilities listed above are planned and are not yet implemented.

## Requirements

- Java 25
- No system Maven installation is required; the repository includes Maven Wrapper 3.10.x.

## Build and test

On Windows:

```powershell
.\mvnw.cmd test
```

On Linux or macOS:

```shell
./mvnw test
```

See [docs/architecture.md](docs/architecture.md) for the planned service boundaries and architectural rules.
