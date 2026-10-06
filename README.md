# Airport Route Planner

Airport Route Planner is an educational and portfolio project for exploring a microservice-based route-planning system with Java and Spring Boot.

## Current status

The repository now contains all five planned applications. `airport-service` provides 161 built-in airports, `fleet-service` provides 23 aircraft types and 77 flight companies, and `flight-service` derives a deterministic, runway-adjusted schedule of 1,232 daily FlightLegs from 616 persisted CompanyRoutes. `route-service` owns no database: on demand, it retrieves those service-owned datasets over HTTP and builds a validated in-memory directed multigraph. Airports are vertices, adjusted FlightLegs are edges, and distances are calculated in Java with the Haversine formula. `GET /api/route-graph/summary` provides diagnostic graph counts. `POST /api/routes/calculate` supports SHORTEST, CHEAPEST, and ECOLOGICAL with one reusable static Dijkstra implementation, plus schedule-aware FASTEST routing with a dedicated time-dependent earliest-arrival Dijkstra implementation.

`web-app` is the Spring MVC and Thymeleaf presentation/BFF application on port 8080. Its Leaflet/OpenStreetMap world map displays the 161 airports retrieved at runtime from `airport-service`, with marker details and origin/destination selectors. Browser code calls only web-app's `GET /api/ui/airports` endpoint. The route-planning controls provide the future interface structure, but route calculation submission, route drawing, and result presentation are not wired yet.

FASTEST interprets the requested departure as local wall-clock time at the origin using that airport's fixed UTC offset. Daylight-saving time is intentionally ignored. Calculations then use UTC, adjusted runway departure times, daily schedule recurrence, zero-minute minimum connections, and theoretical cruise duration (`Haversine distance / aircraft cruise speed`). Initial and connection waiting time are included in total journey time.

## Planned capabilities

The project is intended to eventually demonstrate:

- Java and Spring Boot microservices
- Weighted graph algorithms
- Shortest-route calculation
- Fastest schedule-aware route calculation
- Lowest-total-fuel route calculation
- Ecological fuel-per-passenger route calculation
- A Thymeleaf-based interactive map interface

The four routing modes and the airport-map foundation are implemented. Full route interaction and drawing in the Thymeleaf interface remain future work.

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
