# Airport Route Planner

Airport Route Planner is an educational and portfolio project for exploring a microservice-based route-planning system with Java and Spring Boot.

## Current status

The repository currently contains its Maven multi-module foundation and `airport-service`, which now provides the Airport domain, Flyway-managed persistence, and a read-only REST API. H2 is currently used for development and testing. The built-in airport dataset has not yet been added, and the other planned services and route-planning functionality are not implemented.

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
