# Airport Route Planner

Airport Route Planner is an educational and portfolio project for exploring a microservice-based route-planning system with Java and Spring Boot.

## Current status

The repository currently contains four implemented microservices. `airport-service` provides 161 built-in airports, `fleet-service` provides 23 aircraft types and 77 flight companies, and `flight-service` derives a deterministic, runway-adjusted schedule of 1,232 daily FlightLegs from 616 persisted CompanyRoutes. `route-service` owns no database: on demand, it retrieves those service-owned datasets over HTTP and builds a validated in-memory directed multigraph. Airports are vertices, adjusted FlightLegs are edges, and distances are calculated in Java with the Haversine formula. `GET /api/route-graph/summary` provides diagnostic graph counts, while `POST /api/routes/calculate` supports SHORTEST, CHEAPEST, and ECOLOGICAL static routing through one reusable Dijkstra implementation. FASTEST routing and the user interface are not implemented.

## Planned capabilities

The project is intended to eventually demonstrate:

- Java and Spring Boot microservices
- Weighted graph algorithms
- Shortest-route calculation
- Fastest schedule-aware route calculation
- Lowest-total-fuel route calculation
- Ecological fuel-per-passenger route calculation
- A Thymeleaf-based interactive map interface

The routing algorithms and Thymeleaf interface listed above are planned and are not yet implemented.

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
